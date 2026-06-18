package gamalsolutions.autosavetestsystem.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import gamalsolutions.autosavetestsystem.database.AppDatabase
import gamalsolutions.autosavetestsystem.notifications.AppNotificationManager
import gamalsolutions.autosavetestsystem.repositories.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AutoSaveForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: AppRepository

    companion object {
        private var isRunning = false

        fun isServiceRunning(): Boolean {
            return isRunning
        }

        fun startService(context: Context) {
            val intent = Intent(context, AutoSaveForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AutoSaveForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = AppRepository(database.appDao())
        
        AppNotificationManager.createNotificationChannels(this)
        isRunning = true
        
        serviceScope.launch {
            repository.insertEvent("SERVICE_START", "تم تشغيل خدمة السيارات الفنية بالخلفية للرصد.")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = AppNotificationManager.buildServiceNotification(this)
        
        // Match standard Android versions appropriately
        startForeground(AppNotificationManager.SERVICE_NOTIF_ID, notification)

        // Return STICKY so Android auto-restarts it if killed
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.launch {
            repository.insertEvent("SERVICE_STOP", "تم إيقاف خدمة السيارات الفنية بالخلفية للرصد.")
            serviceJob.cancel()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
