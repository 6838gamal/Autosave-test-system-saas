package gamalsolutions.autosavetestsystem.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedContact::class, LogEntry::class, SystemEvent::class, SystemSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autosave_crm_database"
                )
                .fallbackToDestructiveMigration() // Simple for development / fast updates
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
