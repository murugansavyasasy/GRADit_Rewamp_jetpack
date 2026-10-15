package com.vsca.vsnapvoicecollege.ui.dashboard.credits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreditLoad { LOADING, ERROR, READY }

data class SemCreditsUiState(
    val load: CreditLoad = CreditLoad.LOADING,
    /** Linked children; only parents get the switcher. */
    val children: List<CreditRecord> = emptyList(),
    val selected: CreditRecord? = null,
    val record: CreditRecord? = null,
    val view: CreditView = CreditView.BY_CATEGORY,
    val category: CreditCategory? = null,
    /** Expanded category codes / semester numbers, keyed by view. */
    val expanded: Set<String> = emptySet(),
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
) {
    val showChildSwitcher: Boolean get() = children.size > 1

    /** Categories shown in "By category": all, or the one picked in the filter. */
    val categories: List<CreditCategory>
        get() = category?.let(::listOf) ?: CreditCategory.entries.filter { c -> record?.categoryTotals(c)?.total?.let { it > 0 } == true }

    /** Semesters shown in "By semester" — those with credits in the filtered category. */
    val semesters: List<Int>
        get() {
            val r = record ?: return emptyList()
            return (1..r.totalSemesters).filter { r.semesterTotals(it, category).total > 0 }
        }

    val keysInView: List<String>
        get() = if (view == CreditView.BY_CATEGORY) categories.map { it.code } else semesters.map(::semesterKey)

    val allExpanded: Boolean get() = keysInView.isNotEmpty() && keysInView.all { it in expanded }
}

internal fun semesterKey(semester: Int) = "S$semester"

/**
 * Credit points for a student: totals, arrears, and breakdowns by category or semester.
 * Parents switch between their children; students see their own record.
 */
class SemCreditsViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val isParent = SessionPreferences.getRole(application) == Role.PARENT
    private val children = if (isParent) CreditRepository.children else listOf(CreditRepository.self)

    private val state = MutableStateFlow(
        SemCreditsUiState(
            children = children,
            selected = children.first(),
            view = savedStateHandle.get<String>(ARG_VIEW)
                ?.let { name -> CreditView.entries.firstOrNull { it.name == name } }
                ?: CreditView.BY_CATEGORY,
        ),
    )
    private var loadJob: Job? = null

    val uiState: StateFlow<SemCreditsUiState> =
        combine(state, NetworkMonitor.isOnline, CreditRepository.lastSyncedAt) { s, online, synced ->
            s.copy(isOnline = online, lastSyncedAt = synced)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        NetworkMonitor.start(application)
        load()
    }

    fun load() {
        val child = state.value.selected ?: return
        loadJob?.cancel()
        state.update { it.copy(load = CreditLoad.LOADING) }
        loadJob = viewModelScope.launch {
            try {
                val record = CreditRepository.load(child.studentId)
                state.update { it.copy(load = CreditLoad.READY, record = record) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(load = CreditLoad.ERROR) }
            }
        }
    }

    fun onChildSelected(child: CreditRecord) {
        if (child.studentId == state.value.selected?.studentId) return
        state.update { it.copy(selected = child, record = null, expanded = emptySet(), category = null) }
        load()
    }

    fun onViewSelected(view: CreditView) = state.update { it.copy(view = view) }

    fun onCategorySelected(category: CreditCategory?) = state.update { s ->
        // Filtering to one category opens it, so its semester table is visible straight away.
        val expanded = if (category != null && s.view == CreditView.BY_CATEGORY) s.expanded + category.code else s.expanded
        s.copy(category = category, expanded = expanded)
    }

    fun onToggle(key: String) = state.update { s ->
        s.copy(expanded = if (key in s.expanded) s.expanded - key else s.expanded + key)
    }

    fun onToggleAll() = state.update { s ->
        val keys = s.keysInView
        s.copy(expanded = if (s.allExpanded) s.expanded - keys.toSet() else s.expanded + keys)
    }

    companion object {
        const val ARG_VIEW = "view"
    }
}
