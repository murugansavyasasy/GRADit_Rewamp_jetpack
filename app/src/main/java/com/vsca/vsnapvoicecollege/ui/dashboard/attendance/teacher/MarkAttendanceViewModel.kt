package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.teacher

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportClass
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportStudent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudentSort { NAME, REGISTER }

enum class SendState { IDLE, SENDING, SENT, FAILED }

data class MarkUiState(
    val session: TeacherSession? = null,
    val reportClass: ReportClass? = null,
    val day: Day = Day.today(),
    /** Other periods of the day this class has with the teacher (for "Marking attendance for"). */
    val periods: List<TeacherSession> = emptyList(),
    val students: List<ReportStudent> = emptyList(),
    val statuses: Map<String, StudentStatus> = emptyMap(),
    val kind: SessionKind = SessionKind.GENERAL,
    val topic: String = "",
    val query: String = "",
    val sort: StudentSort = StudentSort.NAME,
    /** Null = all students. */
    val filter: StudentStatus? = null,
    val isEdit: Boolean = false,
    val confirming: Boolean = false,
    val send: SendState = SendState.IDLE,
) {
    fun count(status: StudentStatus): Int = statuses.values.count { it == status }

    val shown: List<ReportStudent>
        get() {
            val needle = query.trim()
            return students
                .filter { s -> filter == null || statuses[s.id] == filter }
                .filter { s -> needle.isEmpty() || s.name.contains(needle, true) || s.registerNo.contains(needle) }
                .sortedWith(if (sort == StudentSort.NAME) compareBy { it.name } else compareBy { it.registerNo })
        }

    val absentees: List<ReportStudent> get() = students.filter { statuses[it.id] == StudentStatus.ABSENT }.sortedBy { it.name }
}

/** "Mark attendance" for one class session: P / A per student, on duty / on leave, then send. */
class MarkAttendanceViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _ui = MutableStateFlow(MarkUiState())
    val uiState: StateFlow<MarkUiState> = _ui.asStateFlow()

    init {
        val session = TeacherRepository.session(savedStateHandle.get<String>(ARG_SESSION).orEmpty())
        val day = savedStateHandle.get<String>(ARG_DAY)?.let(::parseDay) ?: Day.today()
        if (session != null) load(session, day)
    }

    private fun load(session: TeacherSession, day: Day) {
        val record = TeacherRepository.record(session.id, day)
        _ui.update {
            it.copy(
                session = session,
                reportClass = TeacherRepository.classOf(session),
                day = day,
                periods = TeacherRepository.sessionsOn(day).filter { s -> s.classId == session.classId },
                students = TeacherRepository.studentsOf(session),
                statuses = record?.statuses ?: TeacherRepository.initialStatuses(session, day),
                kind = record?.kind ?: session.kind,
                topic = record?.topic.orEmpty(),
                isEdit = record != null,
                confirming = false,
                send = SendState.IDLE,
            )
        }
    }

    /** Switching period loads that period's sheet (sent or fresh). */
    fun onPeriod(session: TeacherSession) = load(session, _ui.value.day)

    fun onKind(kind: SessionKind) = _ui.update { it.copy(kind = kind) }

    fun onTopic(topic: String) = _ui.update { it.copy(topic = topic.take(MAX_TOPIC)) }

    fun onQuery(query: String) = _ui.update { it.copy(query = query) }

    fun onSort(sort: StudentSort) = _ui.update { it.copy(sort = sort) }

    fun onFilter(filter: StudentStatus?) = _ui.update { it.copy(filter = filter) }

    /** The circle switches between present and absent (and clears on duty / on leave). */
    fun onToggle(studentId: String) = _ui.update { s ->
        val next = if (s.statuses[studentId] == StudentStatus.ABSENT) StudentStatus.PRESENT else StudentStatus.ABSENT
        s.copy(statuses = s.statuses + (studentId to next))
    }

    fun onOnDuty(studentId: String, on: Boolean) = _ui.update { s ->
        s.copy(statuses = s.statuses + (studentId to if (on) StudentStatus.ON_DUTY else StudentStatus.PRESENT))
    }

    fun onOnLeave(studentId: String, on: Boolean) = _ui.update { s ->
        s.copy(statuses = s.statuses + (studentId to if (on) StudentStatus.ON_LEAVE else StudentStatus.PRESENT))
    }

    /** Everyone not on duty or on leave becomes present. */
    fun onMarkAllPresent() = _ui.update { s ->
        s.copy(statuses = s.statuses.mapValues { (_, v) -> if (v == StudentStatus.ABSENT) StudentStatus.PRESENT else v })
    }

    fun openConfirm() = _ui.update { it.copy(confirming = true) }

    fun closeConfirm() = _ui.update { it.copy(confirming = false) }

    fun send() {
        val s = _ui.value
        val session = s.session ?: return
        if (s.send == SendState.SENDING) return
        _ui.update { it.copy(send = SendState.SENDING) }
        viewModelScope.launch {
            val result = try {
                TeacherRepository.send(MarkingRecord(session.id, s.day, s.kind, s.topic.trim(), s.statuses, System.currentTimeMillis()))
                SendState.SENT
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SendState.FAILED
            }
            _ui.update { it.copy(send = result, confirming = result != SendState.SENT && it.confirming) }
        }
    }

    fun onFailureShown() = _ui.update { it.copy(send = SendState.IDLE) }

    companion object {
        const val ARG_SESSION = "sessionId"
        const val ARG_DAY = "day"
        private const val MAX_TOPIC = 120

        fun dayArg(day: Day) = "${day.year}-${day.month}-${day.day}"

        private fun parseDay(text: String): Day? = text.split("-").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }?.let { Day(it[0], it[1], it[2]) }
    }
}
