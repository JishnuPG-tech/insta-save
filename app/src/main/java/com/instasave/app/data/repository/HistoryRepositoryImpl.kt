package com.instasave.app.data.repository

import com.instasave.app.data.local.dao.HistoryDao
import com.instasave.app.data.local.entity.HistoryEntity
import com.instasave.app.domain.model.Author
import com.instasave.app.domain.model.Destination
import com.instasave.app.domain.model.DownloadStatus
import com.instasave.app.domain.model.DownloadTask
import com.instasave.app.domain.model.EngineTag
import com.instasave.app.domain.model.FormatOption
import com.instasave.app.domain.model.MediaInfo
import com.instasave.app.domain.model.MediaItem
import com.instasave.app.domain.model.MediaType
import com.instasave.app.domain.model.PostKind
import com.instasave.app.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun observeHistory(): Flow<List<DownloadTask>> {
        return historyDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveRecord(task: DownloadTask) {
        val entity = HistoryEntity(
            id = task.id,
            postId = task.mediaInfo.postId,
            shortcode = task.mediaInfo.shortcode,
            sourceUrl = task.mediaInfo.sourceUrl,
            authorUsername = task.mediaInfo.author.username,
            kind = task.mediaInfo.kind.name,
            mediaType = task.selectedFormat.container,
            targetFilename = task.targetFilename,
            savedFilePath = null,
            savedFileUri = null,
            caption = task.mediaInfo.caption,
            createdAtEpochMs = System.currentTimeMillis()
        )
        historyDao.insert(entity)
    }

    override suspend fun deleteRecord(id: String, deleteFile: Boolean) {
        historyDao.deleteById(id)
    }

    override suspend fun clearAllHistory() {
        historyDao.clearAll()
    }

    private fun HistoryEntity.toDomain(): DownloadTask {
        val mockFormat = FormatOption(id = "hist", container = mediaType, url = sourceUrl)
        val mockMediaInfo = MediaInfo(
            postId = postId,
            shortcode = shortcode,
            sourceUrl = sourceUrl,
            kind = PostKind.valueOf(kind),
            author = Author(username = authorUsername),
            caption = caption,
            items = listOf(MediaItem(id = shortcode, index = 0, type = MediaType.VIDEO, thumbnailUrl = null, formats = listOf(mockFormat))),
            extractedBy = EngineTag.NATIVE
        )

        return DownloadTask(
            id = id,
            mediaInfo = mockMediaInfo,
            selectedItemIndex = 0,
            selectedFormat = mockFormat,
            targetFilename = targetFilename,
            destination = Destination.DEFAULT,
            progress = 1.0f,
            status = DownloadStatus.COMPLETED,
            createdAtEpochMs = createdAtEpochMs
        )
    }
}
