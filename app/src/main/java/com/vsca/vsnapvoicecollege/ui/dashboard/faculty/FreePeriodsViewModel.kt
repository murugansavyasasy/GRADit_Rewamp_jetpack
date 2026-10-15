package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FreeTab { ALL, UNALLOCATED, ALLOCATED }

/** Shown after an allocation: "P3 allocated to Mr. Arul Doss". */
data class AllocationNotice(val period: Int, val teacherName: String?)

data class FreePeriodsUiState(
    val load: LoadState = LoadState.LOADING,
    val directory: FacultyDirectory? = null,
    val tab: FreeTab = FreeTab.ALL,
    val divisionId: String? = null,
    /** Free period whose substitute picker is open. */
    val allocating: FreePeriod? = null,
    val notice: AllocationNotice? = null,
    val isOnline: Boolean = true,
) {
    val periods: List<FreePeriod> get() = directory?.freePeriods.orEmpty().sortedBy { it.period }
    val unallocated: Int get() = periods.count { it.allocatedToId == null }
    val allocated: Int get() = periods.size - unallocated

    val shown: List<FreePeriod>
        get() = periods.filter { p ->
            (divisionId == null || p.divisionId == divisionId) &&
                when (tab) {
                    FreeTab.ALL -> true
                    FreeTab.UNALLOCATED -> p.allocatedToId == null
                    FreeTab.ALLOCATED -> p.allocatedToId != null
                }
        }
}

/**
 * Today's free periods, created by staff who are absent or on leave, and who covers them.
 * Management allocates substitutes; teachers only see the plan.
 */
class FreePeriodsViewModel(application: Application) : AndroidViewModel(application) {

    val canAllocate = SessionPreferences.getRole(application) == Role.MANAGEMENT

    private val state = MutableStateFlow(FreePeriodsUiState())

    val uiState: StateFlow<FreePeriodsUiState> =
        combine(state, FacultyRepository.directory, NetworkMonitor.isOnline) { s, directory, online ->
            s.copy(directory = if (s.load == LoadState.READY) directory else null, isOnline = online)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        NetworkMonitor.start(application)
        load()
    }

    fun load() {
        state.update { it.copy(load = LoadState.LOADING) }
        viewModelScope.launch {
            try {
                FacultyRepository.loadDirectory()
                state.update { it.copy(load = LoadState.READY) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(load = LoadState.ERROR) }
            }
        }
    }

    fun onTab(tab: FreeTab) = state.update { it.copy(tab = tab) }

    fun onDivision(divisionId: String?) = state.update { it.copy(divisionId = divisionId) }

    fun onAllocate(period: FreePeriod) = state.update { it.copy(allocating = period) }

    fun onDismissAllocate() = state.update { it.copy(allocating = null) }

    /** Allocates (or, with null, frees up) the open period; the substitute gets notified. */
    fun onPickSubstitute(facultyId: String?) {
        val period = state.value.allocating ?: return
        FacultyRepository.allocate(period.id, facultyId)
        val name = facultyId?.let { FacultyRepository.directory.value.faculty(it)?.name }
        state.update { it.copy(allocating = null, notice = AllocationNotice(period.period, name)) }
    }

    fun onNoticeShown() = state.update { it.copy(notice = null) }
}
