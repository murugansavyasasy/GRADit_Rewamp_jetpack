package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Holds one received message (by the [ARG_MESSAGE_ID] nav argument). Opening it marks
 * it read; "Got it" records that the user has seen it.
 */
class MessageDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val messageId: String = savedStateHandle.get<String>(ARG_MESSAGE_ID).orEmpty()

    /** Null when the id doesn't match any message. */
    val message: StateFlow<InboxMessage?> =
        MessageRepository.inbox
            .map { messages -> messages.firstOrNull { it.id == messageId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MessageRepository.inbox.value.firstOrNull { it.id == messageId },
            )

    init {
        MessageRepository.markRead(messageId)
    }

    fun onAcknowledge() {
        MessageRepository.acknowledge(messageId)
    }

    fun onMarkUnread() {
        MessageRepository.markUnread(messageId)
    }

    companion object {
        const val ARG_MESSAGE_ID = "messageId"
    }
}
