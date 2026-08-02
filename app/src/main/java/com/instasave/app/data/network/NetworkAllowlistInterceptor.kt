package com.instasave.app.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkAllowlistInterceptor @Inject constructor() : Interceptor {

    private val allowedDomainSuffixes = listOf(
        "instagram.com",
        "cdninstagram.com",
        "fbcdn.net"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host.lowercase()

        val isAllowed = allowedDomainSuffixes.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }

        if (!isAllowed) {
            throw IOException("Network security exception: Connection to non-Instagram domain '$host' blocked by Zero-Relay policy.")
        }

        return chain.proceed(request)
    }
}
