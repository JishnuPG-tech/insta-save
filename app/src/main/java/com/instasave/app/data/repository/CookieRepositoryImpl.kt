package com.instasave.app.data.repository

import com.instasave.app.data.security.EncryptedCookieStore
import com.instasave.app.domain.model.SessionState
import com.instasave.app.domain.repository.CookieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieRepositoryImpl @Inject constructor(
    private val cookieStore: EncryptedCookieStore
) : CookieRepository {

    private val _sessionState = MutableStateFlow<SessionState>(calculateState())

    override fun observeSession(): Flow<SessionState> = _sessionState.asStateFlow()

    override suspend fun save(cookies: Map<String, String>, handle: String?, userId: String?) {
        cookieStore.put(cookies, handle, userId)
        _sessionState.value = calculateState()
    }

    override suspend fun cookieHeader(): String? = cookieStore.cookieHeader()

    override suspend fun clear() {
        cookieStore.clear()
        _sessionState.value = SessionState.SignedOut
    }

    private fun calculateState(): SessionState {
        val handle = cookieStore.handle()
        val userId = cookieStore.userId()
        return if (cookieStore.cookieHeader() != null) {
            SessionState.SignedIn(handle = handle, userId = userId)
        } else {
            SessionState.SignedOut
        }
    }
}
