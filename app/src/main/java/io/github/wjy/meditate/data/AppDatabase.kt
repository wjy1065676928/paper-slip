package io.github.wjy.meditate.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(entities = [JournalEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val mutex = Mutex()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        suspend fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: mutex.withLock {
                INSTANCE ?: run {
                    val settingsManager = SettingsManager(context)
                    val slot = settingsManager.activeSlot.first()
                    val dbName = "paper_database_$slot"
                    
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        dbName
                    )
                        .addMigrations(MIGRATION_1_2)
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }

        /**
         * 预览一个特定的数据库文件（非活跃槽位）
         */
        fun getPreviewDatabase(context: Context, slot: String): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "paper_database_$slot"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        suspend fun checkpoint(context: Context) {
            val db = getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        }
    }
}
