package com.vsca.vsnapvoicecollege.ui.dashboard.changepassword

import com.vsca.vsnapvoicecollege.ui.components.PasswordStrength
import com.vsca.vsnapvoicecollege.ui.components.passwordStrengthByLength

/**
 * Immutable UI state for the Change Password screen.
 */
data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val signOutOtherDevices: Boolean = true,
    val isUpdated: Boolean = false,
) {
    val strength: PasswordStrength get() = passwordStrengthByLength(newPassword)

    val passwordsMatch: Boolean
        get() = confirmPassword.isNotEmpty() && newPassword == confirmPassword

    val showMismatch: Boolean
        get() = confirmPassword.isNotEmpty() && newPassword != confirmPassword

    val canUpdate: Boolean
        get() = currentPassword.isNotEmpty() && newPassword.isNotEmpty() && passwordsMatch
}
