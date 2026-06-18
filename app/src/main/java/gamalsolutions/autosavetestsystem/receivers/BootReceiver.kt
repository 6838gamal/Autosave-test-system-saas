package gamalsolutions.autosavetestsystem.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import gamalsolutions.autosavetestsystem.services.AutoSaveForegroundService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AutoSaveForegroundService.startService(context)
        }
    }
}
