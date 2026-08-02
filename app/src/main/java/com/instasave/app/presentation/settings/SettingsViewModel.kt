package com.instasave.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasave.app.domain.repository.AppSettings
import com.instasave.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun onSaveCaptionSidecarsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSaveCaptionSidecars(enabled) }
    }

    fun onMaxConcurrentChanged(max: Int) {
        viewModelScope.launch { settingsRepository.updateMaxConcurrentDownloads(max) }
    }

    fun onPreferredEngineChanged(engine: String) {
        viewModelScope.launch { settingsRepository.updatePreferredEngine(engine) }
    }

    fun onFilenameTemplateChanged(template: String) {
        viewModelScope.launch { settingsRepository.updateFilenameTemplate(template) }
    }
}
