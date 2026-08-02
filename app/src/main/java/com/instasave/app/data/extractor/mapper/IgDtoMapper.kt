package com.instasave.app.data.extractor.mapper

import com.instasave.app.data.extractor.dto.IgItemDto
import com.instasave.app.domain.model.Author
import com.instasave.app.domain.model.EngineTag
import com.instasave.app.domain.model.FormatOption
import com.instasave.app.domain.model.MediaInfo
import com.instasave.app.domain.model.MediaItem
import com.instasave.app.domain.model.MediaType
import com.instasave.app.domain.model.PostKind
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IgDtoMapper @Inject constructor() {

    fun mapToMediaInfo(item: IgItemDto, sourceUrl: String, engine: EngineTag): MediaInfo {
        val shortcode = item.code ?: item.shortcode ?: ""
        val postId = item.id ?: shortcode

        val username = item.user?.username ?: item.owner?.username ?: "instagram_user"
        val fullName = item.user?.fullName ?: item.owner?.fullName
        val avatarUrl = item.user?.profilePicUrl ?: item.owner?.profilePicUrl

        val author = Author(username = username, displayName = fullName, avatarUrl = avatarUrl)

        val captionText = item.captionObj?.text
            ?: item.edgeCaptionObj?.edges?.firstOrNull()?.node?.text

        val hashtags = extractHashtags(captionText)

        // Parse Carousel or Single Media
        val items = mutableListOf<MediaItem>()
        val carouselList = item.carouselMedia ?: item.sidecarChildren?.edges?.mapNotNull { it.node }

        if (!carouselList.isNullOrEmpty()) {
            carouselList.forEachIndexed { index, childItem ->
                items.add(mapToMediaItem(childItem, index))
            }
        } else {
            items.add(mapToMediaItem(item, 0))
        }

        val kind = when {
            items.size > 1 -> PostKind.CAROUSEL
            item.isVideo == true -> PostKind.REEL
            else -> PostKind.POST
        }

        return MediaInfo(
            postId = postId,
            shortcode = shortcode,
            sourceUrl = sourceUrl,
            kind = kind,
            author = author,
            caption = captionText,
            hashtags = hashtags,
            takenAtEpochSec = item.takenAtTimestamp,
            isPrivate = false,
            items = items,
            extractedBy = engine
        )
    }

    private fun mapToMediaItem(dto: IgItemDto, index: Int): MediaItem {
        val formats = mutableListOf<FormatOption>()
        val isVideo = dto.isVideo == true || dto.mediaType == 2

        val thumbnailUrl = dto.imageVersions?.candidates?.firstOrNull()?.url

        if (isVideo) {
            val videoVersions = dto.videoVersions ?: emptyList()
            videoVersions.forEachIndexed { fIndex, v ->
                val vUrl = v.url ?: return@forEachIndexed
                val width = v.width ?: 1080
                val height = v.height ?: 1920
                formats.add(
                    FormatOption(
                        id = "video_${index}_$fIndex",
                        container = "mp4",
                        width = width,
                        height = height,
                        url = vUrl
                    )
                )
            }
            // Add Audio extraction option if video present
            if (formats.isNotEmpty()) {
                val bestVideoUrl = formats.first().url
                formats.add(
                    FormatOption(
                        id = "audio_${index}_mp3",
                        container = "mp3",
                        codec = "mp3",
                        url = bestVideoUrl,
                        requiresTranscode = true
                    )
                )
            }
        } else {
            val candidates = dto.imageVersions?.candidates ?: emptyList()
            candidates.forEachIndexed { fIndex, c ->
                val cUrl = c.url ?: return@forEachIndexed
                formats.add(
                    FormatOption(
                        id = "image_${index}_$fIndex",
                        container = "jpg",
                        width = c.width ?: 1080,
                        height = c.height ?: 1080,
                        url = cUrl
                    )
                )
            }
        }

        val width = formats.firstOrNull()?.width ?: 1080
        val height = formats.firstOrNull()?.height ?: 1080

        return MediaItem(
            id = dto.id ?: "item_$index",
            index = index,
            type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
            thumbnailUrl = thumbnailUrl,
            width = width,
            height = height,
            formats = formats
        )
    }

    private fun extractHashtags(caption: String?): List<String> {
        if (caption.isNullOrEmpty()) return emptyList()
        return Regex("""#\w+""").findAll(caption).map { it.value }.toList()
    }
}
