package gamalsolutions.autosavetestsystem.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts_table",
    indices = [Index(value = ["phone_number"], unique = true)]
)
data class SavedContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String, // Normalized
    @ColumnInfo(name = "raw_number") val rawNumber: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_active") val lastActive: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "interactions_count") val interactionsCount: Int = 1,
    @ColumnInfo(name = "system_contact_id") val systemContactId: Long? = null
)

@Entity(
    tableName = "logs_table",
    indices = [Index(value = ["phone_number"]), Index(value = ["timestamp"])]
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "status") val status: String, // success, updated, ignored_existing, error
    @ColumnInfo(name = "details") val details: String,
    @ColumnInfo(name = "error_msg") val errorMsg: String? = null
)

@Entity(tableName = "events_table")
data class SystemEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "event_type") val eventType: String, // BOOT, SERVICE_START, SERVICE_STOP, PERMISSION_CHANGED
    @ColumnInfo(name = "details") val details: String
)

@Entity(tableName = "settings_table")
data class SystemSettings(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "client_prefix") val clientPrefix: String = "عميل",
    @ColumnInfo(name = "client_counter") val clientCounter: Int = 1,
    @ColumnInfo(name = "is_calls_enabled") val isCallsEnabled: Boolean = true,
    @ColumnInfo(name = "is_sms_enabled") val isSmsEnabled: Boolean = true,
    @ColumnInfo(name = "is_whatsapp_enabled") val isWhatsappEnabled: Boolean = true
)
