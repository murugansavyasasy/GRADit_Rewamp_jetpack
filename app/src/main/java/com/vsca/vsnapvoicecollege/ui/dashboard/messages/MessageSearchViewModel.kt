package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.MessagePreferences
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Immutable UI state for message search.
 */
data class MessageSearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    /** Matches for a non-blank [query], newest first. */
    val results: List<InboxMessage> = emptyList(),
)

/**
 * Searches the user's received messages by title, text and sender, and keeps the
 * recent-searches list in [MessagePreferences].
 */
class MessageSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    /** Parents search across all their children; students only their own messages. */
    private val childIds: Set<String> =
        if (SessionPreferences.getRole(application) == Role.PARENT) {
            MessageRepository.children.map { it.id }.toSet()
        } else {
            setOf(MessageRepository.studentSelf.id)
        }

    private val query = MutableStateFlow("")
    private val recents = MutableStateFlow(MessagePreferences.recentSearches(application))

    val uiState: StateFlow<MessageSearchUiState> =
        combine(MessageRepository.inbox, query, recents, ::buildState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = buildState(MessageRepository.inbox.value, query.value, recents.value),
            )

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Remembers the current query (keyboard search action, or opening a result). */
    fun onSearchSubmitted() {
        recents.value = MessagePreferences.addRecentSearch(app, query.value)
    }

    fun onRecentSelected(value: String) {
        query.value = value
        onSearchSubmitted()
    }

    fun onRemoveRecent(value: String) {
        recents.value = MessagePreferences.removeRecentSearch(app, value)
    }

    fun onClearRecents() {
        MessagePreferences.clearRecentSearches(app)
        recents.value = emptyList()
    }

    private fun buildState(messages: List<InboxMessage>, query: String, recents: List<String>): MessageSearchUiState {
        val needle = query.trim()
        val results = if (needle.isEmpty()) {
            emptyList()
        } else {
            messages
                .filter { it.childId in childIds }
                .filter {
                    it.title.contains(needle, ignoreCase = true) ||
                        it.body.contains(needle, ignoreCase = true) ||
                        it.sender.name.contains(needle, ignoreCase = true)
                }
                // The same broadcast can reach several children; show it once.
                .distinctBy { Triple(it.sender, it.title, it.sentAt) }
                .sortedByDescending { it.sentAt }
        }
        return MessageSearchUiState(query = query, recentSearches = recents, results = results)
    }
}
