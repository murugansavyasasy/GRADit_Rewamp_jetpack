package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Immutable UI state for the discussion board list.
 */
data class DiscussionBoardUiState(
    val query: String = "",
    val filter: DiscussionFilter = DiscussionFilter.ALL,
    /** Posts matching the current search and filter. */
    val posts: List<DiscussionPost> = emptyList(),
    /** False when the board has no posts at all (shows the "No discussions yet" state). */
    val hasAnyPosts: Boolean = false,
)

/**
 * Holds the search query and filter for the discussion board and applies them to
 * the posts from [DiscussionRepository].
 */
class DiscussionBoardViewModel : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(DiscussionFilter.ALL)

    val uiState: StateFlow<DiscussionBoardUiState> =
        combine(DiscussionRepository.posts, query, filter, ::buildState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = buildState(DiscussionRepository.posts.value, query.value, filter.value),
            )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterSelected(value: DiscussionFilter) {
        filter.value = value
    }

    fun onToggleLike(postId: String) {
        DiscussionRepository.togglePostLike(postId)
    }

    private fun buildState(
        posts: List<DiscussionPost>,
        query: String,
        filter: DiscussionFilter,
    ): DiscussionBoardUiState {
        val needle = query.trim()
        val visible = posts.filter { post ->
            filter.matches(post) && (
                needle.isEmpty() ||
                    post.title.contains(needle, ignoreCase = true) ||
                    post.body.contains(needle, ignoreCase = true) ||
                    post.subject.contains(needle, ignoreCase = true) ||
                    post.author.name.contains(needle, ignoreCase = true)
                )
        }
        return DiscussionBoardUiState(
            query = query,
            filter = filter,
            posts = visible,
            hasAnyPosts = posts.isNotEmpty(),
        )
    }
}
