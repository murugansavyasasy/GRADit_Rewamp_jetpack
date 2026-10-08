package com.vsca.vsnapvoicecollege.ui.auth.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds and exposes the state for [OtpVerificationScreen], including the
 * resend countdown.
 */
class OtpVerificationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OtpVerificationUiState())
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        // TODO: receive the real masked target (mobile/email) via nav args.
        _uiState.update { it.copy(maskedTarget = "+91 ●●●●●●3210") }
        startResendTimer()
    }

    /** Displays the masked contact the code was sent to (carried from forgot-password). */
    fun setMaskedTarget(target: String) {
        if (target.isBlank()) return
        _uiState.update { it.copy(maskedTarget = target) }
    }

    fun onOtpChange(value: String) {
        _uiState.update { it.copy(otp = value, errorMessage = null) }
    }

    fun onResendClick() {
        if (!_uiState.value.canResend) return
        // TODO: request a new OTP from the auth repository.
        _uiState.update { it.copy(otp = "") }
        startResendTimer()
    }

    fun onVerifyClick() {
        // TODO: verify the entered OTP with the auth repository.
        _uiState.update { it.copy(isVerified = true) }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _uiState.update { it.copy(resendSecondsRemaining = RESEND_SECONDS) }
            while (_uiState.value.resendSecondsRemaining > 0) {
                delay(1_000)
                _uiState.update { it.copy(resendSecondsRemaining = it.resendSecondsRemaining - 1) }
            }
        }
    }

    private companion object {
        const val RESEND_SECONDS = 60
    }
}
