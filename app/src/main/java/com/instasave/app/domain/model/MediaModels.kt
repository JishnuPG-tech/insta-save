package com.instasave.app.domain.model

enum class PostKind { REEL, POST, CAROUSEL, STORY, HIGHLIGHT, IGTV, PROFILE_PICTURE }
enum class MediaType { IMAGE, VIDEO, AUDIO }
enum class EngineTag { NATIVE, YTDLP }

data class Author(
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

data class FormatOption(
    val id: String,
    val container: String,          // mp4 | m4a | mp3 | flac | jpg | webp
    val codec: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val bitrateKbps: Int? = null,
    val approxSizeBytes: Long? = null,
    val url: String,
    val requiresMux: Boolean = false,
    val requiresTranscode: Boolean = false
)

data class MediaItem(
    val id: String,
    val index: Int,
    val type: MediaType,
    val thumbnailUrl: String?,
    val durationMs: Long? = null,
    val width: Int = 0,
    val height: Int = 0,
    val formats: List<FormatOption>
) {
    val bestFormat: FormatOption?
        get() = formats.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
}

data class MediaInfo(
    val postId: String,
    val shortcode: String,
    val sourceUrl: String,
    val kind: PostKind,
    val author: Author,
    val caption: String?,
    val hashtags: List<String> = emptyList(),
    val takenAtEpochSec: Long? = null,
    val isPrivate: Boolean = false,
    val items: List<MediaItem>,
    val extractedBy: EngineTag
)
