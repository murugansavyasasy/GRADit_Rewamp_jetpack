package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The chip row on the family board: All, Unread, Pinned, then each category. */
sealed interface FamilyFilter {
    data object All : FamilyFilter
    data object Unread : FamilyFilter
    data object Pinned : FamilyFilter
    data class Category(val category: NoticeCategory) : FamilyFilter

    fun matches(notice: Notice, now: Long): Boolean = when (this) {
        All -> true
        Unread -> !notice.isReadByMe
        Pinned -> notice.isPinnedNow(now)
        is Category -> notice.category == category
    }
}

/**
 * The chips in display order. Built lazily outside [FamilyFilter]: a companion value
 * would be computed while `All` is still initialising and capture null.
 */
val familyFilterChips: List<FamilyFilter> by lazy {
    listOf(FamilyFilter.All, FamilyFilter.Unread, FamilyFilter.Pinned) + NoticeCategory.entries.map(FamilyFilter::Category)
}

enum class NoticeSection(@param:StringRes val label: Int) {
    TODAY(R.string.notice_section_today),
    YESTERDAY(R.string.notice_section_yesterday),
    EARLIER(R.string.notice_section_earlier),
}

data class FamilyNoticeBoardUiState(
    val load: NoticeLoad = NoticeLoad.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val query: String = "",
    val filter: FamilyFilter = FamilyFilter.All,
    val unreadCount: Int = 0,
    /** Live pinned notices, shown in their own box at the top. */
    val pinned: List<Notice> = emptyList(),
    /** Everything else, grouped by day; empty sections are left out. */
    val sections: List<Pair<NoticeSection, List<Notice>>> = emptyList(),
) {
    val isEmpty: Boolean get() = pinned.isEmpty() && sections.isEmpty()
}

/** Notices a parent / student received, with search, chips and day sections. */
class FamilyNoticeBoardViewModel(application: Application) : AndroidViewModel(application) {

    private val role = SessionPreferences.getRole(application)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<FamilyFilter>(FamilyFilter.All)
    private val load = MutableStateFlow(NoticeLoad.LOADING)

    val uiState: StateFlow<FamilyNoticeBoardUiState> = combine(
        combine(NoticeRepository.notices, NoticeRepository.lastSyncedAt, ::Pair),
        query,
        filter,
        load,
        NetworkMonitor.isOnline,
    ) { (notices, synced), query, filter, load, online -> build(notices, synced, query, filter, load, online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyNoticeBoardUiState())

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = NoticeLoad.LOADING
            load.value = try {
                NoticeRepository.refresh()
                NoticeLoad.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NoticeLoad.ERROR
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilter(value: FamilyFilter) {
        filter.value = value
    }

    private fun build(
        all: List<Notice>,
        synced: Long,
        query: String,
        filter: FamilyFilter,
        load: NoticeLoad,
        online: Boolean,
    ): FamilyNoticeBoardUiState {
        val now = System.currentTimeMillis()
        val mine = all.filter { it.isVisibleTo(role, now) }
        val needle = query.trim()
        val visible = mine.filter { notice ->
            filter.matches(notice, now) &&
                (needle.isEmpty() || notice.title.contains(needle, true) || notice.body.contains(needle, true) || notice.author.name.contains(needle, true))
        }
        // Urgent pinned notices first, then the newest.
        val pinned = visible.filter { it.isPinnedNow(now) }
            .sortedWith(compareByDescending<Notice> { it.priority == NoticePriority.URGENT }.thenByDescending { it.effectiveTime })
        val rest = visible.filterNot { it.isPinnedNow(now) }.sortedByDescending { it.effectiveTime }
        val sections = NoticeSection.entries.mapNotNull { section ->
            rest.filter { sectionOf(it.effectiveTime) == section }.takeIf { it.isNotEmpty() }?.let { section to it }
        }
        return FamilyNoticeBoardUiState(
            load = load,
            isOnline = online,
            lastSyncedAt = synced,
            query = query,
            filter = filter,
            unreadCount = mine.count { !it.isReadByMe },
            pinned = if (load == NoticeLoad.READY) pinned else emptyList(),
            sections = if (load == NoticeLoad.READY) sections else emptyList(),
        )
    }

    private fun sectionOf(millis: Long): NoticeSection = when {
        isDaysAgo(millis, 0) -> NoticeSection.TODAY
        isDaysAgo(millis, 1) -> NoticeSection.YESTERDAY
        else -> NoticeSection.EARLIER
    }
}
