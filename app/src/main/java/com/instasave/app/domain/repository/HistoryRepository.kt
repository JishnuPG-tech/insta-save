package com.instasave.app.domain.repository

import com.instasave.app.domain.model.DownloadTask
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeHistory(): Flow<List<DownloadTask>>
    suspend fun saveRecord(task: DownloadTask)
    suspend fun deleteRecord(id: String, deleteFile: Boolean)
    suspend fun clearAllHistory()
}
