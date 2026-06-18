package gamalsolutions.autosavetestsystem.repositories

import gamalsolutions.autosavetestsystem.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class AppRepository(private val appDao: AppDao) {

    // Contacts
    val allContacts: Flow<List<SavedContact>> = appDao.getAllContacts()
    val totalContactsCount: Flow<Int> = appDao.getContactsCountFlow()
    val lastSavedContact: Flow<SavedContact?> = appDao.getLastSavedContactFlow()

    suspend fun getContactsCount(): Int {
        return appDao.getContactsCount()
    }

    fun searchContacts(query: String): Flow<List<SavedContact>> {
        return if (query.isBlank()) {
            appDao.getAllContacts()
        } else {
            appDao.searchContacts("%$query%")
        }
    }

    suspend fun getContactByPhone(phoneNumber: String): SavedContact? {
        return appDao.getContactByPhone(phoneNumber)
    }

    suspend fun insertContact(contact: SavedContact): Long {
        return appDao.insertContact(contact)
    }

    suspend fun updateContact(contact: SavedContact) {
        appDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: SavedContact) {
        appDao.deleteContact(contact)
    }

    suspend fun deleteAllContacts() {
        appDao.deleteAllContacts()
    }


    // Logs
    val allLogs: Flow<List<LogEntry>> = appDao.getAllLogs()

    fun searchLogs(query: String): Flow<List<LogEntry>> {
        return if (query.isBlank()) {
            appDao.getAllLogs()
        } else {
            appDao.searchLogs("%$query%")
        }
    }

    suspend fun insertLog(log: LogEntry) {
        appDao.insertLog(log)
    }

    suspend fun deleteAllLogs() {
        appDao.deleteAllLogs()
    }


    // Events
    val allEvents: Flow<List<SystemEvent>> = appDao.getAllEvents()

    suspend fun insertEvent(eventType: String, details: String) {
        appDao.insertEvent(SystemEvent(eventType = eventType, details = details))
    }

    suspend fun deleteAllEvents() {
        appDao.deleteAllEvents()
    }


    // Settings (reactive flow, initializes if empty)
    val settingsFlow: Flow<SystemSettings> = appDao.getSettingsFlow()
        .onStart {
            // Check if settings exist, initialize if not
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val current = appDao.getSettings()
                if (current == null) {
                    appDao.saveSettings(SystemSettings())
                }
            }
        }
        .onStart {  } // Placeholder
        .mapNotNullSettings()

    // Helper map extension
    private fun Flow<SystemSettings?>.mapNotNullSettings(): Flow<SystemSettings> {
        return this.map { it ?: SystemSettings() }
    }

    suspend fun getSettings(): SystemSettings {
        return appDao.getSettings() ?: SystemSettings().also {
            appDao.saveSettings(it)
        }
    }

    suspend fun saveSettings(settings: SystemSettings) {
        appDao.saveSettings(settings)
    }

    // Atomically increment and get client counter
    suspend fun incrementAndGetCounter(): Int {
        val currentSettings = getSettings()
        val nextCounter = currentSettings.clientCounter + 1
        saveSettings(currentSettings.copy(clientCounter = nextCounter))
        return currentSettings.clientCounter
    }
}
