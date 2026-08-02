package com.instasave.app.data.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpCookieExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cookieStore: EncryptedCookieStore
) {
    fun createTempCookieFile(): File? {
        val cookieHeader = cookieStore.cookieHeader() ?: return null
        val cacheFile = File(context.cacheDir, "temp_cookies_${System.currentTimeMillis()}.txt")

        val builder = StringBuilder()
        builder.append("# Netscape HTTP Cookie File\n")
        
        cookieHeader.split(";").forEach { part ->
            val kv = part.trim().split("=")
            if (kv.size == 2) {
                val key = kv[0]
                val value = kv[1]
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    builder.append(".instagram.com\tTRUE\t/\tTRUE\t0\t$key\t$value\n")
                }
            }
        }

        cacheFile.writeText(builder.toString())
        return cacheFile
    }
}
