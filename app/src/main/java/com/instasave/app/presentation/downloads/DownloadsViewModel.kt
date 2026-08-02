package com.instasave.app.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasave.app.domain.repository.DownloadRepository
import com.instasave.app.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadRepository.observeQueue().collect { queue ->
                _uiState.update { it.copy(activeQueue = queue) }
            }
        }
        viewModelScope.launch {
            historyRepository.observeHistory().collect { hist ->
                _uiState.update { it.copy(history = hist) }
            }
        }
    }

    fun onEvent(event: DownloadsEvent) {
        viewModelScope.launch {
            when (event) {
                is DownloadsEvent.TabSelected -> _uiState.update { it.copy(selectedTab = event.index) }
                is DownloadsEvent.PauseTask -> downloadRepository.pause(event.id)
                is DownloadsEvent.ResumeTask -> downloadRepository.resume(event.id)
                is DownloadsEvent.CancelTask -> downloadRepository.cancel(event.id)
                is DownloadsEvent.RetryTask -> downloadRepository.retry(event.id)
                is DownloadsEvent.DeleteHistoryRecord -> historyRepository.deleteRecord(event.id, deleteFile = false)
                DownloadsEvent.ClearCompletedClicked -> downloadRepository.clearCompleted()
                DownloadsEvent.ClearHistoryClicked -> historyRepository.clearAllHistory()
            }
        }
    }
}
