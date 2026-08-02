package com.instasave.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.instasave.app.data.local.dao.DownloadDao
import com.instasave.app.data.local.dao.HistoryDao
import com.instasave.app.data.local.entity.DownloadEntity
import com.instasave.app.data.local.entity.HistoryEntity

@Database(
    entities = [DownloadEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class InstaSaveDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao
}
