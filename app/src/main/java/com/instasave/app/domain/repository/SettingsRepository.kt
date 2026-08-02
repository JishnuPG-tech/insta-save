package com.instasave.app.domain.repository

import kotlinx.coroutines.flow.Flow

data class AppSettings(
    val saveCaptionSidecars: Boolean = true,
    val maxConcurrentDownloads: Int = 3,
    val preferredEngine: String = "AUTO", // AUTO | NATIVE_ONLY | YTDLP_ONLY
    val customSafUri: String? = null,
    val filenameTemplate: String = "{author}_{shortcode}_{index}"
)

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateSaveCaptionSidecars(enabled: Boolean)
    suspend fun updateMaxConcurrentDownloads(max: Int)
    suspend fun updatePreferredEngine(engine: String)
    suspend fun updateCustomSafUri(uri: String?)
    suspend fun updateFilenameTemplate(template: String)
}
