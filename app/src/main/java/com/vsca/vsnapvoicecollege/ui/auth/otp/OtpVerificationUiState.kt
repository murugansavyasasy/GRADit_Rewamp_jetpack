package com.vsca.vsnapvoicecollege.ui.auth.otp

/**
 * Immutable UI state for the OTP verification screen.
 */
data class OtpVerificationUiState(
    val otp: String = "",
    val maskedTarget: String = "",
    val resendSecondsRemaining: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isVerified: Boolean = false,
) {
    val canResend: Boolean get() = resendSecondsRemaining <= 0
    val isComplete: Boolean get() = otp.length == OTP_LENGTH

    companion object {
        const val OTP_LENGTH = 6
    }
}
