package gamalsolutions.autosavetestsystem.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gamalsolutions.autosavetestsystem.database.AppDatabase
import gamalsolutions.autosavetestsystem.database.LogEntry
import gamalsolutions.autosavetestsystem.database.SavedContact
import gamalsolutions.autosavetestsystem.database.SystemSettings
import gamalsolutions.autosavetestsystem.export.ExportManager
import gamalsolutions.autosavetestsystem.repositories.AppRepository
import gamalsolutions.autosavetestsystem.services.AutoSaveForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    // Search query flows
    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery = _contactSearchQuery.asStateFlow()

    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery = _logSearchQuery.asStateFlow()

    // Main Flows connected reactively to UI
    val savedContacts: StateFlow<List<SavedContact>>
    val allLogs: StateFlow<List<LogEntry>>
    val systemSettings: StateFlow<SystemSettings>
    val lastSavedContact: StateFlow<SavedContact?>
    val totalContactsCount: StateFlow<Int>

    // Dynamic Permission & Service States for Dashboard
    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive = _isServiceActive.asStateFlow()

    private val _permissionsMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionsMap = _permissionsMap.asStateFlow()

    // Export Status
    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus = _exportStatus.asStateFlow()

    // Developer Contact Block State
    private val _showDeveloperContactBlock = MutableStateFlow(false)
    val showDeveloperContactBlock = _showDeveloperContactBlock.asStateFlow()

    fun triggerDeveloperBlock() {
        _showDeveloperContactBlock.value = true
    }

    fun dismissDeveloperBlock() {
        _showDeveloperContactBlock.value = false
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())

        // Wire contacts list reactively with search query
        savedContacts = _contactSearchQuery
            .debounce(200)
            .flatMapLatest { query ->
                repository.searchContacts(query)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Wire logs list reactively with search query
        allLogs = _logSearchQuery
            .debounce(200)
            .flatMapLatest { query ->
                repository.searchLogs(query)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Settings Flow
        systemSettings = repository.settingsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemSettings())

        // General Stats
        lastSavedContact = repository.lastSavedContact
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        totalContactsCount = repository.totalContactsCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        // Initial States check
        refreshStatusStates()
    }

    /**
     * Refreshes the service running checks and all system access permissions maps.
     */
    fun refreshStatusStates() {
        _isServiceActive.value = AutoSaveForegroundService.isServiceRunning()
        
        val context = getApplication<Application>()
        val pm = context.packageManager
        
        val map = mutableMapOf<String, Boolean>()
        
        // 1. Runtime Permissions
        map["SMS"] = hasPermission(android.Manifest.permission.RECEIVE_SMS) && hasPermission(android.Manifest.permission.READ_SMS)
        map["CALLS"] = hasPermission(android.Manifest.permission.READ_PHONE_STATE) && hasPermission(android.Manifest.permission.READ_CALL_LOG)
        map["CONTACTS"] = hasPermission(android.Manifest.permission.READ_CONTACTS) && hasPermission(android.Manifest.permission.WRITE_CONTACTS)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            map["NOTIFICATIONS_POST"] = hasPermission(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            map["NOTIFICATIONS_POST"] = true
        }

        // 2. Battery optimization status
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        map["BATTERY_IGNORE"] = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        // 3. Notification Access Service check
        val notifListeners = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        map["NOTIF_LISTENER"] = notifListeners?.contains(context.packageName) == true

        // 4. Accessibility Service check
        val accessibilityServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_accessibility_services"
        )
        map["ACCESSIBILITY"] = accessibilityServices?.contains(context.packageName) == true

        _permissionsMap.value = map
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // --- Search Queries modifiers ---
    fun setContactSearchQuery(query: String) {
        _contactSearchQuery.value = query
    }

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
    }

    // --- Toggle Background Service ---
    fun toggleForegroundService() {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        val context = getApplication<Application>()
        if (AutoSaveForegroundService.isServiceRunning()) {
            AutoSaveForegroundService.stopService(context)
            viewModelScope.launch {
                repository.insertEvent("SERVICE_UI_TOGGLE", "إيقاف خدمة الرصد يدوياً عبر واجهة المستخدم")
            }
        } else {
            AutoSaveForegroundService.startService(context)
            viewModelScope.launch {
                repository.insertEvent("SERVICE_UI_TOGGLE", "تشغيل خدمة الرصد يدوياً عبر واجهة المستخدم")
            }
        }
        // Small delay to allow service to start/stop before checking running state
        viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            _isServiceActive.value = AutoSaveForegroundService.isServiceRunning()
        }
    }

    // --- Settings handlers ---
    fun updateClientPrefix(prefix: String) {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(clientPrefix = prefix))
        }
    }

    fun updateClientCounter(counter: Int) {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(clientCounter = counter))
        }
    }

    fun toggleCallsMonitoring(enabled: Boolean) {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(isCallsEnabled = enabled))
        }
    }

    fun toggleSmsMonitoring(enabled: Boolean) {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(isSmsEnabled = enabled))
        }
    }

    fun toggleWhatsappMonitoring(enabled: Boolean) {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(isWhatsappEnabled = enabled))
        }
    }

    // --- Contact edits & deletes ---
    fun renameContact(contact: SavedContact, newName: String) {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updated = contact.copy(name = newName)
            repository.updateContact(updated)
            // also we can append a log trace
            repository.insertLog(
                LogEntry(
                    phoneNumber = contact.rawNumber,
                    source = "ui_update",
                    status = "updated",
                    details = "تم تعديل اسم العميل يدوياً من \"${contact.name}\" إلى \"$newName\""
                )
            )
        }
    }

    fun deleteContact(contact: SavedContact) {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteContact(contact)
            // also append log
            repository.insertLog(
                LogEntry(
                    phoneNumber = contact.rawNumber,
                    source = "ui_delete",
                    status = "deleted",
                    details = "تم حذف العميل \"${contact.name}\" من قاعدة البيانات الداخلية"
                )
            )
        }
    }

    fun clearAllLogs() {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllLogs()
        }
    }

    // --- Export Handler ---
    fun triggerExportContacts() {
        if (totalContactsCount.value >= 100) {
            triggerDeveloperBlock()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val contacts = savedContacts.value
            if (contacts.isEmpty()) {
                _exportStatus.value = "empty"
                return@launch
            }
            
            val fileName = ExportManager.exportContactsToCsv(getApplication(), contacts)
            if (fileName != null) {
                _exportStatus.value = fileName
            } else {
                _exportStatus.value = "error"
            }
        }
    }

    fun clearExportStatus() {
        _exportStatus.value = null
    }
}
