package com.instasave.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val shortcode: String,
    val sourceUrl: String,
    val authorUsername: String,
    val kind: String,
    val mediaType: String,
    val targetFilename: String,
    val savedFilePath: String?,
    val savedFileUri: String?,
    val caption: String?,
    val createdAtEpochMs: Long
)
