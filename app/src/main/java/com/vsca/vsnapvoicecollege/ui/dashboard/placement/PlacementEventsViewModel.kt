package com.vsca.vsnapvoicecollege.ui.dashboard.placement

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlacementLoad { LOADING, READY, ERROR }

enum class PlacementTab(@param:StringRes val label: Int) {
    UPCOMING(R.string.placement_tab_upcoming),
    HISTORICAL(R.string.placement_tab_historical),
}

enum class PlacementSection(@param:StringRes val label: Int) {
    THIS_WEEK(R.string.placement_section_this_week),
    LATER(R.string.placement_section_later),
    ENDED(R.string.placement_section_ended),
}

data class PlacementEventsUiState(
    val load: PlacementLoad = PlacementLoad.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val tab: PlacementTab = PlacementTab.UPCOMING,
    val query: String = "",
    /** Null = all modes / all courses. */
    val mode: PlacementMode? = null,
    val course: String? = null,
    val courses: List<String> = emptyList(),
    val upcomingCount: Int = 0,
    val historicalCount: Int = 0,
    /** Upcoming events starting within the next [REMINDER_DAYS] days, soonest first. */
    val soon: List<PlacementEvent> = emptyList(),
    val sections: List<Pair<PlacementSection, List<PlacementEvent>>> = emptyList(),
    val family: List<PlacementChild> = emptyList(),
) {
    val isEmpty: Boolean get() = sections.isEmpty()
    val isFiltered: Boolean get() = query.isNotBlank() || mode != null || course != null

    companion object {
        const val REMINDER_DAYS = 2
        const val THIS_WEEK_DAYS = 7
    }
}

/** Placement events for parents: Upcoming / Historical tabs, search, mode and course filters. */
class PlacementEventsViewModel(application: Application) : AndroidViewModel(application) {

    private val family = PlacementRepository.familyFor(SessionPreferences.getRole(application))
    private val tab = MutableStateFlow(PlacementTab.UPCOMING)
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow<Pair<PlacementMode?, String?>>(null to null)
    private val load = MutableStateFlow(PlacementLoad.LOADING)

    val uiState: StateFlow<PlacementEventsUiState> = combine(
        combine(PlacementRepository.events, PlacementRepository.lastSyncedAt, ::Pair),
        combine(tab, query, ::Pair),
        filters,
        load,
        NetworkMonitor.isOnline,
    ) { (events, synced), (tab, query), (mode, course), load, online ->
        build(events, synced, tab, query, mode, course, load, online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlacementEventsUiState(family = family))

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = PlacementLoad.LOADING
            load.value = try {
                PlacementRepository.refresh()
                PlacementLoad.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PlacementLoad.ERROR
            }
        }
    }

    fun onTab(value: PlacementTab) {
        tab.value = value
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onMode(value: PlacementMode?) {
        filters.value = value to filters.value.second
    }

    fun onCourse(value: String?) {
        filters.value = filters.value.first to value
    }

    private fun build(
        all: List<PlacementEvent>,
        synced: Long,
        tab: PlacementTab,
        query: String,
        mode: PlacementMode?,
        course: String?,
        load: PlacementLoad,
        online: Boolean,
    ): PlacementEventsUiState {
        val now = System.currentTimeMillis()
        val needle = query.trim()
        val matching = all.filter { event ->
            event.matches(needle) && (mode == null || event.mode == mode) && (course == null || event.isEligible(course))
        }
        val (ended, upcoming) = matching.partition { it.hasEnded(now) }
        val sections = when (tab) {
            PlacementTab.UPCOMING -> upcoming.sortedBy { it.startAt }
                .groupBy { if (daysFromToday(it.startAt, now) < PlacementEventsUiState.THIS_WEEK_DAYS) PlacementSection.THIS_WEEK else PlacementSection.LATER }
            PlacementTab.HISTORICAL -> mapOf(PlacementSection.ENDED to ended.sortedByDescending { it.startAt })
        }.let { grouped -> PlacementSection.entries.mapNotNull { section -> grouped[section]?.takeIf { it.isNotEmpty() }?.let { section to it } } }
        // The reminder ignores search and filters: it's about what's coming up, not what's listed.
        val soon = all.filter { !it.hasEnded(now) && daysFromToday(it.startAt, now) <= PlacementEventsUiState.REMINDER_DAYS }
            .sortedBy { it.startAt }
        val ready = load == PlacementLoad.READY
        return PlacementEventsUiState(
            load = load,
            isOnline = online,
            lastSyncedAt = synced,
            tab = tab,
            query = query,
            mode = mode,
            course = course,
            courses = (family.map { it.course } + all.flatMap { it.eligibleCourses }).distinct(),
            upcomingCount = upcoming.size,
            historicalCount = ended.size,
            soon = if (ready) soon else emptyList(),
            sections = if (ready) sections else emptyList(),
            family = family,
        )
    }
}

data class StudentPlacementUiState(
    val load: PlacementLoad = PlacementLoad.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val query: String = "",
    val events: List<PlacementEvent> = emptyList(),
)

/** The student's simple list of upcoming placement events, with search. */
class StudentPlacementViewModel(application: Application) : AndroidViewModel(application) {

    private val query = MutableStateFlow("")
    private val load = MutableStateFlow(PlacementLoad.LOADING)

    val uiState: StateFlow<StudentPlacementUiState> = combine(
        combine(PlacementRepository.events, PlacementRepository.lastSyncedAt, ::Pair),
        query,
        load,
        NetworkMonitor.isOnline,
    ) { (events, synced), query, load, online ->
        val now = System.currentTimeMillis()
        val needle = query.trim()
        StudentPlacementUiState(
            load = load,
            isOnline = online,
            lastSyncedAt = synced,
            query = query,
            events = if (load == PlacementLoad.READY) {
                events.filter { !it.hasEnded(now) && it.matches(needle) }.sortedBy { it.startAt }
            } else {
                emptyList()
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentPlacementUiState())

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = PlacementLoad.LOADING
            load.value = try {
                PlacementRepository.refresh()
                PlacementLoad.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PlacementLoad.ERROR
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }
}
