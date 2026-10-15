package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NewMessageTab { NEW, HISTORY }

enum class HistoryFilter(@param:StringRes val label: Int) {
    ALL(R.string.msg_filter_all),
    TEXT(R.string.msg_filter_text),
    VOICE(R.string.msg_filter_voice),
    ;

    fun matches(message: SentMessage) = when (this) {
        ALL -> true
        TEXT -> message.type == MessageType.TEXT
        VOICE -> message.type == MessageType.VOICE
    }
}

/**
 * Immutable UI state for "New message".
 */
data class NewMessageUiState(
    val type: MessageType = MessageType.TEXT,
    val tab: NewMessageTab = NewMessageTab.NEW,
    val title: String = "",
    val description: String = "",
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val recording: VoiceClip? = null,
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val history: List<SentMessage> = emptyList(),
) {
    /** "Select recipients" needs a title plus the text (or a recording, for voice). */
    val canContinue: Boolean
        get() = title.isNotBlank() && when (type) {
            MessageType.TEXT -> description.isNotBlank()
            MessageType.VOICE -> recording != null && !isRecording
        }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 500
    }
}

/**
 * Step one of sending as management: write a new text / voice message or pick one
 * from history, then hand it to the composer to choose recipients. The initial
 * type comes from the [ARG_TYPE] nav argument ("Voice Msg" opens on Voice).
 */
class NewMessageViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val recorder = VoiceRecorder(application)
    private var tickerJob: Job? = null

    private val form = MutableStateFlow(
        NewMessageUiState(
            type = savedStateHandle.get<String>(ARG_TYPE)
                ?.let { name -> MessageType.entries.firstOrNull { it.name == name } }
                ?: MessageType.TEXT,
        ),
    )

    val uiState: StateFlow<NewMessageUiState> =
        combine(form, MessageRepository.history) { form, history ->
            form.copy(history = history.filter(form.historyFilter::matches))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = form.value.copy(history = MessageRepository.history.value),
        )

    fun onTypeSelected(value: MessageType) {
        if (value != MessageType.VOICE) cancelRecording()
        form.update { it.copy(type = value) }
    }

    fun onTabSelected(value: NewMessageTab) {
        form.update { it.copy(tab = value) }
    }

    fun onTitleChange(value: String) {
        form.update { it.copy(title = value) }
    }

    fun onDescriptionChange(value: String) {
        form.update { it.copy(description = value.take(NewMessageUiState.MAX_DESCRIPTION_LENGTH)) }
    }

    fun onHistoryFilterSelected(value: HistoryFilter) {
        form.update { it.copy(historyFilter = value) }
    }

    /** Starts recording; returns false if the microphone couldn't be opened. */
    fun onStartRecording(): Boolean {
        if (!recorder.start()) return false
        form.update { it.copy(isRecording = true, recordingElapsedMs = 0L, recording = null) }
        tickerJob = viewModelScope.launch {
            while (recorder.isRecording) {
                val elapsed = recorder.elapsedMs
                form.update { it.copy(recordingElapsedMs = elapsed) }
                if (elapsed >= VoiceRecorder.MAX_CLIP_MS) {
                    onStopRecording()
                    break
                }
                delay(TICK_MS)
            }
        }
        return true
    }

    fun onStopRecording() {
        tickerJob?.cancel()
        val clip = recorder.stop()
        form.update { it.copy(isRecording = false, recording = clip) }
    }

    fun onDiscardRecording() {
        form.update { it.copy(recording = null, recordingElapsedMs = 0L) }
    }

    /** Hands the new message to the composer. */
    fun onSelectRecipients() {
        val state = form.value
        if (!state.canContinue) return
        MessageRepository.setPendingDraft(
            ComposeDraft(
                subject = state.title.trim(),
                body = if (state.type == MessageType.TEXT) state.description.trim() else "",
                voice = if (state.type == MessageType.VOICE) state.recording else null,
            ),
        )
    }

    /** Hands a previously sent message to the composer to send again. */
    fun onSendFromHistory(message: SentMessage) {
        MessageRepository.setPendingDraft(ComposeDraft(subject = message.title, body = message.body, voice = message.voice))
    }

    private fun cancelRecording() {
        tickerJob?.cancel()
        recorder.cancel()
        form.update { it.copy(isRecording = false, recordingElapsedMs = 0L) }
    }

    override fun onCleared() {
        cancelRecording()
    }

    companion object {
        const val ARG_TYPE = "type"
        private const val TICK_MS = 200L
    }
}
