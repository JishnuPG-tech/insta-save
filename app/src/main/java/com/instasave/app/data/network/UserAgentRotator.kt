package com.instasave.app.data.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAgentRotator @Inject constructor() {
    private val userAgents = listOf(
        "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.178 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.80 Mobile Safari/537.36",
        "Instagram 318.0.0.27.108 Android (33/13; 480dpi; 1080x2400; Samsung; SM-G998B; o1s; exynos2100; en_US; 568393525)"
    )

    fun current(): String {
        return userAgents[0]
    }
}
