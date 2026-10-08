package com.vsca.vsnapvoicecollege.ui.auth.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.OnboardingPreferences
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Decides the initial destination once the app has finished any startup work
 * (e.g. reading the stored session / auth token and the onboarding flag).
 */
class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        // Read synchronously so the "Get started" button's visibility is correct
        // from the first frame (no flash on repeat launches).
        SplashUiState(
            onboardingCompleted = OnboardingPreferences.isCompleted(application),
            isAuthenticated = SessionPreferences.isLoggedIn(application),
        ),
    )
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            // TODO: replace with a real session check from the auth repository.
            delay(SPLASH_MIN_DURATION_MS)
            _uiState.update { it.copy(isReady = true) }
        }
    }

    private companion object {
        const val SPLASH_MIN_DURATION_MS = 1_000L
    }
}
