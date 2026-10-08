package com.vsca.vsnapvoicecollege.ui.auth.login

import androidx.lifecycle.ViewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds and exposes the state for [LoginScreen] and handles user intents.
 */
class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // TODO: only when a saved session + enrolled biometric exist. Stubbed for UI.
        _uiState.update {
            it.copy(
                isFingerprintEnabled = true,
                biometricUserName = "Rajesh Sharma",
                biometricMaskedId = "+91 ●●●●●● 3210",
            )
        }
    }

    fun onFingerprintClick() {
        _uiState.update { it.copy(showBiometricPrompt = true) }
    }

    fun onIdentifierChange(value: String) {
        // Mobile number only: keep digits and cap at 10.
        val digits = value.filter { it.isDigit() }.take(MAX_MOBILE_DIGITS)
        _uiState.update {
            it.copy(identifier = digits, errorMessage = null, identifierErrorRes = null)
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, passwordErrorRes = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onKeepSignedInChange(checked: Boolean) {
        _uiState.update { it.copy(keepSignedIn = checked) }
    }

    fun onLanguageClick() {
        _uiState.update { it.copy(showLanguageSheet = true) }
    }

    fun onDismissLanguageSheet() {
        _uiState.update { it.copy(showLanguageSheet = false) }
    }

    fun onLanguageSelected(language: Language) {
        // TODO: persist the locale and recreate the UI with the new language.
        _uiState.update {
            it.copy(
                language = language.englishName,
                languageCode = language.code,
                showLanguageSheet = false,
            )
        }
    }

    fun onLoginClick() {
        val identifier = _uiState.value.identifier.trim()
        val password = _uiState.value.password

        val identifierError = when {
            identifier.isEmpty() -> R.string.login_error_enter_identifier
            identifier.length != MAX_MOBILE_DIGITS -> R.string.login_error_invalid_mobile
            else -> null
        }
        val passwordError = if (password.isEmpty()) R.string.login_error_enter_password else null

        if (identifierError != null || passwordError != null) {
            _uiState.update {
                it.copy(identifierErrorRes = identifierError, passwordErrorRes = passwordError)
            }
            return
        }

        // Valid input — clear errors and proceed.
        // TODO: replace this with a real auth repository call; success is stubbed.
        _uiState.update {
            it.copy(identifierErrorRes = null, passwordErrorRes = null, isLoginSuccessful = true)
        }
    }

    /** Consumes the one-time login-success event after navigation has happened. */
    fun onLoginNavigated() {
        _uiState.update { it.copy(isLoginSuccessful = false) }
    }

    /** Validates the mobile number before allowing navigation to forgot-password. */
    fun onForgotPasswordClick() {
        val identifier = _uiState.value.identifier.trim()
        val errorRes = when {
            identifier.isEmpty() -> R.string.login_error_enter_mobile
            identifier.length != MAX_MOBILE_DIGITS -> R.string.login_error_invalid_mobile
            else -> null
        }
        if (errorRes != null) {
            _uiState.update { it.copy(identifierErrorRes = errorRes) }
        } else {
            _uiState.update { it.copy(identifierErrorRes = null, navigateToForgotPassword = true) }
        }
    }

    fun onForgotPasswordNavigated() {
        _uiState.update { it.copy(navigateToForgotPassword = false) }
    }

    fun onDismissBiometricPrompt() {
        _uiState.update { it.copy(showBiometricPrompt = false) }
    }

    fun onAuthenticateWithBiometric() {
        // TODO: trigger androidx.biometric BiometricPrompt and handle the result.
        _uiState.update { it.copy(showBiometricPrompt = false) }
    }

    private companion object {
        const val MAX_MOBILE_DIGITS = 10
    }
}
