package gamalsolutions.autosavetestsystem.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import gamalsolutions.autosavetestsystem.database.AppDatabase
import gamalsolutions.autosavetestsystem.managers.SmartSaveEngine
import gamalsolutions.autosavetestsystem.repositories.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Extract the sender address from the first message
        val sender = messages[0].originatingAddress ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = AppRepository(db.appDao())

                val settings = repository.getSettings()
                if (settings.isSmsEnabled) {
                    SmartSaveEngine.processIncomingNumber(
                        context = context,
                        repository = repository,
                        rawNumber = sender,
                        source = "sms"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
