package com.vsca.vsnapvoicecollege.ui.dashboard.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The notification toggles on the Settings screen. */
enum class SettingToggle { ATTENDANCE, FEE, RESULTS, BUS, BIOMETRIC }

/**
 * Holds the Settings screen state. Toggles are in-memory for now; persist them
 * (DataStore / repository) during API integration.
 */
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onToggle(toggle: SettingToggle, enabled: Boolean) {
        _uiState.update {
            when (toggle) {
                SettingToggle.ATTENDANCE -> it.copy(attendanceAlerts = enabled)
                SettingToggle.FEE -> it.copy(feeReminders = enabled)
                SettingToggle.RESULTS -> it.copy(resultsExams = enabled)
                SettingToggle.BUS -> it.copy(busArrival = enabled)
                SettingToggle.BIOMETRIC -> it.copy(biometricUnlock = enabled)
            }
        }
    }
}
