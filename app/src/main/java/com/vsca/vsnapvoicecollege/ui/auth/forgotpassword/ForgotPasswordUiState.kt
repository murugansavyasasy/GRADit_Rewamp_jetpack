package com.vsca.vsnapvoicecollege.ui.auth.forgotpassword

/** Where the password-reset code should be delivered. */
enum class ResetMethod { SMS, EMAIL }

/**
 * Immutable UI state for the Forgot Password screen.
 */
data class ForgotPasswordUiState(
    val selectedMethod: ResetMethod = ResetMethod.SMS,
    val maskedMobile: String = "",
    val maskedEmail: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCodeSent: Boolean = false,
)
