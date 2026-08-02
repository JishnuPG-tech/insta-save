package com.instasave.app.domain.usecase

import com.instasave.app.domain.model.PostKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParseInstagramUrlUseCaseTest {

    private val useCase = ParseInstagramUrlUseCase()

    @Test
    fun testReelUrlParsing_Success() {
        val url = "https://www.instagram.com/reel/Cx4f2AbCdEf/?igsh=MXY="
        val result = useCase(url)

        assertTrue("Expected parsing success for reel URL", result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(PostKind.REEL, parsed.kind)
        assertEquals("Cx4f2AbCdEf", parsed.shortcodeOrId)
        assertEquals("https://www.instagram.com/reel/Cx4f2AbCdEf/", parsed.normalizedUrl)
    }

    @Test
    fun testPostUrlParsing_Success() {
        val url = "instagram.com/p/Cx4f2AbCdEf"
        val result = useCase(url)

        assertTrue("Expected parsing success for post URL", result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(PostKind.POST, parsed.kind)
        assertEquals("Cx4f2AbCdEf", parsed.shortcodeOrId)
    }

    @Test
    fun testHostSuffixAttack_Rejected() {
        val url = "https://instagram.com.evil.com/p/Cx4f2AbCdEf/"
        val result = useCase(url)

        assertTrue("Host suffix attack should be rejected", result.isFailure)
    }

    @Test
    fun testReservedPath_Rejected() {
        val url = "https://www.instagram.com/accounts/login/"
        val result = useCase(url)

        assertTrue("Reserved path should be rejected", result.isFailure)
    }

    @Test
    fun testNonInstagramHost_Rejected() {
        val url = "https://youtube.com/watch?v=abc"
        val result = useCase(url)

        assertTrue("Non-Instagram host should be rejected", result.isFailure)
    }
}
