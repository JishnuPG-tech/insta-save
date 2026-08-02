package com.instasave.app.data.repository

import com.instasave.app.data.local.SettingsDataStore
import com.instasave.app.domain.repository.AppSettings
import com.instasave.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = settingsDataStore.settingsFlow

    override suspend fun updateSaveCaptionSidecars(enabled: Boolean) {
        settingsDataStore.updateSaveCaptionSidecars(enabled)
    }

    override suspend fun updateMaxConcurrentDownloads(max: Int) {
        settingsDataStore.updateMaxConcurrentDownloads(max)
    }

    override suspend fun updatePreferredEngine(engine: String) {
        settingsDataStore.updatePreferredEngine(engine)
    }

    override suspend fun updateCustomSafUri(uri: String?) {
        settingsDataStore.updateCustomSafUri(uri)
    }

    override suspend fun updateFilenameTemplate(template: String) {
        settingsDataStore.updateFilenameTemplate(template)
    }
}
