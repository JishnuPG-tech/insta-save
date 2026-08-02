package com.instasave.app.domain.repository

import com.instasave.app.domain.model.DownloadTask
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    suspend fun enqueue(tasks: List<DownloadTask>)
    fun observeQueue(): Flow<List<DownloadTask>>
    suspend fun pause(id: String)
    suspend fun resume(id: String)
    suspend fun cancel(id: String)
    suspend fun retry(id: String)
    suspend fun clearCompleted()
}
