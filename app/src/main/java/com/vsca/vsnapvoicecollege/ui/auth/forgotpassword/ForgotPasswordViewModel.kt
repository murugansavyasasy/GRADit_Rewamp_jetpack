package com.vsca.vsnapvoicecollege.ui.auth.forgotpassword

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds and exposes the state for [ForgotPasswordScreen] and handles user intents.
 */
class ForgotPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    init {
        // TODO: load the real masked contact details for the signed-in account.
        _uiState.update {
            it.copy(
                maskedMobile = "+91 ●●●●●● 3210",
                maskedEmail = "pa●●●●●@gmail.com",
            )
        }
    }

    /** Masks and displays the mobile number carried over from the login screen. */
    fun setEnteredMobile(rawMobile: String) {
        if (rawMobile.isBlank()) return
        _uiState.update { it.copy(maskedMobile = maskMobile(rawMobile)) }
    }

    fun onMethodSelected(method: ResetMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun onSendCodeClick() {
        // TODO: request the reset code via the selected method, then surface errors.
        _uiState.update { it.copy(isCodeSent = true) }
    }

    private fun maskMobile(rawMobile: String): String {
        val digits = rawMobile.filter { it.isDigit() }
        if (digits.length < VISIBLE_DIGITS) return rawMobile
        val last = digits.takeLast(VISIBLE_DIGITS)
        val maskedCount = (digits.length - VISIBLE_DIGITS).coerceAtLeast(MIN_MASK_DOTS)
        val dots = MASK_CHAR.toString().repeat(maskedCount)
        return "$COUNTRY_CODE $dots $last"
    }

    private companion object {
        const val COUNTRY_CODE = "+91"
        const val MASK_CHAR = '●'
        const val VISIBLE_DIGITS = 4
        const val MIN_MASK_DOTS = 2
    }
}
