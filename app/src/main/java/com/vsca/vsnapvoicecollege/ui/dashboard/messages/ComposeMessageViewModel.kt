package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.components.EditResult
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.insertLink
import com.vsca.vsnapvoicecollege.ui.components.toggleBullets
import com.vsca.vsnapvoicecollege.ui.components.wrapSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Immutable UI state for the message composer.
 */
data class ComposeMessageUiState(
    val sendAs: SendAs = SendAs.MESSAGE,
    val recipients: List<RecipientGroup> = emptyList(),
    val recipientQuery: String = "",
    val subject: String = "",
    val message: TextFieldValue = TextFieldValue(),
    val attachment: FileAttachment? = null,
    val voice: VoiceClip? = null,
    val notifyBySms: Boolean = true,
    /** Epoch millis to send at; null sends immediately. */
    val scheduledAt: Long? = null,
    val templates: List<MessageTemplate> = emptyList(),
) {
    /** Groups matching what's typed in the "To" field that aren't already added. */
    val suggestions: List<RecipientGroup>
        get() {
            val query = recipientQuery.trim()
            if (query.isEmpty()) return emptyList()
            return MessageRepository.recipientGroups.filter { group ->
                group !in recipients && group.label.contains(query, ignoreCase = true)
            }
        }

    val recipientCount: Int get() = recipients.sumOf { it.memberCount }

    /** "parents" / "students" / "staff", or "recipients" for a mix. */
    val audience: Audience
        get() = recipients.map { it.audience }.distinct().singleOrNull() ?: Audience.MIXED

    val canSend: Boolean
        get() = recipients.isNotEmpty() && subject.isNotBlank() && (message.text.isNotBlank() || voice != null)

    val isBold: Boolean get() = message.isWrappedIn(BOLD_MARKER)
    val isItalic: Boolean get() = message.isWrappedIn(ITALIC_MARKER)

    companion object {
        const val MAX_MESSAGE_LENGTH = 1000
        const val BOLD_MARKER = "**"
        const val ITALIC_MARKER = "_"
    }
}

private fun TextFieldValue.isWrappedIn(marker: String): Boolean {
    val start = selection.min
    val end = selection.max
    return start >= marker.length && text.startsWith(marker, start - marker.length) && text.startsWith(marker, end)
}

/** What was sent, for the confirmation message. */
data class SendResult(val count: Int, val audience: Audience, val scheduledAt: Long?)

/**
 * Holds the composer form: send-as type, recipients, subject, formatted message,
 * attachment / voice clip and delivery options. Picks up any draft prepared on
 * "New message".
 */
class ComposeMessageViewModel(application: Application) : AndroidViewModel(application) {

    private val signature = MessageRepository.senderName(SessionPreferences.getRole(application))

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<ComposeMessageUiState> = _uiState.asStateFlow()

    private fun initialState(): ComposeMessageUiState {
        val draft = MessageRepository.consumePendingDraft()
        return ComposeMessageUiState(
            recipients = listOf(MessageRepository.defaultRecipientGroup),
            subject = draft?.subject.orEmpty(),
            message = draft?.body.orEmpty().let { TextFieldValue(it, TextRange(it.length)) },
            voice = draft?.voice,
            templates = MessageRepository.templates(signature),
        )
    }

    fun onSendAsSelected(value: SendAs) = _uiState.update { it.copy(sendAs = value) }

    fun onRecipientQueryChange(value: String) = _uiState.update { it.copy(recipientQuery = value) }

    fun onAddRecipient(group: RecipientGroup) = _uiState.update {
        if (group in it.recipients) it else it.copy(recipients = it.recipients + group, recipientQuery = "")
    }

    fun onRemoveRecipient(group: RecipientGroup) = _uiState.update { it.copy(recipients = it.recipients - group) }

    /** Backspace in an empty "To" field removes the last recipient. */
    fun onRemoveLastRecipient() = _uiState.update { it.copy(recipients = it.recipients.dropLast(1)) }

    fun onSubjectChange(value: String) = _uiState.update { it.copy(subject = value) }

    fun onMessageChange(value: TextFieldValue) = _uiState.update {
        it.copy(message = if (value.text.length <= ComposeMessageUiState.MAX_MESSAGE_LENGTH) value else it.message)
    }

    fun onToggleBold() = applyEdit { text, start, end -> wrapSelection(text, start, end, ComposeMessageUiState.BOLD_MARKER) }

    fun onToggleItalic() = applyEdit { text, start, end -> wrapSelection(text, start, end, ComposeMessageUiState.ITALIC_MARKER) }

    fun onToggleBullets() = applyEdit(::toggleBullets)

    fun onInsertLink(url: String) = applyEdit { text, start, end -> insertLink(text, start, end, url.trim()) }

    /** Inserts dictated speech at the cursor. */
    fun onDictated(spoken: String) = applyEdit { text, start, end ->
        val before = text.substring(0, start)
        val insert = if (before.isEmpty() || before.endsWith(' ') || before.endsWith('\n')) spoken else " $spoken"
        val result = before + insert + text.substring(end)
        EditResult(result, start + insert.length, start + insert.length)
    }

    fun onTemplateSelected(template: MessageTemplate) = _uiState.update {
        it.copy(subject = template.subject, message = TextFieldValue(template.body, TextRange(template.body.length)))
    }

    fun onAttachmentPicked(attachment: FileAttachment) = _uiState.update { it.copy(attachment = attachment) }

    fun onRemoveAttachment() = _uiState.update { it.copy(attachment = null) }

    fun onRemoveVoice() = _uiState.update { it.copy(voice = null) }

    fun onNotifyBySmsChange(value: Boolean) = _uiState.update { it.copy(notifyBySms = value) }

    fun onScheduleChange(value: Long?) = _uiState.update { it.copy(scheduledAt = value) }

    /** Sends (or schedules) the message; returns null when the form isn't ready. */
    fun onSend(): SendResult? {
        val state = _uiState.value
        if (!state.canSend) return null
        // TODO: include recipients, send-as type, attachment, SMS flag and schedule in the API call.
        MessageRepository.send(subject = state.subject.trim(), body = state.message.text.trim(), voice = state.voice)
        return SendResult(state.recipientCount, state.audience, state.scheduledAt)
    }

    private fun applyEdit(edit: (String, Int, Int) -> EditResult) = _uiState.update { state ->
        val value = state.message
        val result = edit(value.text, value.selection.min, value.selection.max)
        if (result.text.length > ComposeMessageUiState.MAX_MESSAGE_LENGTH) {
            state
        } else {
            state.copy(message = TextFieldValue(result.text, TextRange(result.selectionStart, result.selectionEnd)))
        }
    }
}
