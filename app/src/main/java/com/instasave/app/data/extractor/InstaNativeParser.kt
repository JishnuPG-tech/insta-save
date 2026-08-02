package com.instasave.app.data.extractor

import com.instasave.app.data.extractor.dto.IgMediaContainerDto
import com.instasave.app.data.extractor.mapper.IgDtoMapper
import com.instasave.app.data.network.OkHttpProvider
import com.instasave.app.domain.model.EngineTag
import com.instasave.app.domain.model.ExtractionError
import com.instasave.app.domain.model.InstagramUrl
import com.instasave.app.domain.model.MediaInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstaNativeParser @Inject constructor(
    private val okHttpProvider: OkHttpProvider,
    private val mapper: IgDtoMapper,
    private val json: Json
) {
    suspend fun parse(url: InstagramUrl, ioDispatcher: CoroutineDispatcher): Result<MediaInfo> = withContext(ioDispatcher) {
        val client = okHttpProvider.getClient(isDebug = false)
        val shortcode = url.shortcodeOrId

        // Rung 1: GET /p/{shortcode}/?__a=1&__d=dis
        try {
            val rung1Request = Request.Builder()
                .url("https://www.instagram.com/p/$shortcode/?__a=1&__d=dis")
                .get()
                .build()

            client.newCall(rung1Request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrEmpty() && isNotLoginWall(bodyStr)) {
                        val container = json.decodeFromString<IgMediaContainerDto>(bodyStr)
                        val item = container.items?.firstOrNull() ?: container.graphql?.shortcodeMedia
                        if (item != null) {
                            return@withContext Result.success(mapper.mapToMediaInfo(item, url.rawUrl, EngineTag.NATIVE))
                        }
                    } else if (isLoginWall(response.request.url.toString(), bodyStr)) {
                        return@withContext Result.failure(Exception("LOGIN_REQUIRED"))
                    }
                }
            }
        } catch (e: Exception) {
            if (e.message == "LOGIN_REQUIRED") {
                return@withContext Result.failure(Exception("LOGIN_REQUIRED"))
            }
        }

        // Rung 2: GET /p/{shortcode}/embed/captioned/ (Jsoup html parsing)
        try {
            val rung2Request = Request.Builder()
                .url("https://www.instagram.com/p/$shortcode/embed/captioned/")
                .get()
                .build()

            client.newCall(rung2Request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string()
                    if (!html.isNullOrEmpty()) {
                        val doc = Jsoup.parse(html ?: "")
                        val videoElem = doc.select("video.EmbeddedMediaImage").firstOrNull()
                            ?: doc.select("video").firstOrNull()
                        val imageElem = doc.select("img.EmbeddedMediaImage").firstOrNull()

                        val videoUrl = videoElem?.attr("src")
                        val imageUrl = imageElem?.attr("src")

                        if (!videoUrl.isNullOrEmpty() || !imageUrl.isNullOrEmpty()) {
                            // Synthesize minimal DTO from embed HTML
                            val captionText = doc.select(".Caption").text()
                            val username = doc.select(".Username").text().ifEmpty { "instagram_user" }

                            val mockItem = com.instasave.app.data.extractor.dto.IgItemDto(
                                id = shortcode,
                                code = shortcode,
                                isVideo = !videoUrl.isNullOrEmpty(),
                                videoVersions = if (!videoUrl.isNullOrEmpty()) listOf(com.instasave.app.data.extractor.dto.IgVideoVersionDto(url = videoUrl, width = 1080, height = 1920)) else null,
                                imageVersions = if (!imageUrl.isNullOrEmpty()) com.instasave.app.data.extractor.dto.IgImageContainerDto(listOf(com.instasave.app.data.extractor.dto.IgImageCandidateDto(url = imageUrl, width = 1080, height = 1080))) else null,
                                captionObj = com.instasave.app.data.extractor.dto.IgCaptionDto(text = captionText),
                                user = com.instasave.app.data.extractor.dto.IgUserDto(username = username)
                            )
                            return@withContext Result.success(mapper.mapToMediaInfo(mockItem, url.rawUrl, EngineTag.NATIVE))
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Rung 3: GET /p/{shortcode}/ (JSON-LD from <head>)
        try {
            val rung3Request = Request.Builder()
                .url("https://www.instagram.com/p/$shortcode/")
                .get()
                .build()

            client.newCall(rung3Request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val html = response.body?.string()
                if (isLoginWall(finalUrl, html)) {
                    return@withContext Result.failure(Exception("LOGIN_REQUIRED"))
                }
            }
        } catch (e: Exception) {
            if (e.message == "LOGIN_REQUIRED") {
                return@withContext Result.failure(Exception("LOGIN_REQUIRED"))
            }
        }

        Result.failure(Exception("Native parsing failed across all rungs"))
    }

    private fun isLoginWall(finalUrl: String, body: String?): Boolean {
        if (finalUrl.contains("/accounts/login/")) return true
        if (body.isNullOrEmpty()) return false
        return body.contains("login_required") || body.contains("loginForm")
    }

    private fun isNotLoginWall(body: String?): Boolean = !isLoginWall("", body)

    private fun String?.isNullEmpty(): Boolean = this == null || this.trim().isEmpty()
}
