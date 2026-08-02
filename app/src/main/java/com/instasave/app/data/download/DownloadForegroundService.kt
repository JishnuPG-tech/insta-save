package com.instasave.app.data.download

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.instasave.app.data.local.dao.DownloadDao
import com.instasave.app.data.storage.FilenameTemplater
import com.instasave.app.data.storage.MediaStoreWriter
import com.instasave.app.data.storage.SidecarWriter
import com.instasave.app.domain.model.DownloadStatus
import com.instasave.app.domain.model.MediaType
import com.instasave.app.domain.repository.HistoryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject lateinit var notificationController: NotificationController
    @Inject lateinit var chunkedDownloader: ChunkedDownloader
    @Inject lateinit var mediaStoreWriter: MediaStoreWriter
    @Inject lateinit var sidecarWriter: SidecarWriter
    @Inject lateinit var filenameTemplater: FilenameTemplater
    @Inject lateinit var downloadDao: DownloadDao
    @Inject lateinit var historyRepository: HistoryRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val notification = notificationController.buildProgressNotification("Initializing...", 0, "Starting queue...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationController.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationController.NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            processQueue()
        }
        return START_STICKY
    }

    private suspend fun processQueue() {
        val queue = downloadDao.observeAll().first()
        val pendingTasks = queue.filter { it.status == DownloadStatus.QUEUED.name }

        if (pendingTasks.isEmpty()) {
            stopSelf()
            return
        }

        for (task in pendingTasks) {
            downloadDao.update(task.copy(status = DownloadStatus.DOWNLOADING.name))

            val uri = mediaStoreWriter.createPendingUri(
                displayName = task.targetFilename,
                container = task.formatContainer,
                kind = if (task.formatContainer == "mp3") MediaType.AUDIO else MediaType.VIDEO
            )

            if (uri == null) {
                downloadDao.update(task.copy(status = DownloadStatus.FAILED.name, errorDetail = "Failed to create MediaStore entry"))
                continue
            }

            try {
                val outputStream = mediaStoreWriter.openOutputStream(uri)
                if (outputStream == null) {
                    mediaStoreWriter.abandonUri(uri)
                    downloadDao.update(task.copy(status = DownloadStatus.FAILED.name, errorDetail = "Failed to open OutputStream"))
                    continue
                }

                chunkedDownloader.download(task.downloadUrl, outputStream).collect { progress ->
                    val percent = if (progress.totalBytes > 0) ((progress.bytesDownloaded * 100) / progress.totalBytes).toInt() else 0
                    val speedKb = progress.speedBytesPerSec / 1024
                    val speedText = "$speedKb KB/s"

                    downloadDao.update(
                        task.copy(
                            progress = percent / 100f,
                            downloadedBytes = progress.bytesDownloaded,
                            totalBytes = progress.totalBytes,
                            speedBytesPerSec = progress.speedBytesPerSec
                        )
                    )

                    val notification = notificationController.buildProgressNotification(task.targetFilename, percent, speedText)
                    startForeground(NotificationController.NOTIFICATION_ID, notification)
                }

                mediaStoreWriter.finalizeUri(uri)
                downloadDao.update(task.copy(status = DownloadStatus.COMPLETED.name, progress = 1.0f))
                notificationController.notifyCompleted(task.targetFilename)

            } catch (e: Exception) {
                mediaStoreWriter.abandonUri(uri)
                downloadDao.update(task.copy(status = DownloadStatus.FAILED.name, errorDetail = e.message))
            }
        }

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
