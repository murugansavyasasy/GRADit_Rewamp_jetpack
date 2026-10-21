package com.vsca.vsnapvoicecollege.ui.dashboard.placement.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementChild
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementLoad
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementMode
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementSection
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementTab
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.daysFromToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlacementTrainingUiState(
    val load: PlacementLoad = PlacementLoad.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val tab: PlacementTab = PlacementTab.UPCOMING,
    val query: String = "",
    /** Null = all modes / all batches. */
    val mode: PlacementMode? = null,
    val batch: Int? = null,
    val modes: List<PlacementMode> = emptyList(),
    val batches: List<Int> = emptyList(),
    val upcomingCount: Int = 0,
    val historicalCount: Int = 0,
    /** Trainings with a session in the next [REMINDER_DAYS] days, soonest first. */
    val soon: List<PlacementTraining> = emptyList(),
    val sections: List<Pair<PlacementSection, List<PlacementTraining>>> = emptyList(),
    val family: List<PlacementChild> = emptyList(),
) {
    val isEmpty: Boolean get() = sections.isEmpty()
    val isFiltered: Boolean get() = query.isNotBlank() || mode != null || batch != null

    companion object {
        const val REMINDER_DAYS = 2
        const val THIS_WEEK_DAYS = 7
    }
}

/** Placement training for parents: Upcoming / Historical tabs, search, mode and batch filters. */
class PlacementTrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val family = PlacementRepository.familyFor(SessionPreferences.getRole(application))
    private val tab = MutableStateFlow(PlacementTab.UPCOMING)
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow<Pair<PlacementMode?, Int?>>(null to null)
    private val load = MutableStateFlow(PlacementLoad.LOADING)

    val uiState: StateFlow<PlacementTrainingUiState> = combine(
        combine(PlacementTrainingRepository.trainings, PlacementTrainingRepository.lastSyncedAt, ::Pair),
        combine(tab, query, ::Pair),
        filters,
        load,
        NetworkMonitor.isOnline,
    ) { (trainings, synced), (tab, query), (mode, batch), load, online ->
        build(trainings, synced, tab, query, mode, batch, load, online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlacementTrainingUiState(family = family))

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = PlacementLoad.LOADING
            load.value = try {
                PlacementTrainingRepository.refresh()
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

    fun onBatch(value: Int?) {
        filters.value = filters.value.first to value
    }

    private fun build(
        all: List<PlacementTraining>,
        synced: Long,
        tab: PlacementTab,
        query: String,
        mode: PlacementMode?,
        batch: Int?,
        load: PlacementLoad,
        online: Boolean,
    ): PlacementTrainingUiState {
        val now = System.currentTimeMillis()
        val needle = query.trim()
        val matching = all.filter { training ->
            training.matches(needle) && (mode == null || training.mode == mode) && (batch == null || training.isFor(batch))
        }
        val (ended, upcoming) = matching.partition { it.hasEnded(now) }
        val sections = when (tab) {
            PlacementTab.UPCOMING -> upcoming.sortedBy { it.nextSession(now) }.groupBy {
                val days = daysFromToday(it.nextSession(now) ?: it.firstStart, now)
                if (days < PlacementTrainingUiState.THIS_WEEK_DAYS) PlacementSection.THIS_WEEK else PlacementSection.LATER
            }
            PlacementTab.HISTORICAL -> mapOf(PlacementSection.ENDED to ended.sortedByDescending { it.lastStart })
        }.let { grouped -> PlacementSection.entries.mapNotNull { section -> grouped[section]?.takeIf { it.isNotEmpty() }?.let { section to it } } }
        // The reminder ignores search and filters: it's about what's coming up, not what's listed.
        val soon = all.mapNotNull { training -> training.nextSession(now)?.let { training to it } }
            .filter { (_, next) -> daysFromToday(next, now) <= PlacementTrainingUiState.REMINDER_DAYS }
            .sortedBy { (_, next) -> next }
            .map { (training, _) -> training }
        val ready = load == PlacementLoad.READY
        return PlacementTrainingUiState(
            load = load,
            isOnline = online,
            lastSyncedAt = synced,
            tab = tab,
            query = query,
            mode = mode,
            batch = batch,
            modes = PlacementMode.entries.filter { m -> all.any { it.mode == m } },
            batches = (family.map { it.batch } + all.flatMap { it.batches }).filter { it > 0 }.distinct().sorted(),
            upcomingCount = upcoming.size,
            historicalCount = ended.size,
            soon = if (ready) soon else emptyList(),
            sections = if (ready) sections else emptyList(),
            family = family,
        )
    }
}

data class StudentTrainingUiState(
    val load: PlacementLoad = PlacementLoad.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val query: String = "",
    val trainings: List<PlacementTraining> = emptyList(),
)

/** The student's own trainings (planned for their batch) that are still running, with search. */
class StudentTrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val batches = PlacementRepository.familyFor(Role.STUDENT).map { it.batch }
    private val query = MutableStateFlow("")
    private val load = MutableStateFlow(PlacementLoad.LOADING)

    val uiState: StateFlow<StudentTrainingUiState> = combine(
        combine(PlacementTrainingRepository.trainings, PlacementTrainingRepository.lastSyncedAt, ::Pair),
        query,
        load,
        NetworkMonitor.isOnline,
    ) { (trainings, synced), query, load, online ->
        val now = System.currentTimeMillis()
        val needle = query.trim()
        StudentTrainingUiState(
            load = load,
            isOnline = online,
            lastSyncedAt = synced,
            query = query,
            trainings = if (load == PlacementLoad.READY) {
                trainings.filter { t -> !t.hasEnded(now) && batches.any(t::isFor) && t.matches(needle) }
                    .sortedBy { it.nextSession(now) }
            } else {
                emptyList()
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentTrainingUiState())

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = PlacementLoad.LOADING
            load.value = try {
                PlacementTrainingRepository.refresh()
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
