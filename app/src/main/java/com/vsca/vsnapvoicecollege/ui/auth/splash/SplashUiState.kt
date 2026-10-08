package com.vsca.vsnapvoicecollege.ui.auth.splash

/**
 * Immutable UI state for the Splash screen.
 *
 * Once [isReady] is true, the host should navigate based on [isAuthenticated]:
 * authenticated users go to the main graph, others to the auth flow.
 */
data class SplashUiState(
    val isReady: Boolean = false,
    val isAuthenticated: Boolean = false,
)
