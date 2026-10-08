package com.vsca.vsnapvoicecollege.ui.dashboard.help

/**
 * Immutable UI state for the raise-a-concern form.
 */
data class RaiseConcernUiState(
    val selectedCategory: ConcernCategory = ConcernCategory.ATTENDANCE,
    val subject: String = "",
    val description: String = "",
    val isSubmitted: Boolean = false,
) {
    /** Enables the submit button only when the required fields are filled. */
    val canSubmit: Boolean
        get() = subject.isNotBlank() && description.isNotBlank()

    companion object {
        /** Maximum characters allowed in the description. */
        const val MAX_DESCRIPTION_LENGTH = 500
    }
}
