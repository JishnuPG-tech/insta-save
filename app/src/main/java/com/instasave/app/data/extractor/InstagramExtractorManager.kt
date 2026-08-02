package com.instasave.app.data.extractor

import com.instasave.app.domain.model.InstagramUrl
import com.instasave.app.domain.model.MediaInfo
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstagramExtractorManager @Inject constructor(
    private val nativeParser: InstaNativeParser,
    private val fallbackEngine: YtDlpAndroidEngine
) {
    suspend fun extract(
        url: InstagramUrl,
        preferredEngine: String,
        ioDispatcher: CoroutineDispatcher
    ): Result<MediaInfo> {
        if (preferredEngine == "YTDLP_ONLY") {
            return fallbackEngine.parse(url, ioDispatcher)
        }

        // Try primary Native parser
        val nativeResult = nativeParser.parse(url, ioDispatcher)
        if (nativeResult.isSuccess) {
            return nativeResult
        }

        val nativeException = nativeResult.exceptionOrNull()
        if (nativeException?.message == "LOGIN_REQUIRED") {
            return Result.failure(nativeException)
        }

        if (preferredEngine == "NATIVE_ONLY") {
            return nativeResult
        }

        // Fallback to yt-dlp engine if preferred engine is AUTO
        return fallbackEngine.parse(url, ioDispatcher)
    }
}
