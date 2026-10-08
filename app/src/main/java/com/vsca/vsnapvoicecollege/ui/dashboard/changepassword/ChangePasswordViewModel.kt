package com.vsca.vsnapvoicecollege.ui.dashboard.changepassword

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds and exposes the state for [ChangePasswordScreen] and handles user intents.
 */
class ChangePasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    fun onToggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    fun onSignOutOtherDevicesChange(checked: Boolean) {
        _uiState.update { it.copy(signOutOtherDevices = checked) }
    }

    fun onUpdateClick() {
        if (!_uiState.value.canUpdate) return
        // TODO: submit the password change to the auth repository.
        _uiState.update { it.copy(isUpdated = true) }
    }
}
