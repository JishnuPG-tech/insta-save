package com.instasave.app.presentation.downloads

import com.instasave.app.domain.model.DownloadTask

data class DownloadsUiState(
    val activeQueue: List<DownloadTask> = emptyList(),
    val history: List<DownloadTask> = emptyList(),
    val selectedTab: Int = 0 // 0 = Active Queue, 1 = History
)

sealed interface DownloadsEvent {
    data class TabSelected(val index: Int) : DownloadsEvent
    data class PauseTask(val id: String) : DownloadsEvent
    data class ResumeTask(val id: String) : DownloadsEvent
    data class CancelTask(val id: String) : DownloadsEvent
    data class RetryTask(val id: String) : DownloadsEvent
    data class DeleteHistoryRecord(val id: String) : DownloadsEvent
    data object ClearCompletedClicked : DownloadsEvent
    data object ClearHistoryClicked : DownloadsEvent
}
