package com.instasave.app.domain.repository

import com.instasave.app.domain.model.InstagramUrl
import com.instasave.app.domain.model.MediaInfo

interface MediaRepository {
    suspend fun extract(url: InstagramUrl): Result<MediaInfo>
    suspend fun invalidate(shortcode: String)
}
