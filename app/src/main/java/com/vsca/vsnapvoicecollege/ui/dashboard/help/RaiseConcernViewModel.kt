package com.vsca.vsnapvoicecollege.ui.dashboard.help

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vsca.vsnapvoicecollege.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the state for the raise-a-concern form.
 *
 * Subject and description are seeded with sample content to mirror the design;
 * submission is stubbed until the support backend is wired in.
 */
class RaiseConcernViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        RaiseConcernUiState(
            subject = application.getString(R.string.concern_subject_prefill),
            description = application.getString(R.string.concern_describe_prefill),
        ),
    )
    val uiState: StateFlow<RaiseConcernUiState> = _uiState.asStateFlow()

    fun onCategorySelected(category: ConcernCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onSubjectChange(value: String) {
        _uiState.update { it.copy(subject = value) }
    }

    fun onDescriptionChange(value: String) {
        val trimmed = value.take(RaiseConcernUiState.MAX_DESCRIPTION_LENGTH)
        _uiState.update { it.copy(description = trimmed) }
    }

    fun onSubmit() {
        // TODO: send the concern to the support backend.
        _uiState.update { it.copy(isSubmitted = true) }
    }
}
