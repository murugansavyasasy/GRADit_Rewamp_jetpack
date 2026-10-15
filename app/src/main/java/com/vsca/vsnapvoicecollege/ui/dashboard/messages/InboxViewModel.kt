package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LoadState { LOADING, ERROR, READY }

/**
 * Immutable UI state for the received-messages list.
 */
data class InboxUiState(
    val loadState: LoadState = LoadState.LOADING,
    /** Linked children — only parents get the child switcher. */
    val children: List<LinkedChild> = emptyList(),
    val selectedChild: LinkedChild? = null,
    val filter: InboxFilter = InboxFilter.ALL,
    val counts: Map<InboxFilter, Int> = emptyMap(),
    /** Messages matching the filter, grouped by day; empty sections are omitted. */
    val sections: List<Pair<InboxSection, List<InboxMessage>>> = emptyList(),
) {
    val showChildSelector: Boolean get() = children.size > 1
}

/**
 * Holds the inbox filter and (for parents) the selected child, and loads their
 * messages from [MessageRepository]. The initial filter comes from the
 * [ARG_FILTER] nav argument ("Text Msg" opens on Text, "Voice Msg" on Voice).
 */
class InboxViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val role = SessionPreferences.getRole(application)
    private val children = if (role == Role.PARENT) MessageRepository.children else emptyList()

    private val childId = MutableStateFlow(
        if (role == Role.PARENT) children.first().id else MessageRepository.studentSelf.id,
    )
    private val filter = MutableStateFlow(
        savedStateHandle.get<String>(ARG_FILTER)
            ?.let { name -> InboxFilter.entries.firstOrNull { it.name == name } }
            ?: InboxFilter.ALL,
    )
    private val loadState = MutableStateFlow(LoadState.LOADING)
    private var loadJob: Job? = null

    val uiState: StateFlow<InboxUiState> =
        combine(MessageRepository.inbox, childId, filter, loadState, ::buildState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = buildState(MessageRepository.inbox.value, childId.value, filter.value, loadState.value),
            )

    init {
        load()
    }

    /** (Re)loads the selected child's messages; also the error state's "Retry". */
    fun load() {
        loadJob?.cancel()
        loadState.value = LoadState.LOADING
        loadJob = viewModelScope.launch {
            loadState.value = try {
                MessageRepository.refreshInbox()
                LoadState.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.ERROR
            }
        }
    }

    fun onFilterSelected(value: InboxFilter) {
        filter.value = value
    }

    fun onChildSelected(child: LinkedChild) {
        if (child.id == childId.value) return
        childId.value = child.id
        load()
    }

    fun onMarkAllRead() {
        MessageRepository.markAllRead(childId.value)
    }

    private fun buildState(
        messages: List<InboxMessage>,
        childId: String,
        filter: InboxFilter,
        loadState: LoadState,
    ): InboxUiState {
        val mine = messages.filter { it.childId == childId }.sortedByDescending { it.sentAt }
        val visible = mine.filter(filter::matches)
        return InboxUiState(
            loadState = loadState,
            children = children,
            selectedChild = children.firstOrNull { it.id == childId },
            filter = filter,
            counts = InboxFilter.entries.associateWith { f -> mine.count(f::matches) },
            sections = InboxSection.entries.mapNotNull { section ->
                visible.filter { sectionFor(it.sentAt) == section }
                    .takeIf { it.isNotEmpty() }
                    ?.let { section to it }
            },
        )
    }

    companion object {
        const val ARG_FILTER = "filter"
    }
}
