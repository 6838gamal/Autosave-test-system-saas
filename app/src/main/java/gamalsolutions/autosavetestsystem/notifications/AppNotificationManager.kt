package gamalsolutions.autosavetestsystem.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import gamalsolutions.autosavetestsystem.MainActivity
import gamalsolutions.autosavetestsystem.R

object AppNotificationManager {

    const val SERVICE_CHANNEL_ID = "autosave_background_service_channel"
    const val ALERTS_CHANNEL_ID = "autosave_saves_alerts_channel"
    const val SERVICE_NOTIF_ID = 4001
    const val ALERT_NOTIF_ID_BASE = 5000

    private var alertIdCounter = 0

    /**
     * Initializes notification channels for Android 8.0+.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Channel for background foreground service
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }

            // 2. Channel for save success alerts
            val alertsChannel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                context.getString(R.string.notif_save_success_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_save_success_title)
                enableLights(true)
                enableVibration(true)
            }

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    /**
     * Builds the persistent notification required to run our Foreground Service.
     */
    fun buildServiceNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flag)

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_service_title))
            .setContentText(context.getString(R.string.notif_service_desc))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Shows a HUD alert when a new contact is successfully added.
     */
    fun showSaveSuccessNotification(context: Context, clientName: String, phone: String, sourceName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flag)

        val message = context.getString(R.string.notif_save_success_desc, clientName, phone, sourceName)

        val notification = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_save_success_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(ALERT_NOTIF_ID_BASE + (++alertIdCounter), notification)
    }
}
