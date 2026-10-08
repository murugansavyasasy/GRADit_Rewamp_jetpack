package com.vsca.vsnapvoicecollege.ui.auth.roleselection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vsca.vsnapvoicecollege.ui.auth.navigation.AuthRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the state for the role selection screen.
 *
 * The signed-in number is taken from the mobile the user typed at login (passed
 * as a nav argument); roles and the college list are stubbed until the auth data
 * layer lands.
 */
class RoleSelectionViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleSelectionUiState())
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    init {
        val roles = Role.entries.toList()
        val mobile = savedStateHandle.get<String>(AuthRoute.ARG_MOBILE).orEmpty()
        _uiState.update {
            it.copy(
                maskedMobile = maskMobile(mobile),
                availableRoles = roles,
                selectedRole = roles.firstOrNull(),
                colleges = SUPPORTED_COLLEGES,
                selectedCollege = SUPPORTED_COLLEGES.firstOrNull(),
            )
        }
    }

    /** Masks the typed [mobile], keeping the country code and the last four digits. */
    private fun maskMobile(mobile: String): String {
        val digits = mobile.filter(Char::isDigit)
        if (digits.length < 4) return DEFAULT_MASKED
        val last4 = digits.takeLast(4)
        val bullets = "•".repeat(digits.length - 4)
        return "+91 $bullets$last4"
    }

    fun onRoleSelected(role: Role) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun onCollegeSelected(college: String) {
        _uiState.update { it.copy(selectedCollege = college) }
    }

    private companion object {
        /** Fallback shown when no mobile was passed in (e.g. previews / deep links). */
        const val DEFAULT_MASKED = "+91 ••••• ••••"

        // TODO: replace with the colleges linked to the signed-in account.
        val SUPPORTED_COLLEGES = listOf(
            "University College of Engineering, Chennai",
            "PSG College of Technology, Coimbatore",
            "Thiagarajar College of Engineering, Madurai",
        )
    }
}
