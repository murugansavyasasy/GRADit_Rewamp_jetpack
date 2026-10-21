package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.teacher

import androidx.annotation.StringRes
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportClass
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportStudent
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.stableUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.Calendar

enum class SessionKind(@param:StringRes val label: Int) {
    GENERAL(R.string.att_kind_general),
    THEORY(R.string.att_kind_theory),
    PRACTICAL(R.string.att_kind_practical),
}

/** A class the teacher takes on a weekday: subject, class, room and time. */
data class TeacherSession(
    val id: String,
    val dayOfWeek: Int,
    val period: Int,
    val startMinute: Int,
    val endMinute: Int,
    val subject: String,
    val classId: String,
    val room: String,
    val kind: SessionKind,
)

enum class StudentStatus { PRESENT, ABSENT, ON_DUTY, ON_LEAVE }

/** Attendance sent for one session on one day. */
data class MarkingRecord(
    val sessionId: String,
    val day: Day,
    val kind: SessionKind,
    val topic: String,
    val statuses: Map<String, StudentStatus>,
    val sentAt: Long,
) {
    val present: Int get() = statuses.values.count { it == StudentStatus.PRESENT || it == StudentStatus.ON_DUTY }
}

/** Teachers can change attendance they sent until 6 PM the same day. */
const val EDIT_UNTIL_MINUTE = 18 * 60

/**
 * The signed-in teacher's timetable and the attendance they have sent.
 * TODO: load the timetable and marking from the attendance API; the teacher is Dr. Meena Krishnan.
 */
object TeacherRepository {

    const val TEACHER_NAME = "Dr. Meena Krishnan"
    const val TEACHER_DEPARTMENT = "CSE"

    private const val SEND_DELAY_MS = 900L
    private const val LOAD_DELAY_MS = 600L

    val timetable: List<TeacherSession> = buildList {
        fun s(day: Int, period: Int, start: Int, end: Int, subject: String, classId: String, room: String, kind: SessionKind) =
            add(TeacherSession("$day-$period", day, period, start, end, subject, classId, room, kind))
        val p1 = 9 * 60
        val p2 = 9 * 60 + 50
        val p3 = 11 * 60 + 30
        val p5 = 12 * 60 + 20
        val p6 = 14 * 60
        s(Calendar.MONDAY, 1, p1, p1 + 50, "DBMS", "cse-5-A", "Room B-204", SessionKind.THEORY)
        s(Calendar.MONDAY, 3, p3, 13 * 60 + 10, "DBMS Lab", "cse-5-B", "Lab 3", SessionKind.PRACTICAL)
        s(Calendar.TUESDAY, 1, p1, p1 + 50, "DBMS", "cse-5-A", "Room B-204", SessionKind.THEORY)
        s(Calendar.TUESDAY, 3, p3, 13 * 60 + 10, "DBMS Lab", "cse-5-B", "Lab 3", SessionKind.PRACTICAL)
        s(Calendar.TUESDAY, 6, p6, p6 + 50, "Mentoring hour", "cse-5-A", "Room B-204", SessionKind.GENERAL)
        s(Calendar.WEDNESDAY, 2, p2, p2 + 50, "Data Mining", "cse-7-A", "Room B-301", SessionKind.THEORY)
        s(Calendar.WEDNESDAY, 5, p5, p5 + 50, "DBMS", "cse-5-A", "Room B-204", SessionKind.THEORY)
        s(Calendar.THURSDAY, 1, p1, p1 + 50, "DBMS", "cse-5-B", "Room B-206", SessionKind.THEORY)
        s(Calendar.THURSDAY, 3, p3, 13 * 60 + 10, "DBMS Lab", "cse-5-A", "Lab 3", SessionKind.PRACTICAL)
        s(Calendar.FRIDAY, 2, p2, p2 + 50, "DBMS", "cse-5-A", "Room B-204", SessionKind.THEORY)
        s(Calendar.FRIDAY, 5, p5, p5 + 50, "Data Mining", "cse-7-A", "Room B-301", SessionKind.THEORY)
        s(Calendar.FRIDAY, 6, p6, p6 + 50, "Mentoring hour", "cse-5-A", "Room B-204", SessionKind.GENERAL)
        s(Calendar.SATURDAY, 1, p1, p1 + 50, "DBMS", "cse-5-B", "Room B-206", SessionKind.THEORY)
        s(Calendar.SATURDAY, 2, p2, p2 + 50, "Data Mining", "cse-7-A", "Room B-301", SessionKind.THEORY)
    }

    fun sessionsOn(day: Day): List<TeacherSession> =
        if (!AcademicCalendar.isWorkingDay(day)) emptyList() else timetable.filter { it.dayOfWeek == day.dayOfWeek }.sortedBy { it.startMinute }

    fun session(id: String): TeacherSession? = timetable.firstOrNull { it.id == id }

    fun classOf(session: TeacherSession): ReportClass? = ReportRepository.data().classById(session.classId)

    fun studentsOf(session: TeacherSession): List<ReportStudent> = classOf(session)?.students.orEmpty()

    private val _records = MutableStateFlow<Map<String, MarkingRecord>>(emptyMap())
    val records: StateFlow<Map<String, MarkingRecord>> = _records.asStateFlow()

    private var seeded = false
    private var loadedOnce = false

    fun key(sessionId: String, day: Day) = "${day.year}-${day.month}-${day.day}|$sessionId"

    fun record(sessionId: String, day: Day): MarkingRecord? = _records.value[key(sessionId, day)]

    /** Loads the week's marking. Offline, what was loaded stays; with nothing loaded yet this throws. */
    suspend fun load() {
        delay(LOAD_DELAY_MS)
        if (!NetworkMonitor.isOnline.value && !loadedOnce) throw IOException("offline")
        loadedOnce = true
        seed()
    }

    /** Statuses a fresh sheet starts with: everyone present, except students on approved leave. */
    fun initialStatuses(session: TeacherSession, day: Day): Map<String, StudentStatus> =
        studentsOf(session).associate { s ->
            s.id to if (LeaveRepository.approvedOn(s.id, day) != null) StudentStatus.ON_LEAVE else StudentStatus.PRESENT
        }

    suspend fun send(record: MarkingRecord) {
        delay(SEND_DELAY_MS)
        if (!NetworkMonitor.isOnline.value) throw IOException("offline")
        _records.update { it + (key(record.sessionId, record.day) to record) }
    }

    /**
     * Sample history: everything before today was taken; today the first class is taken
     * and the rest are pending.
     */
    private fun seed() {
        if (seeded) return
        seeded = true
        val today = Day.today()
        val seededRecords = mutableMapOf<String, MarkingRecord>()
        var day = today.plusDays(-21)
        while (day <= today) {
            val sessions = sessionsOn(day)
            val taken = if (day == today) sessions.take(1) else sessions
            taken.forEach { session ->
                val statuses = studentsOf(session).associate { s ->
                    val roll = stableUnit("mark", s.id, day.month, day.day, session.period)
                    s.id to when {
                        LeaveRepository.approvedOn(s.id, day) != null -> StudentStatus.ON_LEAVE
                        roll > s.rate -> StudentStatus.ABSENT
                        roll > s.rate - 0.02 -> StudentStatus.ON_DUTY
                        else -> StudentStatus.PRESENT
                    }
                }
                val sentAt = day.toCalendar().apply {
                    set(Calendar.HOUR_OF_DAY, session.startMinute / 60)
                    set(Calendar.MINUTE, session.startMinute % 60 + 6)
                }.timeInMillis
                seededRecords[key(session.id, day)] = MarkingRecord(session.id, day, session.kind, topicFor(session, day), statuses, sentAt)
            }
            day = day.plusDays(1)
        }
        _records.update { seededRecords + it }
    }

    private fun topicFor(session: TeacherSession, day: Day): String {
        val topics = when (session.subject) {
            "DBMS" -> listOf("ER model and relational mapping", "SQL joins and subqueries", "Functional dependencies", "Transactions and ACID", "Indexing with B+ trees")
            "DBMS Lab" -> listOf("Lab 4: SQL DDL and constraints", "Lab 5: Joins and views", "Lab 6: Normalization – 3NF and BCNF")
            "Data Mining" -> listOf("Association rules – Apriori", "Decision tree classifiers", "k-means clustering")
            else -> listOf("Mentoring and academic progress")
        }
        return topics[(day.day + session.period) % topics.size]
    }
}
