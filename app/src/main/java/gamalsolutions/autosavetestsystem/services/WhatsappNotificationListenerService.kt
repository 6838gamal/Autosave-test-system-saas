package gamalsolutions.autosavetestsystem.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import gamalsolutions.autosavetestsystem.database.AppDatabase
import gamalsolutions.autosavetestsystem.managers.SmartSaveEngine
import gamalsolutions.autosavetestsystem.repositories.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class WhatsappNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: AppRepository

    // Matches phone formats like: +967 777 777 777, +967777777777, 0777777777, 777777777, 1-234-567-8901
    private val phonePattern = Pattern.compile("(\\+?[0-9][0-9\\s\\-]{7,15}[0-9])")

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repository = AppRepository(db.appDao())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: ""
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") {
            return
        }

        serviceScope.launch {
            try {
                val settings = repository.getSettings()
                if (!settings.isWhatsappEnabled) {
                    return@launch
                }

                val extras = sbn.notification.extras ?: return@launch
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

                // Log trace Event for debugging
                repository.insertEvent(
                    "WHATSAPP_NOTIF",
                    "تلّقي إشعار من واتساب: العنوان \"$title\"، اللب \"$text\""
                )

                // Unsaved numbers inside WhatsApp usually display the raw phone number as the title.
                // Let's test if the title contains a phone number.
                val matcher = phonePattern.matcher(title)
                if (matcher.find()) {
                    val detectedNumber = matcher.group(1) ?: ""
                    // Process this number
                    SmartSaveEngine.processIncomingNumber(
                        context = this@WhatsappNotificationListenerService,
                        repository = repository,
                        rawNumber = detectedNumber,
                        source = "whatsapp"
                    )
                } else {
                    // Try parsing the message body (sometimes numbers are shared inside WhatsApp texts)
                    val bodyMatcher = phonePattern.matcher(text)
                    if (bodyMatcher.find()) {
                        val detectedNumber = bodyMatcher.group(1) ?: ""
                        SmartSaveEngine.processIncomingNumber(
                            context = this@WhatsappNotificationListenerService,
                            repository = repository,
                            rawNumber = detectedNumber,
                            source = "whatsapp"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
