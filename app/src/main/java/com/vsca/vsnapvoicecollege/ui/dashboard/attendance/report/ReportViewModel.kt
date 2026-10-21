package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.LoadState
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.PeriodSlot
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
import java.util.Calendar

/** One bar of the chart: "P1" / "Mon 5" / "CSE" and its percentage. */
data class ChartBar(val label: String, val percent: Double, val highlighted: Boolean = false, val present: Int = 0, val absent: Int = 0)

data class ClassRow(val reportClass: ReportClass, val totals: Totals)

data class GroupRow(val group: ReportStaffGroup, val totals: Totals)

data class ReportUiState(
    val load: LoadState = LoadState.LOADING,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val filter: ReportFilter = ReportFilter(),
    val today: Day = Day.today(),
    val span: DateSpan = DateSpan(Day.today(), Day.today()),
    val data: ReportData? = null,
    /** Average per working day for multi-day spans. */
    val current: Totals = Totals(),
    val previous: Totals = Totals(),
    val workingDays: Int = 0,
    /** By period for one day, by day for longer spans. */
    val chart: List<ChartBar> = emptyList(),
    val showTable: Boolean = false,
    val classes: List<ClassRow> = emptyList(),
    val groups: List<GroupRow> = emptyList(),
    val departments: List<Pair<String, String>> = emptyList(),
    val semesters: List<Int> = emptyList(),
    val sections: List<String> = emptyList(),
) {
    /** Nothing was marked: a Sunday, a holiday or a day not marked yet. */
    val isEmpty: Boolean get() = data != null && current.marked == 0

    val studentCount: Int get() = classes.sumOf { it.reportClass.students.size }
}

/** The principal's attendance report: students by class or staff by group, for a day, week or month. */
class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val load = MutableStateFlow(LoadState.LOADING)
    private val ui = MutableStateFlow(ReportUiState())

    val uiState: StateFlow<ReportUiState> = combine(ui, load, ReportRepository.lastSyncedAt, NetworkMonitor.isOnline) { state, load, synced, online ->
        val s = state.copy(load = load, isOnline = online, lastSyncedAt = synced)
        if (load == LoadState.READY) build(s, ReportRepository.data()) else s.copy(data = null)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = LoadState.LOADING
            load.value = try {
                ReportRepository.load()
                LoadState.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.ERROR
            }
        }
    }

    private fun updateFilter(change: (ReportFilter) -> ReportFilter) = ui.update { it.copy(filter = change(it.filter)) }

    fun onRange(range: ReportRange) = updateFilter { it.copy(range = range, customDay = null) }

    fun onCustomDay(day: Day) = updateFilter { it.copy(range = ReportRange.CUSTOM, customDay = day) }

    fun onDepartment(id: String?) = updateFilter { it.copy(departmentId = id, semester = null, section = null) }

    fun onSemester(semester: Int?) = updateFilter { it.copy(semester = semester, section = null) }

    fun onSection(section: String?) = updateFilter { it.copy(section = section) }

    fun onClearFilters() = updateFilter { it.copy(departmentId = null, semester = null, section = null) }

    fun onAudience(audience: Audience) = updateFilter { it.copy(audience = audience) }

    fun onToggleTable() = ui.update { it.copy(showTable = !it.showTable) }

    /** Remembers the current report for the export screen. */
    fun prepareExport() {
        ReportExportSession.filter = uiState.value.filter
        ReportExportSession.result = null
    }

    private fun build(state: ReportUiState, data: ReportData): ReportUiState {
        val today = Day.today()
        val f = state.filter
        val span = spanFor(f, today)
        val previous = previousSpan(f, span)
        val classes = data.classes.filter { c ->
            (f.departmentId == null || c.division.departmentId == f.departmentId) &&
                (f.semester == null || c.division.semester == f.semester) &&
                (f.section == null || c.division.section == f.section)
        }
        val groups = data.staffGroups.filter { g -> f.departmentId == null || g.departmentId == f.departmentId }
        val workingDays = span.workingDays.filter { it <= today }

        val chart: List<ChartBar>
        val current: Totals
        val before: Totals
        val classRows: List<ClassRow>
        val groupRows: List<GroupRow>
        when (f.audience) {
            Audience.STUDENTS -> {
                classRows = classes.map { c -> ClassRow(c, workingDays.fold(Totals()) { acc, d -> acc + c.totalsOn(d, today) }) }
                current = classRows.fold(Totals()) { acc, r -> acc + r.totals }.perDay(workingDays.size)
                val prevDays = previous.workingDays
                before = classes.fold(Totals()) { acc, c -> prevDays.fold(acc) { a, d -> a + c.totalsOn(d, today) } }.perDay(prevDays.size)
                chart = if (span.isSingleDay) periodBars(classes, span.from, today) else dayBars(workingDays) { d -> classes.fold(Totals()) { a, c -> a + c.totalsOn(d, today) } }
                groupRows = emptyList()
            }
            Audience.STAFF -> {
                groupRows = groups.map { g -> GroupRow(g, workingDays.fold(Totals()) { acc, d -> acc + g.totalsOn(d, today) }) }
                current = groupRows.fold(Totals()) { acc, r -> acc + r.totals }.perDay(workingDays.size)
                val prevDays = previous.workingDays
                before = groups.fold(Totals()) { acc, g -> prevDays.fold(acc) { a, d -> a + g.totalsOn(d, today) } }.perDay(prevDays.size)
                chart = if (span.isSingleDay) {
                    groupRows.map { ChartBar(it.group.name.take(8), it.totals.percent, present = it.totals.present, absent = it.totals.absent) }
                } else {
                    dayBars(workingDays) { d -> groups.fold(Totals()) { a, g -> a + g.totalsOn(d, today) } }
                }
                classRows = emptyList()
            }
        }
        val deptDivisions = data.classes.filter { f.departmentId == null || it.division.departmentId == f.departmentId }
        return state.copy(
            data = data,
            today = today,
            span = span,
            current = current,
            previous = before,
            workingDays = workingDays.size,
            chart = chart,
            classes = classRows.sortedBy { it.reportClass.shortName },
            groups = groupRows.sortedWith(compareBy<GroupRow> { !it.group.teaching }.thenBy { it.group.name }),
            departments = data.directory.departments.map { it.id to it.name },
            semesters = deptDivisions.map { it.division.semester }.distinct().sorted(),
            sections = deptDivisions.filter { f.semester == null || it.division.semester == f.semester }.map { it.division.section }.distinct().sorted(),
        )
    }

    /** P1…P6 for one day, weighted by class size; the period in progress is highlighted. */
    private fun periodBars(classes: List<ReportClass>, day: Day, today: Day): List<ChartBar> {
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val students = classes.sumOf { it.students.size }.coerceAtLeast(1)
        return PeriodSlot.entries.take(PERIODS_PER_DAY).mapNotNull { slot ->
            // Today's later periods haven't been marked yet.
            if (day == today && slot.startMinute > now) return@mapNotNull null
            val weighted = classes.sumOf { it.periodPercent(day, slot.number, today) * it.students.size } / students
            val present = Math.round(students * weighted / 100).toInt()
            ChartBar("P${slot.number}", weighted, highlighted = day == today && now in slot.startMinute until slot.endMinute, present = present, absent = students - present)
        }
    }

    private fun dayBars(days: List<Day>, totalsOf: (Day) -> Totals): List<ChartBar> = days.map { d ->
        val t = totalsOf(d)
        ChartBar(d.day.toString(), t.percent, present = t.present, absent = t.absent)
    }

    companion object {
        const val PERIODS_PER_DAY = 6
    }
}
