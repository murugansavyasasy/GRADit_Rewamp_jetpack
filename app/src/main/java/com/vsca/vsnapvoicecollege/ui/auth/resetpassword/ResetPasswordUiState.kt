package com.vsca.vsnapvoicecollege.ui.auth.resetpassword

/** Password strength buckets, each mapping to a number of filled meter segments. */
enum class PasswordStrength(val segments: Int) {
    NONE(0),
    WEAK(1),
    FAIR(2),
    STRONG(3),
    VERY_STRONG(4),
}

/**
 * Immutable UI state for the Create / Reset Password screen.
 *
 * Validation rules and strength are derived from the entered values.
 */
data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPasswordReset: Boolean = false,
) {
    val hasMinLength: Boolean get() = newPassword.length >= MIN_LENGTH
    val hasUpperAndLower: Boolean
        get() = newPassword.any { it.isUpperCase() } && newPassword.any { it.isLowerCase() }
    val hasNumberOrSymbol: Boolean
        get() = newPassword.any { it.isDigit() } || newPassword.any { !it.isLetterOrDigit() }
    val passwordsMatch: Boolean
        get() = newPassword.isNotEmpty() && newPassword == confirmPassword

    private val satisfiedRuleCount: Int
        get() = listOf(hasMinLength, hasUpperAndLower, hasNumberOrSymbol).count { it }

    val strength: PasswordStrength
        get() = when {
            newPassword.isEmpty() -> PasswordStrength.NONE
            satisfiedRuleCount <= 1 -> PasswordStrength.WEAK
            satisfiedRuleCount == 2 -> PasswordStrength.FAIR
            satisfiedRuleCount == 3 && newPassword.length < STRONG_LENGTH -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }

    val canSave: Boolean
        get() = hasMinLength && hasUpperAndLower && hasNumberOrSymbol && passwordsMatch

    companion object {
        const val MIN_LENGTH = 8
        private const val STRONG_LENGTH = 12
        const val STRENGTH_SEGMENTS = 4
    }
}
