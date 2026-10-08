package com.vsca.vsnapvoicecollege.ui.auth.login

import androidx.annotation.StringRes

/**
 * Immutable UI state for the Login screen.
 */
data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val keepSignedIn: Boolean = true,
    val language: String = "English",
    val languageCode: String = "en",
    val showLanguageSheet: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    @param:StringRes val identifierErrorRes: Int? = null,
    @param:StringRes val passwordErrorRes: Int? = null,
    val navigateToForgotPassword: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    // Biometric (UI only for now)
    val isFingerprintEnabled: Boolean = false,
    val showBiometricPrompt: Boolean = false,
    val biometricUserName: String = "",
    val biometricMaskedId: String = "",
)
