package com.instasave.app.data.storage

import android.content.Context
import android.os.Environment
import com.instasave.app.domain.model.MediaInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SidecarWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun writeCaptionSidecar(
        baseFilenameWithoutExt: String,
        mediaInfo: MediaInfo,
        subFolder: String = "InstaSave"
    ): File? {
        val captionText = mediaInfo.caption
        if (captionText.isNullOrEmpty()) return null

        try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetFolder = File(picturesDir, subFolder)
            if (!targetFolder.exists()) targetFolder.mkdirs()

            val sidecarFile = File(targetFolder, "$baseFilenameWithoutExt.txt")
            val content = StringBuilder().apply {
                append("Author: @${mediaInfo.author.username}\n")
                if (!mediaInfo.author.displayName.isNullOrEmpty()) {
                    append("Name: ${mediaInfo.author.displayName}\n")
                }
                append("Source URL: ${mediaInfo.sourceUrl}\n")
                if (mediaInfo.takenAtEpochSec != null) {
                    append("Date: ${java.util.Date(mediaInfo.takenAtEpochSec * 1000)}\n")
                }
                append("----------------------------------------\n\n")
                append(captionText)
                if (mediaInfo.hashtags.isNotEmpty()) {
                    append("\n\nHashtags:\n${mediaInfo.hashtags.joinToString(" ")}")
                }
            }.toString()

            sidecarFile.writeText(content)
            return sidecarFile
        } catch (_: Exception) {
            return null
        }
    }
}
