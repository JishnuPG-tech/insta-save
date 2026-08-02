package com.instasave.app.domain.usecase

import com.instasave.app.domain.model.InstagramUrl
import com.instasave.app.domain.model.MediaInfo
import com.instasave.app.domain.repository.MediaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractMediaUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(url: InstagramUrl): Result<MediaInfo> {
        return mediaRepository.extract(url)
    }
}
