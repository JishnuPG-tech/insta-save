package com.instasave.app.domain.repository

import com.instasave.app.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

interface CookieRepository {
    fun observeSession(): Flow<SessionState>
    suspend fun save(cookies: Map<String, String>, handle: String?, userId: String?)
    suspend fun cookieHeader(): String?
    suspend fun clear()
}
