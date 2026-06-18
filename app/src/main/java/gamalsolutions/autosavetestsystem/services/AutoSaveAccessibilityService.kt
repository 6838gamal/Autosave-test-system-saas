package gamalsolutions.autosavetestsystem.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import gamalsolutions.autosavetestsystem.database.AppDatabase
import gamalsolutions.autosavetestsystem.managers.SmartSaveEngine
import gamalsolutions.autosavetestsystem.repositories.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AutoSaveAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: AppRepository
    private val phonePattern = Pattern.compile("(\\+?[0-9][0-9\\s\\-]{7,15}[0-9])")

    companion object {
        private var isRunning = false
        fun isServiceRunning(): Boolean = isRunning
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repository = AppRepository(db.appDao())
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        serviceScope.launch {
            repository.insertEvent("ACCESSIBILITY_START", "تم تفعيل خدمة إمكانية الوصول للرصد المتقدم.")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Filter events specifically from WhatsApp
        val packageName = event.packageName?.toString() ?: ""
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
            return
        }

        // Run checking asynchronously in a lightweight scan
        val rootNode = rootInActiveWindow ?: return
        serviceScope.launch {
            try {
                val settings = repository.getSettings()
                if (settings.isWhatsappEnabled) {
                    scanNodeForPhoneNumbers(rootNode)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun scanNodeForPhoneNumbers(node: AccessibilityNodeInfo?) {
        if (node == null) return

        // Extract and audit text contents of the node
        val nodeText = node.text?.toString() ?: ""
        if (nodeText.isNotBlank()) {
            val matcher = phonePattern.matcher(nodeText)
            while (matcher.find()) {
                val foundNumber = matcher.group(1) ?: ""
                // Avoid too short and fake strings
                if (foundNumber.filter { it.isDigit() }.length >= 7) {
                    SmartSaveEngine.processIncomingNumber(
                        context = this@AutoSaveAccessibilityService,
                        repository = repository,
                        rawNumber = foundNumber,
                        source = "accessibility"
                    )
                }
            }
        }

        // Recurse over children nodes safely
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            scanNodeForPhoneNumbers(child)
        }
    }

    override fun onInterrupt() {
        // Handle interruptions or stop request
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.launch {
            repository.insertEvent("ACCESSIBILITY_STOP", "تم إيقاف خدمة إمكانية الوصول.")
        }
    }
}
