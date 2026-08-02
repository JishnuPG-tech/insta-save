package com.instasave.app.di

import android.content.Context
import androidx.room.Room
import com.instasave.app.data.local.InstaSaveDatabase
import com.instasave.app.data.local.dao.DownloadDao
import com.instasave.app.data.local.dao.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): InstaSaveDatabase {
        return Room.databaseBuilder(
            context,
            InstaSaveDatabase::class.java,
            "instasave.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDownloadDao(db: InstaSaveDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideHistoryDao(db: InstaSaveDatabase): HistoryDao = db.historyDao()
}
