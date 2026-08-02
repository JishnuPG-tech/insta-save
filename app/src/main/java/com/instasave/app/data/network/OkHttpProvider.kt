package com.instasave.app.data.network

import com.instasave.app.data.security.EncryptedCookieStore
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpProvider @Inject constructor(
    private val allowlistInterceptor: NetworkAllowlistInterceptor,
    private val userAgentRotator: UserAgentRotator,
    private val cookieStore: EncryptedCookieStore
) {
    fun getClient(isDebug: Boolean = false): OkHttpClient {
        val modernTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .cipherSuites(
                CipherSuite.TLS_AES_128_GCM_SHA256,
                CipherSuite.TLS_AES_256_GCM_SHA384,
                CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
            )
            .build()

        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectionSpecs(listOf(modernTlsSpec, ConnectionSpec.CLEARTEXT))
            .addInterceptor(allowlistInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", userAgentRotator.current())
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("X-IG-App-ID", "936619743392459")

                val cookieHeader = cookieStore.cookieHeader()
                if (!cookieHeader.isNullEmpty()) {
                    requestBuilder.header("Cookie", cookieHeader)
                }

                chain.proceed(requestBuilder.build())
            }

        if (isDebug) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    private fun String?.isNullEmpty(): Boolean = this == null || this.trim().isEmpty()
}
