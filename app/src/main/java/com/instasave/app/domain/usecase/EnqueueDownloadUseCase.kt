package com.instasave.app.domain.usecase

import com.instasave.app.domain.model.DownloadTask
import com.instasave.app.domain.repository.DownloadRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnqueueDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(tasks: List<DownloadTask>) {
        downloadRepository.enqueue(tasks)
    }
}
