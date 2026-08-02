package com.instasave.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_queue")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val shortcode: String,
    val sourceUrl: String,
    val authorUsername: String,
    val selectedIndex: Int,
    val formatId: String,
    val formatContainer: String,
    val downloadUrl: String,
    val targetFilename: String,
    val destinationType: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val status: String,
    val errorDetail: String?,
    val saveCaptionSidecar: Boolean,
    val createdAtEpochMs: Long
)
