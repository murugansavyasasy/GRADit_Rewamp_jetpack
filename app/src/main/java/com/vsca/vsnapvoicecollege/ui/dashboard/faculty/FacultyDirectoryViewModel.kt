package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LoadState { LOADING, ERROR, READY }

/** Which bottom sheet is open over the directory. */
sealed interface DirectorySheet {
    data object Stream : DirectorySheet
    data class Departments(val draft: Set<String>) : DirectorySheet
    data class Summary(val departmentId: String) : DirectorySheet
    data class Export(val format: ExportFormat = ExportFormat.PDF, val exporting: Boolean = false, val result: ExportedFile? = null) : DirectorySheet
}

data class DirectoryUiState(
    val load: LoadState = LoadState.LOADING,
    val directory: FacultyDirectory? = null,
    val filter: DirectoryFilter = DirectoryFilter(),
    val view: DirectoryView = DirectoryView.BY_DEPARTMENT,
    /** Department ids / division ids whose group is folded. */
    val collapsed: Set<String> = emptySet(),
    val sheet: DirectorySheet? = null,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val exportFailed: Boolean = false,
) {
    val faculty: List<Faculty> get() = directory?.filtered(filter).orEmpty()
    val counts: FacultyCounts get() = counts(faculty)
}

/**
 * The staff directory: who teaches where, today's presence, and exports of the
 * current selection. Filters cascade stream → departments → year / division → search.
 */
class FacultyDirectoryViewModel(application: Application) : AndroidViewModel(application) {

    val canManage = SessionPreferences.getRole(application) == Role.MANAGEMENT

    private val state = MutableStateFlow(DirectoryUiState())
    private var loadJob: Job? = null

    val uiState: StateFlow<DirectoryUiState> =
        combine(state, FacultyRepository.directory, NetworkMonitor.isOnline, FacultyRepository.lastSyncedAt) { s, directory, online, synced ->
            // Mutations (class teacher, head of department) show straight away once loaded.
            s.copy(directory = if (s.load == LoadState.READY) directory else s.directory, isOnline = online, lastSyncedAt = synced)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        NetworkMonitor.start(application)
        load()
    }

    fun load() {
        loadJob?.cancel()
        state.update { it.copy(load = LoadState.LOADING) }
        loadJob = viewModelScope.launch {
            try {
                val directory = FacultyRepository.loadDirectory()
                state.update { it.copy(load = LoadState.READY, directory = directory) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(load = LoadState.ERROR) }
            }
        }
    }

    fun onQueryChange(query: String) = updateFilter { it.copy(query = query) }

    fun onView(view: DirectoryView) = state.update { it.copy(view = view) }

    fun onToggleGroup(id: String) = state.update { s ->
        s.copy(collapsed = if (id in s.collapsed) s.collapsed - id else s.collapsed + id)
    }

    // ---- Filters ----

    fun openStreams() = state.update { it.copy(sheet = DirectorySheet.Stream) }

    /** Picking a stream clears the steps after it. */
    fun onStream(streamId: String?) {
        updateFilter { DirectoryFilter(streamId = streamId, query = it.query) }
        closeSheet()
    }

    fun openDepartments() = state.update { it.copy(sheet = DirectorySheet.Departments(it.filter.departmentIds)) }

    fun onToggleDepartmentDraft(id: String?) = state.update { s ->
        val sheet = s.sheet as? DirectorySheet.Departments ?: return@update s
        // "All departments" clears the selection.
        val draft = when {
            id == null -> emptySet()
            id in sheet.draft -> sheet.draft - id
            else -> sheet.draft + id
        }
        s.copy(sheet = sheet.copy(draft = draft))
    }

    fun onClearDepartmentDraft() = onToggleDepartmentDraft(null)

    fun onApplyDepartments() {
        val sheet = state.value.sheet as? DirectorySheet.Departments ?: return
        updateFilter { it.copy(departmentIds = sheet.draft, semester = null, section = null) }
        closeSheet()
    }

    fun onSemester(semester: Int?) = updateFilter { it.copy(semester = semester, section = null) }

    fun onSection(section: String?) = updateFilter { it.copy(section = section) }

    fun onRemoveStream() = updateFilter { DirectoryFilter(query = it.query) }

    fun onRemoveDepartment(id: String) = updateFilter { it.copy(departmentIds = it.departmentIds - id, semester = null, section = null) }

    fun onRemoveSemester() = updateFilter { it.copy(semester = null, section = null) }

    fun onRemoveSection() = updateFilter { it.copy(section = null) }

    fun onClearQuery() = updateFilter { it.copy(query = "") }

    fun onResetAll() = updateFilter { DirectoryFilter() }

    // ---- Department summary ----

    fun openSummary(departmentId: String) = state.update { it.copy(sheet = DirectorySheet.Summary(departmentId)) }

    /** "Show only Computer Science": narrows the list to that department. */
    fun onShowOnly(departmentId: String) {
        val dept = state.value.directory?.department(departmentId) ?: return
        updateFilter { it.copy(streamId = dept.streamId, departmentIds = setOf(departmentId), semester = null, section = null) }
        closeSheet()
    }

    // ---- Export ----

    fun openExport() = state.update { it.copy(sheet = DirectorySheet.Export(), exportFailed = false) }

    fun onExportFormat(format: ExportFormat) = state.update { s ->
        val sheet = s.sheet as? DirectorySheet.Export ?: return@update s
        s.copy(sheet = sheet.copy(format = format))
    }

    fun onExport() {
        val s = state.value
        val sheet = s.sheet as? DirectorySheet.Export ?: return
        val directory = s.directory ?: return
        state.update { it.copy(sheet = sheet.copy(exporting = true)) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    FacultyExport.export(getApplication(), directory, s.faculty, directory.exportBaseName(s.filter), sheet.format)
                }
            }
            state.update {
                it.copy(
                    sheet = sheet.copy(exporting = false, result = result.getOrNull()),
                    exportFailed = result.isFailure,
                )
            }
        }
    }

    fun closeSheet() = state.update { it.copy(sheet = null) }

    private fun updateFilter(change: (DirectoryFilter) -> DirectoryFilter) = state.update { it.copy(filter = change(it.filter)) }
}
