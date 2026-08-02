package com.instasave.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instasave.app.domain.model.SessionState
import com.instasave.app.domain.repository.CookieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val cookieRepository: CookieRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = cookieRepository.observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionState.SignedOut)

    fun onCookiesCaptured(cookies: Map<String, String>, handle: String?) {
        viewModelScope.launch {
            val userId = cookies["ds_user_id"]
            cookieRepository.save(cookies, handle, userId)
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            cookieRepository.clear()
        }
    }
}
