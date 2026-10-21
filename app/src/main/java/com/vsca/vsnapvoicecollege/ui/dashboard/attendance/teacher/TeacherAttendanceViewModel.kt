package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.teacher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRequest
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveStatus
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportClass
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.LoadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TeacherTab { CLASSES, LEAVES }

enum class SessionState { TAKEN, PENDING, MISSED, UPCOMING }

data class SessionCard(
    val session: TeacherSession,
    val reportClass: ReportClass?,
    val state: SessionState,
    val record: MarkingRecord?,
    /** Sent attendance can be changed until 6 PM the same day. */
    val canEdit: Boolean,
)

/** Dot under a day of the week strip: all taken (green), something left (grey), no classes (none). */
enum class DayDot { NONE, DONE, OPEN }

data class TeacherUiState(
    val load: LoadState = LoadState.LOADING,
    val isOnline: Boolean = true,
    val tab: TeacherTab = TeacherTab.CLASSES,
    val today: Day = Day.today(),
    /** Sunday the shown week starts on. */
    val weekStart: Day = startOfWeek(Day.today()),
    val selected: Day = Day.today(),
    val dots: Map<Day, DayDot> = emptyMap(),
    val cards: List<SessionCard> = emptyList(),
    /** Classes still to take today, for the tab badge. */
    val pendingToday: Int = 0,
    val leaveFilter: LeaveStatus = LeaveStatus.PENDING,
    val leaves: List<LeaveRequest> = emptyList(),
    val leaveCounts: Map<LeaveStatus, Int> = emptyMap(),
    /** Request whose reject sheet is open. */
    val rejecting: LeaveRequest? = null,
    val deciding: Boolean = false,
    val message: TeacherMessage? = null,
) {
    val weekDays: List<Day> get() = (0 until 7).map { weekStart.plusDays(it) }
    val pendingOnSelected: Int get() = cards.count { it.state == SessionState.PENDING || it.state == SessionState.MISSED }
}

sealed interface TeacherMessage {
    data class Decided(val name: String, val approved: Boolean) : TeacherMessage
    data object Failed : TeacherMessage
}

fun startOfWeek(day: Day): Day = day.plusDays(-(day.dayOfWeek - Calendar.SUNDAY))

/** Classes the advisor sees leave requests for. TODO: from the signed-in teacher's class-advisor role. */
private val ADVISED_CLASSES = setOf(LeaveRepository.CSE_5A)

/** The teacher's Attendance tab: classes by day with their marking, and leave requests to decide. */
class TeacherAttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val load = MutableStateFlow(LoadState.LOADING)
    private val ui = MutableStateFlow(TeacherUiState())

    val uiState: StateFlow<TeacherUiState> = combine(ui, load, TeacherRepository.records, LeaveRepository.requests, NetworkMonitor.isOnline) { s, load, records, leaves, online ->
        build(s.copy(load = load, isOnline = online), records, leaves)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeacherUiState())

    init {
        NetworkMonitor.start(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            load.value = LoadState.LOADING
            load.value = try {
                TeacherRepository.load()
                LoadState.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.ERROR
            }
        }
    }

    fun onTab(tab: TeacherTab) = ui.update { it.copy(tab = tab) }

    fun onDay(day: Day) = ui.update { it.copy(selected = day) }

    fun onWeek(delta: Int) = ui.update { s ->
        val start = s.weekStart.plusDays(7 * delta)
        // Keep the same weekday selected in the new week.
        s.copy(weekStart = start, selected = start.plusDays(s.selected.dayOfWeek - Calendar.SUNDAY))
    }

    fun onLeaveFilter(status: LeaveStatus) = ui.update { it.copy(leaveFilter = status) }

    fun approve(request: LeaveRequest) = decide(request, approve = true, note = null)

    fun openReject(request: LeaveRequest) = ui.update { it.copy(rejecting = request) }

    fun closeReject() = ui.update { it.copy(rejecting = null) }

    fun reject(note: String) {
        val request = ui.value.rejecting ?: return
        decide(request, approve = false, note = note)
    }

    private fun decide(request: LeaveRequest, approve: Boolean, note: String?) {
        if (ui.value.deciding) return
        ui.update { it.copy(deciding = true) }
        viewModelScope.launch {
            val message = try {
                LeaveRepository.decide(request.id, approve, note)
                TeacherMessage.Decided(request.studentName, approve)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                TeacherMessage.Failed
            }
            ui.update { it.copy(deciding = false, rejecting = if (message is TeacherMessage.Failed) it.rejecting else null, message = message) }
        }
    }

    fun onMessageShown() = ui.update { it.copy(message = null) }

    private fun build(s: TeacherUiState, records: Map<String, MarkingRecord>, leaves: List<LeaveRequest>): TeacherUiState {
        if (s.load != LoadState.READY) return s
        val today = Day.today()
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        fun cardsFor(day: Day) = TeacherRepository.sessionsOn(day).map { session ->
            val record = records[TeacherRepository.key(session.id, day)]
            val state = when {
                record != null -> SessionState.TAKEN
                day > today -> SessionState.UPCOMING
                day < today -> SessionState.MISSED
                else -> SessionState.PENDING
            }
            SessionCard(session, TeacherRepository.classOf(session), state, record, canEdit = record != null && day == today && now < EDIT_UNTIL_MINUTE)
        }
        val dots = s.weekDays.associateWith { day ->
            val cards = cardsFor(day)
            when {
                cards.isEmpty() -> DayDot.NONE
                cards.all { it.state == SessionState.TAKEN } -> DayDot.DONE
                else -> DayDot.OPEN
            }
        }
        val advised = leaves.filter { it.classId in ADVISED_CLASSES }
        return s.copy(
            today = today,
            dots = dots,
            cards = cardsFor(s.selected),
            pendingToday = cardsFor(today).count { it.state == SessionState.PENDING },
            leaves = advised.filter { it.status == s.leaveFilter }.sortedByDescending { it.appliedAt },
            leaveCounts = advised.groupingBy { it.status }.eachCount(),
        )
    }
}
