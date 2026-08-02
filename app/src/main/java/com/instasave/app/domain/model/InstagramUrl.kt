package com.instasave.app.domain.model

data class InstagramUrl(
    val rawUrl: String,
    val normalizedUrl: String,
    val kind: PostKind,
    val shortcodeOrId: String,
    val needsRedirectResolution: Boolean = false
)
