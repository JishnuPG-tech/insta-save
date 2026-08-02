package com.instasave.app.domain.model

sealed interface SessionState {
    data object SignedOut : SessionState
    data class SignedIn(val handle: String?, val userId: String?) : SessionState
}
