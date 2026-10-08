package com.vsca.vsnapvoicecollege.ui.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Decides the initial destination once the app has finished any startup work
 * (e.g. reading the stored session / auth token).
 */
class SplashViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            // TODO: replace with a real session check from the auth repository.
            delay(SPLASH_MIN_DURATION_MS)
            _uiState.update {
                it.copy(isReady = true, isAuthenticated = false)
            }
        }
    }

    private companion object {
        const val SPLASH_MIN_DURATION_MS = 1_000L
    }
}
