package com.instasave.app.data.extractor

import android.content.Context
import com.instasave.app.data.security.YtDlpCookieExporter
import com.instasave.app.domain.model.Author
import com.instasave.app.domain.model.EngineTag
import com.instasave.app.domain.model.FormatOption
import com.instasave.app.domain.model.InstagramUrl
import com.instasave.app.domain.model.MediaInfo
import com.instasave.app.domain.model.MediaItem
import com.instasave.app.domain.model.MediaType
import com.instasave.app.domain.model.PostKind
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpAndroidEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cookieExporter: YtDlpCookieExporter
) {
    private var isInitialized = false

    suspend fun ensureInitialized(ioDispatcher: CoroutineDispatcher) = withContext(ioDispatcher) {
        if (!isInitialized) {
            try {
                YoutubeDL.getInstance().init(context)
                isInitialized = true
            } catch (_: Exception) {}
        }
    }

    suspend fun parse(url: InstagramUrl, ioDispatcher: CoroutineDispatcher): Result<MediaInfo> = withContext(ioDispatcher) {
        ensureInitialized(ioDispatcher)
        var tempCookieFile: File? = null

        try {
            tempCookieFile = cookieExporter.createTempCookieFile()

            val request = YoutubeDLRequest(url.normalizedUrl).apply {
                addOption("--dump-single-json")
                addOption("--no-warnings")
                if (tempCookieFile != null && tempCookieFile.exists()) {
                    addOption("--cookies", tempCookieFile.absolutePath)
                }
            }

            val response = YoutubeDL.getInstance().execute(request)
            val jsonStr = response.out

            if (!jsonStr.isNullOrEmpty()) {
                val jsonObj = JSONObject(jsonStr)
                val title = jsonObj.optString("title", "")
                val uploader = jsonObj.optString("uploader", "instagram_user")
                val webpageUrl = jsonObj.optString("webpage_url", url.normalizedUrl)

                val formatsList = mutableListOf<FormatOption>()
                val formatsArray = jsonObj.optJSONArray("formats")
                if (formatsArray != null) {
                    for (i in 0 until formatsArray.length()) {
                        val f = formatsArray.getJSONObject(i)
                        val fUrl = f.optString("url")
                        val ext = f.optString("ext", "mp4")
                        val width = f.optInt("width", 1080)
                        val height = f.optInt("height", 1920)

                        if (fUrl.isNotEmpty()) {
                            formatsList.add(
                                FormatOption(
                                    id = "ytdlp_$i",
                                    container = ext,
                                    width = width,
                                    height = height,
                                    url = fUrl
                                )
                            )
                        }
                    }
                }

                val items = listOf(
                    MediaItem(
                        id = url.shortcodeOrId,
                        index = 0,
                        type = MediaType.VIDEO,
                        thumbnailUrl = jsonObj.optString("thumbnail", null),
                        width = formatsList.firstOrNull()?.width ?: 1080,
                        height = formatsList.firstOrNull()?.height ?: 1920,
                        formats = formatsList
                    )
                )

                val mediaInfo = MediaInfo(
                    postId = url.shortcodeOrId,
                    shortcode = url.shortcodeOrId,
                    sourceUrl = webpageUrl,
                    kind = PostKind.REEL,
                    author = Author(username = uploader),
                    caption = title,
                    items = items,
                    extractedBy = EngineTag.YTDLP
                )

                return@withContext Result.success(mediaInfo)
            }

            Result.failure(Exception("yt-dlp returned empty JSON response"))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Delete temp cookie file safely in finally block
            tempCookieFile?.delete()
        }
    }
}
