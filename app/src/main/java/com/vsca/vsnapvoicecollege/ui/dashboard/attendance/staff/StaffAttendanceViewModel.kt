package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.YearMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportFormat
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportedFile
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.Faculty
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyDirectory
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.LoadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StaffTab { TODAY, STAFF_WISE }

/** Search and filters; [todayGroup] and [belowOnly] come from tapping a summary tile. */
data class StaffFilter(
    val query: String = "",
    val streamId: String? = null,
    val departmentId: String? = null,
    val todayGroup: TodayGroup? = null,
    val belowOnly: Boolean = false,
) {
    val hasNarrowing: Boolean get() = query.isNotBlank() || streamId != null || departmentId != null
}

/** The export bottom sheet: pick a format, then the saved file. */
data class ExportSheetState(
    val tab: StaffTab,
    val format: ExportFormat = ExportFormat.PDF,
    val exporting: Boolean = false,
    val failed: Boolean = false,
    val result: ExportedFile? = null,
)

data class StaffRow(val faculty: Faculty, val summary: StaffSummary)

data class StaffAttendanceUiState(
    val load: LoadState = LoadState.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val directory: FacultyDirectory? = null,
    val today: Day = Day.today(),
    val tab: StaffTab = StaffTab.TODAY,
    val filter: StaffFilter = StaffFilter(),
    val year: Int = Day.today().year,
    /** Null = all months of [year]. */
    val month: Int? = Day.today().month,
    // Today
    val totalStaff: Int = 0,
    val todayCounts: Map<TodayGroup, Int> = emptyMap(),
    val todayGroups: List<Pair<TodayGroup, List<Faculty>>> = emptyList(),
    // Staff-wise
    val rows: List<StaffRow> = emptyList(),
    val averagePercent: Double = 0.0,
    val belowCount: Int = 0,
    val workingDays: Int = 0,
    val leaveDays: Int = 0,
    val sheet: ExportSheetState? = null,
) {
    val todayShown: Int get() = todayGroups.sumOf { it.second.size }

    /** No punches at all for the picked period (summer vacation). */
    val noDataForPeriod: Boolean get() = month != null && AcademicCalendar.isVacationMonth(YearMonth(year, month))

    val years: List<Int> get() = listOf(today.year - 1, today.year)

    /** Months of [year] that have started. */
    val months: List<Int> get() = if (year < today.year) (1..12).toList() else (1..today.month).toList()

    /** Free periods each absent staff member left today. */
    fun freePeriodsOf(facultyId: String): Int = directory?.freePeriods?.count { it.absentFacultyId == facultyId } ?: 0
}

/** Staff attendance for the principal: today's punch-ins and month / year summaries per staff member. */
class StaffAttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val load = MutableStateFlow(LoadState.LOADING)
    private val ui = MutableStateFlow(StaffAttendanceUiState())

    val uiState: StateFlow<StaffAttendanceUiState> = combine(
        ui,
        load,
        FacultyRepository.directory,
        FacultyRepository.lastSyncedAt,
        NetworkMonitor.isOnline,
    ) { state, load, directory, synced, online ->
        build(state.copy(load = load, isOnline = online, lastSyncedAt = synced), directory)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StaffAttendanceUiState())

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = LoadState.LOADING
            load.value = try {
                FacultyRepository.loadDirectory()
                LoadState.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.ERROR
            }
        }
    }

    fun onTab(tab: StaffTab) = ui.update { it.copy(tab = tab) }

    fun onQuery(query: String) = ui.update { it.copy(filter = it.filter.copy(query = query)) }

    fun onStream(streamId: String?) = ui.update { it.copy(filter = it.filter.copy(streamId = streamId, departmentId = null)) }

    fun onDepartment(departmentId: String?) = ui.update { it.copy(filter = it.filter.copy(departmentId = departmentId)) }

    /** Tapping a tile filters to that group; tapping it again (or "Total staff") clears it. */
    fun onTodayGroup(group: TodayGroup?) = ui.update {
        it.copy(filter = it.filter.copy(todayGroup = if (group == it.filter.todayGroup) null else group))
    }

    fun onBelowOnly(value: Boolean) = ui.update { it.copy(filter = it.filter.copy(belowOnly = value)) }

    fun onResetFilters() = ui.update { it.copy(filter = StaffFilter()) }

    fun onYear(year: Int) = ui.update { s ->
        val today = s.today
        // Keep the month if it exists in the new year; the current year stops at this month.
        val month = s.month?.let { if (year == today.year && it > today.month) today.month else it }
        s.copy(year = year, month = month)
    }

    fun onMonth(month: Int?) = ui.update { it.copy(month = month) }

    // ---- Export ----

    fun openExport() = ui.update { it.copy(sheet = ExportSheetState(it.tab)) }

    fun closeExport() = ui.update { it.copy(sheet = null) }

    fun onExportFormat(format: ExportFormat) = ui.update { s -> s.copy(sheet = s.sheet?.copy(format = format, failed = false)) }

    fun export() {
        val state = uiState.value
        val sheet = state.sheet ?: return
        if (sheet.exporting) return
        ui.update { it.copy(sheet = sheet.copy(exporting = true, failed = false)) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { StaffAttendanceExport.export(getApplication(), state, sheet.tab, sheet.format) }
            }.getOrNull()
            ui.update { s -> s.copy(sheet = s.sheet?.copy(exporting = false, failed = result == null, result = result)) }
        }
    }

    // ---- Derived lists ----

    private fun build(state: StaffAttendanceUiState, directory: FacultyDirectory): StaffAttendanceUiState {
        if (state.load != LoadState.READY) return state.copy(directory = null)
        val today = Day.today()
        val filter = state.filter
        val inScope = directory.faculty.filter { f ->
            val department = directory.department(f.departmentId)
            (filter.streamId == null || department?.streamId == filter.streamId) &&
                (filter.departmentId == null || f.inDepartment(filter.departmentId))
        }
        val needle = filter.query.trim()
        val searched = inScope.filter { f ->
            needle.isEmpty() || f.name.contains(needle, true) || f.employeeId.contains(needle, true)
        }.sortedBy { it.name.substringAfter(". ") }

        // Today
        val byGroup = searched.groupBy(::todayGroupOf)
        val todayGroups = TodayGroup.entries
            // Late staff are present too, so "Present" lists them as well.
            .filter { filter.todayGroup == null || it == filter.todayGroup || (filter.todayGroup == TodayGroup.PRESENT && it == TodayGroup.LATE) }
            .mapNotNull { g -> byGroup[g]?.takeIf { it.isNotEmpty() }?.let { g to it } }
        val counts = inScope.groupingBy(::todayGroupOf).eachCount()

        // Staff-wise
        val days = staffPeriodDays(state.year, state.month, today)
        val rows = if (state.tab == StaffTab.STAFF_WISE) searched.map { StaffRow(it, staffSummary(it, days, today)) } else emptyList()
        val allRows = if (state.tab == StaffTab.STAFF_WISE) inScope.map { staffSummary(it, days, today) } else emptyList()
        val shownRows = if (filter.belowOnly) rows.filter { it.summary.isBelowMinimum } else rows

        return state.copy(
            directory = directory,
            today = today,
            totalStaff = inScope.size,
            todayCounts = counts,
            todayGroups = todayGroups,
            rows = shownRows,
            averagePercent = allRows.filter { it.workingDays > 0 }.map { it.percent }.average().takeUnless { it.isNaN() } ?: 0.0,
            belowCount = allRows.count { it.isBelowMinimum },
            workingDays = AcademicCalendar.workingDays(days.firstOrNull() ?: today, days.lastOrNull() ?: today).size.takeIf { days.isNotEmpty() } ?: 0,
            leaveDays = allRows.sumOf { it.leave },
        )
    }
}
