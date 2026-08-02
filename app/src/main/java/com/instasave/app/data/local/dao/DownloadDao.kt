package com.instasave.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.instasave.app.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_queue ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download_queue WHERE id = :id")
    suspend fun getById(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DownloadEntity>)

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("DELETE FROM download_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM download_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()
}
