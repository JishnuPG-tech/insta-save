package com.instasave.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasave.app.data.storage.FilenameTemplater
import com.instasave.app.domain.model.DownloadStatus
import com.instasave.app.domain.model.DownloadTask
import com.instasave.app.domain.model.ExtractionError
import com.instasave.app.domain.usecase.EnqueueDownloadUseCase
import com.instasave.app.domain.usecase.ExtractMediaUseCase
import com.instasave.app.domain.usecase.ParseInstagramUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val parseInstagramUrlUseCase: ParseInstagramUrlUseCase,
    private val extractMediaUseCase: ExtractMediaUseCase,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase,
    private val filenameTemplater: FilenameTemplater
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UrlInputChanged -> {
                val parseResult = parseInstagramUrlUseCase(event.newUrl)
                _uiState.update {
                    it.copy(
                        urlInput = event.newUrl,
                        urlValid = parseResult.isSuccess,
                        error = null
                    )
                }
            }
            HomeEvent.ExtractClicked -> extractMedia()
            HomeEvent.ClipboardChipTapped -> {
                val suggestion = _uiState.value.clipboardSuggestion ?: return
                onEvent(HomeEvent.UrlInputChanged(suggestion))
                extractMedia()
            }
            HomeEvent.DismissClipboardChip -> {
                _uiState.update { it.copy(clipboardSuggestion = null) }
            }
            HomeEvent.DismissFormatSheet -> {
                _uiState.update { it.copy(isFormatSheetOpen = false) }
            }
            is HomeEvent.ConfirmDownload -> confirmDownload(event.selectedItemIndices, event.selectedFormatId)
        }
    }

    fun setClipboardSuggestion(url: String) {
        val parseResult = parseInstagramUrlUseCase(url)
        if (parseResult.isSuccess && url != _uiState.value.urlInput) {
            _uiState.update { it.copy(clipboardSuggestion = url) }
        }
    }

    private fun extractMedia() {
        val input = _uiState.value.urlInput
        val parseResult = parseInstagramUrlUseCase(input)

        if (parseResult.isFailure) {
            _uiState.update { it.copy(error = ExtractionError.InvalidUrl) }
            return
        }

        val igUrl = parseResult.getOrThrow()

        viewModelScope.launch {
            _uiState.update { it.copy(phase = ExtractPhase.EXTRACTING_NATIVE, error = null) }

            val result = extractMediaUseCase(igUrl)

            if (result.isSuccess) {
                val mediaInfo = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        phase = ExtractPhase.READY,
                        mediaInfo = mediaInfo,
                        isFormatSheetOpen = true
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                val error = if (exception?.message == "LOGIN_REQUIRED") {
                    ExtractionError.LoginRequired
                } else {
                    ExtractionError.Unknown(exception?.message ?: "Extraction failed")
                }
                _uiState.update { it.copy(phase = ExtractPhase.IDLE, error = error) }
            }
        }
    }

    private fun confirmDownload(selectedItemIndices: List<Int>, selectedFormatId: String) {
        val mediaInfo = _uiState.value.mediaInfo ?: return

        viewModelScope.launch {
            val tasks = mutableListOf<DownloadTask>()

            selectedItemIndices.forEach { itemIndex ->
                val item = mediaInfo.items.getOrNull(itemIndex) ?: return@forEach
                val format = item.formats.find { it.id == selectedFormatId } ?: item.bestFormat ?: return@forEach

                val filename = filenameTemplater.format(
                    template = "{author}_{shortcode}_{index}",
                    author = mediaInfo.author.username,
                    shortcode = mediaInfo.shortcode,
                    index = itemIndex,
                    extension = format.container
                )

                tasks.add(
                    DownloadTask(
                        id = "${mediaInfo.shortcode}_${itemIndex}_${System.currentTimeMillis()}",
                        mediaInfo = mediaInfo,
                        selectedItemIndex = itemIndex,
                        selectedFormat = format,
                        targetFilename = filename,
                        status = DownloadStatus.QUEUED
                    )
                )
            }

            enqueueDownloadUseCase(tasks)
            _uiState.update { it.copy(isFormatSheetOpen = false, phase = ExtractPhase.IDLE) }
        }
    }
}
