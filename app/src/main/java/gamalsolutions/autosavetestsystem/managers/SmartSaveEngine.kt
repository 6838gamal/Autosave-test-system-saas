package gamalsolutions.autosavetestsystem.managers

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import gamalsolutions.autosavetestsystem.database.LogEntry
import gamalsolutions.autosavetestsystem.database.SavedContact
import gamalsolutions.autosavetestsystem.notifications.AppNotificationManager
import gamalsolutions.autosavetestsystem.repositories.AppRepository
import gamalsolutions.autosavetestsystem.utils.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmartSaveEngine {

    /**
     * Processes a detected phone number.
     * @param context Host application context
     * @param repository AppRepository for database read/write
     * @param rawNumber Original detected phone number
     * @param source Source identifier (e.g., call, sms, WhatsApp)
     */
    suspend fun processIncomingNumber(
        context: Context,
        repository: AppRepository,
        rawNumber: String,
        source: String
    ): Unit = withContext(Dispatchers.IO) {
        val cleanNumber = PhoneNumberUtils.cleanForDisplay(rawNumber)
        val normalized = PhoneNumberUtils.normalize(cleanNumber)

        if (normalized.isBlank()) return@withContext

        // 1. Check device system contacts first
        // If it is in the device's native contacts list, IGNORE operational flow entirely.
        // The requirements say: No saving, no adding to internal database, no adding to logs.
        if (ContactExistenceChecker.existsInSystemContacts(context, cleanNumber)) {
            return@withContext
        }

        // 2. Check internal database
        val existingInternal = repository.getContactByPhone(normalized)
        if (existingInternal != null) {
            // Already in internal database, update last active activity and interactions
            val updatedContact = existingInternal.copy(
                lastActive = System.currentTimeMillis(),
                interactionsCount = existingInternal.interactionsCount + 1
            )
            repository.updateContact(updatedContact)

            // Log the operation update
            val logDetails = "تلقي نشاط جديد من رقم مسجل داخلياً: $cleanNumber"
            repository.insertLog(
                LogEntry(
                    phoneNumber = cleanNumber,
                    source = source,
                    status = "updated",
                    details = logDetails
                )
            )
            return@withContext
        }

        // 3. Completely new number! Save to device contacts and internal database
        try {
            // Check trial version limit
            if (repository.getContactsCount() >= 100) {
                repository.insertLog(
                    LogEntry(
                        phoneNumber = cleanNumber,
                        source = source,
                        status = "error",
                        details = "توقف الحفظ التلقائي: تم الوصول للحد الأقصى للتجربة (100 عميل). يرجى التواصل مع المطور لتفعيل النظام بالكامل."
                    )
                )
                return@withContext
            }

            // Load naming configurations from Settings
            val settings = repository.getSettings()
            val currentPrefix = settings.clientPrefix.trim()
            
            // Get next counter and increment instantly to preserve unique atomic indexing
            val nextCounterIndex = repository.incrementAndGetCounter()
            val finalClientName = "$currentPrefix $nextCounterIndex"

            // Save in Android native Contacts Provider
            val systemContactId = saveToSystemContacts(context, finalClientName, cleanNumber)

            // Save in internal Room DB
            val savedContact = SavedContact(
                name = finalClientName,
                phoneNumber = normalized,
                rawNumber = cleanNumber,
                source = source,
                timestamp = System.currentTimeMillis(),
                lastActive = System.currentTimeMillis(),
                interactionsCount = 1,
                systemContactId = systemContactId
            )
            repository.insertContact(savedContact)

            // Save transaction trace in logs
            val logDetails = "حفظ العميل الجديد \"$finalClientName\" بنجاح وتصدير لجهات اتصال النظام."
            repository.insertLog(
                LogEntry(
                    phoneNumber = cleanNumber,
                    source = source,
                    status = "success",
                    details = logDetails
                )
            )

            // Send notification about the automatic action
            AppNotificationManager.showSaveSuccessNotification(
                context = context,
                clientName = finalClientName,
                phone = cleanNumber,
                sourceName = getLocalizedSource(context, source)
            )

        } catch (e: Exception) {
            e.printStackTrace()
            // Record failure inside logs
            repository.insertLog(
                LogEntry(
                    phoneNumber = cleanNumber,
                    source = source,
                    status = "error",
                    details = "فشل أثناء جلب العداد أو حفظ جهة اتصال جديدة باسم تلقائي.",
                    errorMsg = e.localizedMessage
                )
            )
        }
    }

    /**
     * Inserts contact into native Android Contacts.
     */
    private fun saveToSystemContacts(context: Context, name: String, phoneNumber: String): Long? {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val ops = ArrayList<ContentProviderOperation>()

        // Raw contact insert
        val rawContactIdx = ops.size
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        // Name insert
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIdx)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        // Phone mobile number insert
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIdx)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                )
                .build()
        )

        try {
            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            if (results.isNotEmpty() && results[0] != null) {
                val uri = results[0].uri
                if (uri != null) {
                    return android.content.ContentUris.parseId(uri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Formats database string sources into beautiful readable Arabic texts.
     */
    private fun getLocalizedSource(context: Context, source: String): String {
        return when (source) {
            "incoming_call" -> "مكالمة واردة"
            "outgoing_call" -> "مكالمة صادرة"
            "sms" -> "رسالة SMS"
            "whatsapp" -> "إشعار واتساب"
            "accessibility" -> "إمكانية الوصول (واتساب)"
            else -> source
        }
    }
}
