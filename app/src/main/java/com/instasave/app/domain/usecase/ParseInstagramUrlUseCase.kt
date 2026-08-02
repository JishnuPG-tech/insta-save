package com.instasave.app.domain.usecase

import com.instasave.app.domain.model.InstagramUrl
import com.instasave.app.domain.model.PostKind
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParseInstagramUrlUseCase @Inject constructor() {

    private object IgPatterns {
        val POST = Regex("""instagram\.com/(?:[\w.]+/)?p/([A-Za-z0-9_-]{5,20})""")
        val REEL = Regex("""instagram\.com/(?:[\w.]+/)?reels?/([A-Za-z0-9_-]{5,20})""")
        val IGTV = Regex("""instagram\.com/(?:[\w.]+/)?tv/([A-Za-z0-9_-]{5,20})""")
        val STORY = Regex("""instagram\.com/stories/([\w.]+)/(?:(\d+))?""")
        val HIGHLIGHT = Regex("""instagram\.com/stories/highlights/(\d+)""")
        val PROFILE = Regex("""instagram\.com/([\w.]{1,30})/?$""")
        val SHARE = Regex("""instagram\.com/share/([A-Za-z0-9_-]+)""")
    }

    private val reservedWords = setOf(
        "p", "reel", "reels", "tv", "stories", "explore",
        "accounts", "direct", "about", "developer", "legal", "api", "share"
    )

    operator fun invoke(rawInput: String): Result<InstagramUrl> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("URL input cannot be empty"))
        }

        // Add scheme if missing
        var candidateUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        // Validate domain host strictly (prevent host suffix attacks like instagram.com.evil.com)
        val hostMatch = Regex("""^https?://(?:www\.|m\.)?instagram\.com(?:[:/]|$)""", RegexOption.IGNORE_CASE).find(candidateUrl)
            ?: return Result.failure(IllegalArgumentException("Invalid host: URL must belong to instagram.com"))


        // Strip tracking parameters
        candidateUrl = candidateUrl.replace(Regex("""\?(?:igsh|igshid|utm_[^&]+|hl)=[^&]*&?"""), "?")
            .trimEnd('?', '&')

        // Match against patterns
        IgPatterns.POST.find(candidateUrl)?.let { match ->
            val shortcode = match.groupValues[1]
            return Result.success(
                InstagramUrl(
                    rawUrl = rawInput,
                    normalizedUrl = "https://www.instagram.com/p/$shortcode/",
                    kind = PostKind.POST,
                    shortcodeOrId = shortcode
                )
            )
        }

        IgPatterns.REEL.find(candidateUrl)?.let { match ->
            val shortcode = match.groupValues[1]
            return Result.success(
                InstagramUrl(
                    rawUrl = rawInput,
                    normalizedUrl = "https://www.instagram.com/reel/$shortcode/",
                    kind = PostKind.REEL,
                    shortcodeOrId = shortcode
                )
            )
        }

        IgPatterns.IGTV.find(candidateUrl)?.let { match ->
            val shortcode = match.groupValues[1]
            return Result.success(
                InstagramUrl(
                    rawUrl = rawInput,
                    normalizedUrl = "https://www.instagram.com/tv/$shortcode/",
                    kind = PostKind.IGTV,
                    shortcodeOrId = shortcode
                )
            )
        }

        IgPatterns.HIGHLIGHT.find(candidateUrl)?.let { match ->
            val highlightId = match.groupValues[1]
            return Result.success(
                InstagramUrl(
                    rawUrl = rawInput,
                    normalizedUrl = "https://www.instagram.com/stories/highlights/$highlightId/",
                    kind = PostKind.HIGHLIGHT,
                    shortcodeOrId = highlightId
                )
            )
        }

        IgPatterns.STORY.find(candidateUrl)?.let { match ->
            val username = match.groupValues[1]
            val storyId = match.groupValues.getOrNull(2) ?: ""
            if (!reservedWords.contains(username.lowercase())) {
                return Result.success(
                    InstagramUrl(
                        rawUrl = rawInput,
                        normalizedUrl = "https://www.instagram.com/stories/$username/$storyId",
                        kind = PostKind.STORY,
                        shortcodeOrId = if (storyId.isNotEmpty()) storyId else username
                    )
                )
            }
        }

        IgPatterns.SHARE.find(candidateUrl)?.let { match ->
            val shareId = match.groupValues[1]
            return Result.success(
                InstagramUrl(
                    rawUrl = rawInput,
                    normalizedUrl = "https://www.instagram.com/share/$shareId/",
                    kind = PostKind.POST,
                    shortcodeOrId = shareId,
                    needsRedirectResolution = true
                )
            )
        }

        IgPatterns.PROFILE.find(candidateUrl)?.let { match ->
            val handle = match.groupValues[1]
            if (!reservedWords.contains(handle.lowercase())) {
                return Result.success(
                    InstagramUrl(
                        rawUrl = rawInput,
                        normalizedUrl = "https://www.instagram.com/$handle/",
                        kind = PostKind.PROFILE_PICTURE,
                        shortcodeOrId = handle
                    )
                )
            }
        }

        return Result.failure(IllegalArgumentException("Unrecognized or unsupported Instagram URL format"))
    }
}
