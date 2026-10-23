package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BatchDao
import com.example.data.local.dao.ExportDao
import com.example.data.local.dao.LinkDao
import com.example.data.local.dao.ScreenshotDao
import com.example.data.local.dao.SyncOutboxDao
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.ExportRecordEntity
import com.example.data.local.entity.LinkItemEntity
import com.example.data.local.entity.ScreenshotEntity
import com.example.data.local.entity.SyncOutboxEntity

@Database(
    entities = [
        BatchEntity::class,
        LinkItemEntity::class,
        ScreenshotEntity::class,
        SyncOutboxEntity::class,
        ExportRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun linkDao(): LinkDao
    abstract fun screenshotDao(): ScreenshotDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun exportDao(): ExportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mf_evidence_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
