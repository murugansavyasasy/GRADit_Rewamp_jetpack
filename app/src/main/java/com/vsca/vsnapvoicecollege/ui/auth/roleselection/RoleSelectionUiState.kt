package com.vsca.vsnapvoicecollege.ui.auth.roleselection

/**
 * Immutable UI state for the role selection screen.
 *
 * [maskedMobile] is the partially hidden number shown in the "Signed in as …"
 * label; [availableRoles] are the roles this account may act as.
 */
data class RoleSelectionUiState(
    val maskedMobile: String = "",
    val availableRoles: List<Role> = emptyList(),
    val selectedRole: Role? = null,
    val colleges: List<String> = emptyList(),
    val selectedCollege: String? = null,
)
