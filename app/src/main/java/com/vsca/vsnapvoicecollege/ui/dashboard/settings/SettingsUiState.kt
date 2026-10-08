package com.vsca.vsnapvoicecollege.ui.dashboard.settings

/**
 * Immutable UI state for the Settings screen's notification toggles.
 */
data class SettingsUiState(
    val attendanceAlerts: Boolean = true,
    val feeReminders: Boolean = true,
    val resultsExams: Boolean = true,
    val busArrival: Boolean = false,
    val biometricUnlock: Boolean = true,
)
