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
    /** True once the one-time onboarding intro has been completed on this install. */
    val onboardingCompleted: Boolean = false,
    /** True once the user confirmed their region/country via "Continue". */
    val regionSelected: Boolean = false,
)
