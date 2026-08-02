package com.instasave.app.data.download

import com.instasave.app.data.network.OkHttpProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Request
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long
)

@Singleton
class ChunkedDownloader @Inject constructor(
    private val okHttpProvider: OkHttpProvider
) {
    fun download(
        downloadUrl: String,
        outputStream: OutputStream,
        startByte: Long = 0L
    ): Flow<DownloadProgress> = flow {
        val client = okHttpProvider.getClient(isDebug = false)

        val requestBuilder = Request.Builder().url(downloadUrl).get()
        if (startByte > 0) {
            requestBuilder.header("Range", "bytes=$startByte-")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw java.io.IOException("HTTP error code: ${response.code}")
        }

        val body = response.body ?: throw java.io.IOException("Response body is null")
        val contentLength = body.contentLength()
        val totalBytes = if (contentLength > 0) contentLength + startByte else -1L

        val inputStream = body.byteStream()
        val buffer = ByteArray(8192)
        var bytesDownloaded = startByte
        var bytesSinceLastSample = 0L
        var lastEmitTime = System.currentTimeMillis()
        var currentSpeed = 0L

        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            bytesDownloaded += bytesRead
            bytesSinceLastSample += bytesRead

            val currentTime = System.currentTimeMillis()
            val timeDeltaMs = currentTime - lastEmitTime

            if (timeDeltaMs >= 250) { // Limit emissions to <= 4 Hz (250ms interval)
                val instantaneousSpeed = (bytesSinceLastSample * 1000) / timeDeltaMs
                // EWMA speed calculation
                currentSpeed = if (currentSpeed == 0L) instantaneousSpeed else (currentSpeed * 0.7 + instantaneousSpeed * 0.3).toLong()

                emit(DownloadProgress(bytesDownloaded, totalBytes, currentSpeed))

                lastEmitTime = currentTime
                bytesSinceLastSample = 0L
            }
        }

        outputStream.flush()
        emit(DownloadProgress(bytesDownloaded, totalBytes, 0L))
    }
}
