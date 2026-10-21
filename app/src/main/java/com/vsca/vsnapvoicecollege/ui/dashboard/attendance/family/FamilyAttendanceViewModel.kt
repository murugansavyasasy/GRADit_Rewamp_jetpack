package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.family

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.YearMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRequest
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.Mark
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportClass
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportStudent
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.studentMark
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.subjectAt
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.stableUnit
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.LoadState
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.PeriodSlot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** How a day reads on the family calendar. */
enum class DayKind { PRESENT, PARTLY, ABSENT, LEAVE, HOLIDAY, NONE }

enum class PeriodMark { PRESENT, ABSENT, LEAVE }

/** One period of a day: P1 Database Management Systems 09:00 – 09:50, present. */
data class PeriodEntry(val slot: PeriodSlot, val subject: String, val mark: PeriodMark)

data class SubjectFigure(val subject: String, val teacher: String, val attended: Int, val absent: Int) {
    val percent: Double get() = percentOf(attended, attended + absent)
}

enum class FamilyTab { ATTENDANCE, LEAVES }

enum class FamilyView { CALENDAR, SUBJECTS }

data class FamilyChild(val student: ReportStudent, val reportClass: ReportClass)

data class FamilyUiState(
    val load: LoadState = LoadState.LOADING,
    val isOnline: Boolean = true,
    val children: List<FamilyChild> = emptyList(),
    val child: FamilyChild? = null,
    val tab: FamilyTab = FamilyTab.ATTENDANCE,
    val view: FamilyView = FamilyView.CALENDAR,
    val today: Day = Day.today(),
    val month: YearMonth = Day.today().yearMonth,
    val semesterStart: Day = AcademicCalendar.semesterStart(Day.today()),
    val presentDays: Int = 0,
    val workingDays: Int = 0,
    val days: Map<Day, DayKind> = emptyMap(),
    val subjects: List<SubjectFigure> = emptyList(),
    val leaves: List<LeaveRequest> = emptyList(),
    /** Day whose period-wise sheet is open. */
    val openDay: Day? = null,
) {
    val percent: Double get() = percentOf(presentDays, workingDays)
    val showChildSwitcher: Boolean get() = children.size > 1
    fun monthCount(kind: DayKind): Int = days.count { (d, k) -> d.yearMonth == month && (k == kind || (kind == DayKind.PRESENT && k == DayKind.PARTLY)) }
}

/** Periods in a college day on the family timetable. */
const val FAMILY_PERIODS = 5

private const val PARTLY_PRESENT_SHARE = 0.12

/**
 * Period-wise marks for a student on [day]. A present day can still miss a period ("partly
 * present"); approved leave covers the whole day.
 *
 * TODO: read period marks from the attendance API.
 */
fun periodMarks(child: FamilyChild, day: Day, today: Day): List<PeriodEntry> {
    val s = child.student
    if (day > today || !AcademicCalendar.isWorkingDay(day)) return emptyList()
    val dayMark = if (LeaveRepository.approvedOn(s.id, day) != null) Mark.LEAVE else studentMark(s, day, today)
    return PeriodSlot.entries.take(FAMILY_PERIODS).map { slot ->
        val mark = when (dayMark) {
            Mark.ABSENT -> PeriodMark.ABSENT
            Mark.LEAVE -> PeriodMark.LEAVE
            Mark.OFF -> PeriodMark.ABSENT
            // Most present days are fully present; about one in eight misses a single period.
            Mark.PRESENT, Mark.LATE -> {
                val partly = stableUnit("pd", s.id, day.month, day.day) < PARTLY_PRESENT_SHARE
                val missed = 1 + (stableUnit("pm", s.id, day.month, day.day) * FAMILY_PERIODS).toInt()
                if (partly && slot.number == missed) PeriodMark.ABSENT else PeriodMark.PRESENT
            }
        }
        PeriodEntry(slot, child.reportClass.subjectAt(day, slot.number)?.name.orEmpty(), mark)
    }
}

fun dayKind(child: FamilyChild, day: Day, today: Day): DayKind {
    if (day > today) return DayKind.NONE
    if (!AcademicCalendar.isWorkingDay(day)) return DayKind.HOLIDAY
    val marks = periodMarks(child, day, today)
    return when {
        marks.all { it.mark == PeriodMark.LEAVE } -> DayKind.LEAVE
        marks.all { it.mark == PeriodMark.ABSENT } -> DayKind.ABSENT
        marks.all { it.mark == PeriodMark.PRESENT } -> DayKind.PRESENT
        else -> DayKind.PARTLY
    }
}

/** The family's attendance: overall %, the month calendar, subject-wise hours and leave history. */
class FamilyAttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val studentIds = when (SessionPreferences.getRole(application)) {
        Role.PARENT -> listOf("cse-5-A-aarav", "it-3-B-diya")
        else -> listOf("cse-5-A-aarav")
    }
    private val load = MutableStateFlow(LoadState.LOADING)
    private val ui = MutableStateFlow(FamilyUiState())

    val uiState: StateFlow<FamilyUiState> = combine(ui, load, LeaveRepository.requests, NetworkMonitor.isOnline) { s, load, leaves, online ->
        build(s.copy(load = load, isOnline = online), leaves)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyUiState())

    private var loadedOnce = false

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    /** TODO: load from the attendance API; offline keeps what was loaded. */
    fun refresh() {
        viewModelScope.launch {
            load.value = LoadState.LOADING
            load.value = try {
                delay(LOAD_DELAY_MS)
                if (!NetworkMonitor.isOnline.value && !loadedOnce) throw IOException("offline")
                loadedOnce = true
                LoadState.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.ERROR
            }
        }
    }

    fun onChild(studentId: String) = ui.update { s -> s.copy(child = s.children.firstOrNull { it.student.id == studentId }) }

    fun onTab(tab: FamilyTab) = ui.update { it.copy(tab = tab) }

    fun onView(view: FamilyView) = ui.update { it.copy(view = view) }

    fun onMonth(delta: Int) = ui.update { s ->
        val next = s.month.plus(delta)
        if (next > s.today.yearMonth || next < s.semesterStart.yearMonth) s else s.copy(month = next)
    }

    fun openDay(day: Day) = ui.update { it.copy(openDay = day) }

    fun closeDay() = ui.update { it.copy(openDay = null) }

    private fun build(s: FamilyUiState, leaves: List<LeaveRequest>): FamilyUiState {
        if (s.load != LoadState.READY) return s
        val data = ReportRepository.data()
        val children = studentIds.mapNotNull { id ->
            val student = data.student(id) ?: return@mapNotNull null
            data.classById(student.classId)?.let { FamilyChild(student, it) }
        }
        val child = s.child?.let { c -> children.firstOrNull { it.student.id == c.student.id } } ?: children.firstOrNull() ?: return s.copy(children = children)
        val today = Day.today()
        val start = AcademicCalendar.semesterStart(today)
        val working = AcademicCalendar.workingDays(start, today)
        val kinds = working.associateWith { dayKind(child, it, today) }
        // Every day of the semester for the calendar (holidays and Sundays included).
        val calendar = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= today }.associateWith { kinds[it] ?: DayKind.HOLIDAY }
        val subjectCounts = mutableMapOf<String, Pair<Int, Int>>()
        working.forEach { day ->
            periodMarks(child, day, today).forEach { p ->
                if (p.subject.isNotEmpty() && p.mark != PeriodMark.LEAVE) {
                    val (a, b) = subjectCounts[p.subject] ?: (0 to 0)
                    subjectCounts[p.subject] = if (p.mark == PeriodMark.PRESENT) (a + 1) to b else a to (b + 1)
                }
            }
        }
        val teachers = child.reportClass.subjects.associate { it.name to it.teacher }
        return s.copy(
            children = children,
            child = child,
            today = today,
            semesterStart = start,
            presentDays = kinds.values.count { it == DayKind.PRESENT || it == DayKind.PARTLY },
            workingDays = working.size,
            days = calendar,
            subjects = subjectCounts.map { (name, counts) -> SubjectFigure(name, teachers[name].orEmpty(), counts.first, counts.second) }.sortedByDescending { it.percent },
            leaves = leaves.filter { it.studentId == child.student.id }.sortedByDescending { it.appliedAt },
        )
    }

    companion object {
        private const val LOAD_DELAY_MS = 600L
    }
}
