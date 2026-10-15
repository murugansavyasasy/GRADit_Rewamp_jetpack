package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Immutable UI state for a discussion thread.
 */
data class DiscussionThreadUiState(
    /** Null when the post id doesn't match any post. */
    val post: DiscussionPost? = null,
    val draft: String = "",
    val draftAttachment: FileAttachment? = null,
) {
    val canSend: Boolean
        get() = post != null && (draft.isNotBlank() || draftAttachment != null)
}

/**
 * Holds one post (looked up by the [ARG_POST_ID] nav argument) and the reply being
 * composed. Replies are posted as the signed-in user's role.
 */
class DiscussionThreadViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val postId: String = savedStateHandle.get<String>(ARG_POST_ID).orEmpty()
    private val currentUser = DiscussionRepository.currentUser(SessionPreferences.getRole(application))

    private val draft = MutableStateFlow(Draft())

    val uiState: StateFlow<DiscussionThreadUiState> =
        combine(DiscussionRepository.posts, draft) { posts, draft -> buildState(posts, draft) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = buildState(DiscussionRepository.posts.value, draft.value),
            )

    fun onDraftChange(value: String) {
        draft.update { it.copy(text = value) }
    }

    fun onAttachmentPicked(attachment: FileAttachment) {
        draft.update { it.copy(attachment = attachment) }
    }

    fun onRemoveAttachment() {
        draft.update { it.copy(attachment = null) }
    }

    fun onSend() {
        val current = draft.value
        if (current.text.isBlank() && current.attachment == null) return
        // TODO: upload the attachment and post the reply to the discussion API.
        DiscussionRepository.addReply(postId, currentUser, current.text.trim(), current.attachment)
        draft.value = Draft()
    }

    fun onToggleReplyLike(replyId: String) {
        DiscussionRepository.toggleReplyLike(postId, replyId)
    }

    private fun buildState(posts: List<DiscussionPost>, draft: Draft) = DiscussionThreadUiState(
        post = posts.firstOrNull { it.id == postId },
        draft = draft.text,
        draftAttachment = draft.attachment,
    )

    private data class Draft(val text: String = "", val attachment: FileAttachment? = null)

    companion object {
        const val ARG_POST_ID = "postId"
    }
}
