package com.instasave.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.instasave.app.domain.repository.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "instasave_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SAVE_CAPTION_SIDECARS = booleanPreferencesKey("save_caption_sidecars")
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val PREFERRED_ENGINE = stringPreferencesKey("preferred_engine")
        val CUSTOM_SAF_URI = stringPreferencesKey("custom_saf_uri")
        val FILENAME_TEMPLATE = stringPreferencesKey("filename_template")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            saveCaptionSidecars = prefs[Keys.SAVE_CAPTION_SIDECARS] ?: true,
            maxConcurrentDownloads = prefs[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 3,
            preferredEngine = prefs[Keys.PREFERRED_ENGINE] ?: "AUTO",
            customSafUri = prefs[Keys.CUSTOM_SAF_URI],
            filenameTemplate = prefs[Keys.FILENAME_TEMPLATE] ?: "{author}_{shortcode}_{index}"
        )
    }

    suspend fun updateSaveCaptionSidecars(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SAVE_CAPTION_SIDECARS] = enabled }
    }

    suspend fun updateMaxConcurrentDownloads(max: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.MAX_CONCURRENT_DOWNLOADS] = max }
    }

    suspend fun updatePreferredEngine(engine: String) {
        context.dataStore.edit { prefs -> prefs[Keys.PREFERRED_ENGINE] = engine }
    }

    suspend fun updateCustomSafUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[Keys.CUSTOM_SAF_URI] = uri else prefs.remove(Keys.CUSTOM_SAF_URI)
        }
    }

    suspend fun updateFilenameTemplate(template: String) {
        context.dataStore.edit { prefs -> prefs[Keys.FILENAME_TEMPLATE] = template }
    }
}
