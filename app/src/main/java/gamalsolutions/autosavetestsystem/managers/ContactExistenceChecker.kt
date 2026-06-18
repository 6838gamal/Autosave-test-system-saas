package gamalsolutions.autosavetestsystem.managers

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import gamalsolutions.autosavetestsystem.repositories.AppRepository
import gamalsolutions.autosavetestsystem.utils.PhoneNumberUtils

object ContactExistenceChecker {

    /**
     * Checks if a phone number exists in the Android device's native contacts provider.
     */
    fun existsInSystemContacts(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        try {
            // Using PhoneLookup since Android optimizes it to match different formats of the same phone number
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Secondary fallback search in ContactsContract.CommonDataKinds.Phone
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    while (cursor.moveToNext()) {
                        val sysNumber = cursor.getString(numberIndex)
                        if (PhoneNumberUtils.areEqual(sysNumber, phoneNumber)) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    /**
     * Checks if a phone number exists inside our internal Room database.
     */
    suspend fun existsInInternalDb(repository: AppRepository, phoneNumber: String): Boolean {
        val normalized = PhoneNumberUtils.normalize(phoneNumber)
        if (normalized.isEmpty()) return false
        return repository.getContactByPhone(normalized) != null
    }

    /**
     * Checks if a phone number exists in either the system contacts or our internal database.
     */
    suspend fun isAlreadySaved(context: Context, repository: AppRepository, phoneNumber: String): Boolean {
        // 1. Check system contacts
        if (existsInSystemContacts(context, phoneNumber)) {
            return true
        }
        // 2. Check internal Room database
        return existsInInternalDb(repository, phoneNumber)
    }
}
