package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Immutable UI state for the new-post form.
 */
data class NewPostUiState(
    val subjects: List<String> = DiscussionRepository.subjects,
    val subject: String = DiscussionRepository.subjects.first(),
    val title: String = "",
    val description: String = "",
    val attachment: FileAttachment? = null,
    val notifyTeacher: Boolean = true,
    val classTeacher: String = DiscussionRepository.CLASS_TEACHER,
) {
    /** "Post" is enabled once the required title is filled. */
    val canPost: Boolean
        get() = title.isNotBlank()

    companion object {
        const val MAX_TITLE_LENGTH = 80
        const val MAX_DESCRIPTION_LENGTH = 500
    }
}

/**
 * Holds the new-post form and publishes it as the signed-in user's role.
 */
class NewPostViewModel(application: Application) : AndroidViewModel(application) {

    private val currentUser = DiscussionRepository.currentUser(SessionPreferences.getRole(application))

    private val _uiState = MutableStateFlow(NewPostUiState())
    val uiState: StateFlow<NewPostUiState> = _uiState.asStateFlow()

    fun onSubjectSelected(value: String) {
        _uiState.update { it.copy(subject = value) }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value.take(NewPostUiState.MAX_TITLE_LENGTH)) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value.take(NewPostUiState.MAX_DESCRIPTION_LENGTH)) }
    }

    fun onAttachmentPicked(attachment: FileAttachment) {
        _uiState.update { it.copy(attachment = attachment) }
    }

    fun onRemoveAttachment() {
        _uiState.update { it.copy(attachment = null) }
    }

    fun onNotifyTeacherChange(value: Boolean) {
        _uiState.update { it.copy(notifyTeacher = value) }
    }

    /** Publishes the post; returns false (and does nothing) when the form isn't ready. */
    fun onPost(): Boolean {
        val state = _uiState.value
        if (!state.canPost) return false
        // TODO: upload the attachment, publish via the discussion API and honour notifyTeacher.
        DiscussionRepository.addPost(
            author = currentUser,
            subject = state.subject,
            title = state.title.trim(),
            body = state.description.trim(),
            attachment = state.attachment,
        )
        return true
    }
}
