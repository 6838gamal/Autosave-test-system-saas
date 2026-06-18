package gamalsolutions.autosavetestsystem.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import gamalsolutions.autosavetestsystem.database.AppDatabase
import gamalsolutions.autosavetestsystem.managers.SmartSaveEngine
import gamalsolutions.autosavetestsystem.repositories.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private var isIncoming = false
        private var savedNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        // Handle outgoing call
        if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return
            processNumberAsync(context, outgoingNumber, "outgoing_call")
            return
        }

        // Handle phone state change (Incoming / Outgoing)
        if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val rawNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            val state = when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.EXTRA_STATE_RINGING
                TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.EXTRA_STATE_OFFHOOK
                TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.EXTRA_STATE_IDLE
                else -> TelephonyManager.EXTRA_STATE_IDLE
            }

            if (lastState == state) {
                return // Prevent redundant triggers
            }

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    isIncoming = true
                    if (rawNumber != null) {
                        savedNumber = rawNumber
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Outgoing or Incoming is active
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended, process the stored incoming number if detected
                    val numberToProcess = savedNumber
                    if (isIncoming && numberToProcess != null) {
                        processNumberAsync(context, numberToProcess, "incoming_call")
                    }
                    // Reset states
                    isIncoming = false
                    savedNumber = null
                }
            }
            lastState = state
        }
    }

    private fun processNumberAsync(context: Context, number: String, source: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = AppRepository(db.appDao())
                
                val settings = repository.getSettings()
                if (settings.isCallsEnabled) {
                    SmartSaveEngine.processIncomingNumber(
                        context = context,
                        repository = repository,
                        rawNumber = number,
                        source = source
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
