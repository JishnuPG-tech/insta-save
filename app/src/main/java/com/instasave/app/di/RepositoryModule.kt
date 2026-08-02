package com.instasave.app.di

import com.instasave.app.data.repository.CookieRepositoryImpl
import com.instasave.app.data.repository.DownloadRepositoryImpl
import com.instasave.app.data.repository.HistoryRepositoryImpl
import com.instasave.app.data.repository.MediaRepositoryImpl
import com.instasave.app.data.repository.SettingsRepositoryImpl
import com.instasave.app.domain.repository.CookieRepository
import com.instasave.app.domain.repository.DownloadRepository
import com.instasave.app.domain.repository.HistoryRepository
import com.instasave.app.domain.repository.MediaRepository
import com.instasave.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindCookieRepository(impl: CookieRepositoryImpl): CookieRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
