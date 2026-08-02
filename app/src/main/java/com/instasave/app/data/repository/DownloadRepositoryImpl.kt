package com.instasave.app.data.repository

import com.instasave.app.data.local.dao.DownloadDao
import com.instasave.app.data.local.entity.DownloadEntity
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
import com.instasave.app.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao
) : DownloadRepository {

    override suspend fun enqueue(tasks: List<DownloadTask>) {
        val entities = tasks.map { it.toEntity() }
        downloadDao.insertAll(entities)
    }

    override fun observeQueue(): Flow<List<DownloadTask>> {
        return downloadDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun pause(id: String) {
        val existing = downloadDao.getById(id) ?: return
        downloadDao.update(existing.copy(status = DownloadStatus.PAUSED.name))
    }

    override suspend fun resume(id: String) {
        val existing = downloadDao.getById(id) ?: return
        downloadDao.update(existing.copy(status = DownloadStatus.QUEUED.name))
    }

    override suspend fun cancel(id: String) {
        downloadDao.deleteById(id)
    }

    override suspend fun retry(id: String) {
        val existing = downloadDao.getById(id) ?: return
        downloadDao.update(existing.copy(status = DownloadStatus.QUEUED.name, errorDetail = null))
    }

    override suspend fun clearCompleted() {
        downloadDao.deleteCompleted()
    }

    private fun DownloadTask.toEntity() = DownloadEntity(
        id = id,
        postId = mediaInfo.postId,
        shortcode = mediaInfo.shortcode,
        sourceUrl = mediaInfo.sourceUrl,
        authorUsername = mediaInfo.author.username,
        selectedIndex = selectedItemIndex,
        formatId = selectedFormat.id,
        formatContainer = selectedFormat.container,
        downloadUrl = selectedFormat.url,
        targetFilename = targetFilename,
        destinationType = destination.name,
        progress = progress,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        speedBytesPerSec = speedBytesPerSec,
        status = status.name,
        errorDetail = errorDetail,
        saveCaptionSidecar = saveCaptionSidecar,
        createdAtEpochMs = createdAtEpochMs
    )

    private fun DownloadEntity.toDomain(): DownloadTask {
        val mockFormat = FormatOption(id = formatId, container = formatContainer, url = downloadUrl)
        val mockMediaInfo = MediaInfo(
            postId = postId,
            shortcode = shortcode,
            sourceUrl = sourceUrl,
            kind = PostKind.REEL,
            author = Author(username = authorUsername),
            caption = null,
            items = listOf(MediaItem(id = shortcode, index = selectedIndex, type = MediaType.VIDEO, thumbnailUrl = null, formats = listOf(mockFormat))),
            extractedBy = EngineTag.NATIVE
        )

        return DownloadTask(
            id = id,
            mediaInfo = mockMediaInfo,
            selectedItemIndex = selectedIndex,
            selectedFormat = mockFormat,
            targetFilename = targetFilename,
            destination = Destination.valueOf(destinationType),
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            status = DownloadStatus.valueOf(status),
            errorDetail = errorDetail,
            saveCaptionSidecar = saveCaptionSidecar,
            createdAtEpochMs = createdAtEpochMs
        )
    }
}
