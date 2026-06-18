package gamalsolutions.autosavetestsystem.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Contacts ---
    @Query("SELECT * FROM contacts_table ORDER BY timestamp DESC")
    fun getAllContacts(): Flow<List<SavedContact>>

    @Query("SELECT * FROM contacts_table WHERE phone_number = :normalizedNumber LIMIT 1")
    suspend fun getContactByPhone(normalizedNumber: String): SavedContact?

    @Query("SELECT * FROM contacts_table WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Int): SavedContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: SavedContact): Long

    @Update
    suspend fun updateContact(contact: SavedContact)

    @Delete
    suspend fun deleteContact(contact: SavedContact)

    @Query("DELETE FROM contacts_table")
    suspend fun deleteAllContacts()

    @Query("SELECT COUNT(*) FROM contacts_table")
    fun getContactsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM contacts_table")
    suspend fun getContactsCount(): Int

    @Query("SELECT * FROM contacts_table ORDER BY timestamp DESC LIMIT 1")
    fun getLastSavedContactFlow(): Flow<SavedContact?>

    @Query("SELECT * FROM contacts_table WHERE name LIKE :query OR phone_number LIKE :query ORDER BY timestamp DESC")
    fun searchContacts(query: String): Flow<List<SavedContact>>


    // --- Logs ---
    @Query("SELECT * FROM logs_table ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry)

    @Query("DELETE FROM logs_table")
    suspend fun deleteAllLogs()

    @Query("SELECT * FROM logs_table WHERE phone_number LIKE :query OR details LIKE :query ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<LogEntry>>


    // --- Events ---
    @Query("SELECT * FROM events_table ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SystemEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SystemEvent)

    @Query("DELETE FROM events_table")
    suspend fun deleteAllEvents()


    // --- Settings ---
    @Query("SELECT * FROM settings_table WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SystemSettings?

    @Query("SELECT * FROM settings_table WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SystemSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SystemSettings)
}
