package com.instasave.app.data.repository

import com.instasave.app.data.extractor.InstagramExtractorManager
import com.instasave.app.data.local.SettingsDataStore
import com.instasave.app.di.IoDispatcher
import com.instasave.app.domain.model.InstagramUrl
import com.instasave.app.domain.model.MediaInfo
import com.instasave.app.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val extractorManager: InstagramExtractorManager,
    private val settingsDataStore: SettingsDataStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MediaRepository {

    override suspend fun extract(url: InstagramUrl): Result<MediaInfo> {
        val appSettings = settingsDataStore.settingsFlow.first()
        return extractorManager.extract(url, appSettings.preferredEngine, ioDispatcher)
    }

    override suspend fun invalidate(shortcode: String) {
        // Cache invalidation logic if needed
    }
}
