package com.vsca.vsnapvoicecollege.ui.auth.roleselection

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the state for the role selection screen.
 *
 * The signed-in number, available roles and college list are stubbed here; move
 * them to a data layer (auth repository) when that work lands.
 */
class RoleSelectionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoleSelectionUiState())
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    init {
        // TODO: receive the real masked number, roles and colleges from the auth repository.
        val roles = Role.entries.toList()
        _uiState.update {
            it.copy(
                maskedMobile = "+91 ••••• •3210",
                availableRoles = roles,
                selectedRole = roles.firstOrNull(),
                colleges = SUPPORTED_COLLEGES,
                selectedCollege = SUPPORTED_COLLEGES.firstOrNull(),
            )
        }
    }

    fun onRoleSelected(role: Role) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun onCollegeSelected(college: String) {
        _uiState.update { it.copy(selectedCollege = college) }
    }

    private companion object {
        // TODO: replace with the colleges linked to the signed-in account.
        val SUPPORTED_COLLEGES = listOf(
            "University College of Engineering, Chennai",
            "PSG College of Technology, Coimbatore",
            "Thiagarajar College of Engineering, Madurai",
        )
    }
}
