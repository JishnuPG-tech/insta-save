package com.instasave.app.presentation.home

import com.instasave.app.domain.model.ExtractionError
import com.instasave.app.domain.model.MediaInfo

enum class ExtractPhase { IDLE, EXTRACTING_NATIVE, EXTRACTING_FALLBACK, READY }

data class HomeUiState(
    val urlInput: String = "",
    val urlValid: Boolean = false,
    val clipboardSuggestion: String? = null,
    val phase: ExtractPhase = ExtractPhase.IDLE,
    val mediaInfo: MediaInfo? = null,
    val error: ExtractionError? = null,
    val isFormatSheetOpen: Boolean = false
)

sealed interface HomeEvent {
    data class UrlInputChanged(val newUrl: String) : HomeEvent
    data object ExtractClicked : HomeEvent
    data object ClipboardChipTapped : HomeEvent
    data object DismissClipboardChip : HomeEvent
    data object DismissFormatSheet : HomeEvent
    data class ConfirmDownload(val selectedItemIndices: List<Int>, val selectedFormatId: String) : HomeEvent
}
