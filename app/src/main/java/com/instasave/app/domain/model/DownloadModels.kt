package com.instasave.app.domain.model

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

enum class Destination { DEFAULT, SAF_CUSTOM }

data class DownloadTask(
    val id: String,
    val mediaInfo: MediaInfo,
    val selectedItemIndex: Int,
    val selectedFormat: FormatOption,
    val targetFilename: String,
    val destination: Destination = Destination.DEFAULT,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorDetail: String? = null,
    val saveCaptionSidecar: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
