package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave

import androidx.annotation.StringRes
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.Calendar
import java.util.UUID

enum class LeaveType(@param:StringRes val label: Int) {
    CASUAL(R.string.att_leave_casual),
    SICK(R.string.att_leave_sick),
    MEDICAL(R.string.att_leave_medical),
    ON_DUTY(R.string.att_leave_on_duty),
}

enum class LeaveStatus { PENDING, APPROVED, REJECTED }

/** A student's leave request, applied by the student or a parent and decided by the class advisor. */
data class LeaveRequest(
    val id: String,
    val studentId: String,
    val studentName: String,
    val initials: String,
    /** "B.E. CSE · Sem 5 A". */
    val classLabel: String,
    /** "B.E. CSE · Year 3 · Sem 5 A". */
    val classLongLabel: String,
    val classId: String,
    val registerNo: String,
    val type: LeaveType,
    val from: Day,
    val to: Day,
    val appliedAt: Long,
    val reason: String,
    val attachment: FileAttachment? = null,
    val status: LeaveStatus = LeaveStatus.PENDING,
    /** Why it was rejected, shown to the student and parent. */
    val decisionNote: String? = null,
    val decidedAt: Long? = null,
) {
    /** Working days the leave covers (Sundays and holidays aren't counted). */
    val days: Int get() = AcademicCalendar.workingDays(from, to).size

    fun covers(day: Day): Boolean = day in from..to
}

/**
 * Leave requests shared by the family "Apply leave" form and the class advisor's
 * "Leave requests" tab, so a request sent by a parent shows up for the teacher at once.
 *
 * TODO: replace with the leave API.
 */
object LeaveRepository {

    private val _requests = MutableStateFlow(sample())
    val requests: StateFlow<List<LeaveRequest>> = _requests.asStateFlow()

    fun byId(id: String): LeaveRequest? = _requests.value.firstOrNull { it.id == id }

    /** Approved leave for [studentId] on [day], if any. */
    fun approvedOn(studentId: String, day: Day): LeaveRequest? =
        _requests.value.firstOrNull { it.studentId == studentId && it.status == LeaveStatus.APPROVED && it.covers(day) }

    suspend fun submit(request: LeaveRequest) {
        delay(SEND_DELAY_MS)
        if (!NetworkMonitor.isOnline.value) throw IOException("offline")
        _requests.update { listOf(request) + it }
    }

    suspend fun decide(id: String, approve: Boolean, note: String?) {
        delay(SEND_DELAY_MS)
        if (!NetworkMonitor.isOnline.value) throw IOException("offline")
        _requests.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = if (approve) LeaveStatus.APPROVED else LeaveStatus.REJECTED,
                        decisionNote = note?.trim()?.takeIf(String::isNotEmpty),
                        decidedAt = System.currentTimeMillis(),
                    )
                } else {
                    it
                }
            }
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    // ---- Sample data ----

    private const val SEND_DELAY_MS = 800L
    const val CSE_5A = "cse-5-A"
    private const val CSE_5A_LABEL = "B.E. CSE · Sem 5 A"
    private const val CSE_5A_LONG = "B.E. CSE · Year 3 · Sem 5 A"

    private fun at(dayOffset: Int, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }.timeInMillis

    private fun sample(): List<LeaveRequest> {
        val today = Day.today()
        fun d(offset: Int) = today.plusDays(offset)
        return listOf(
            LeaveRequest(
                "lv-rahul", "$CSE_5A-rahul", "Rahul Kumar", "RK", CSE_5A_LABEL, CSE_5A_LONG, CSE_5A, "2024503041", LeaveType.SICK,
                d(1), d(2), at(0, 8, 12),
                "Running a high fever since last night. The doctor has advised two days of rest. I will catch up on the DBMS lab record with Sneha’s notes.",
                FileAttachment("Medical_certificate.pdf", 240 * 1024, AttachmentType.PDF),
            ),
            LeaveRequest(
                "lv-sneha", "$CSE_5A-sneha", "Sneha Nair", "SN", CSE_5A_LABEL, CSE_5A_LONG, CSE_5A, "2024503052", LeaveType.CASUAL,
                d(3), d(3), at(-1, 19, 40),
                "Family function at our native place in Thanjavur.",
            ),
            LeaveRequest(
                "lv-aarav-1", "$CSE_5A-aarav", "Aarav R.", "AR", CSE_5A_LABEL, CSE_5A_LONG, CSE_5A, "2024503017", LeaveType.SICK,
                d(-16), d(-15), at(-17, 7, 55), "Viral fever; resting at home on the doctor’s advice.",
                status = LeaveStatus.APPROVED, decidedAt = at(-17, 10, 5),
            ),
            LeaveRequest(
                "lv-aarav-2", "$CSE_5A-aarav", "Aarav R.", "AR", CSE_5A_LABEL, CSE_5A_LONG, CSE_5A, "2024503017", LeaveType.ON_DUTY,
                d(-30), d(-30), at(-33, 18, 20), "Representing the college at the inter-college coding contest.",
                status = LeaveStatus.APPROVED, decidedAt = at(-32, 9, 30),
            ),
            LeaveRequest(
                "lv-aarav-3", "$CSE_5A-aarav", "Aarav R.", "AR", CSE_5A_LABEL, CSE_5A_LONG, CSE_5A, "2024503017", LeaveType.CASUAL,
                d(-44), d(-44), at(-46, 20, 10), "Cousin’s wedding.",
                status = LeaveStatus.REJECTED, decisionNote = "Exam week", decidedAt = at(-45, 11, 0),
            ),
            LeaveRequest(
                "lv-karthik", "$CSE_5A-karthik", "Karthik S.", "KS", CSE_5A_LABEL, CSE_5A_LONG, CSE_5A, "2024503033", LeaveType.MEDICAL,
                d(-9), d(-7), at(-10, 9, 0), "Minor surgery; certificate attached.",
                FileAttachment("Discharge_summary.pdf", 410 * 1024, AttachmentType.PDF),
                status = LeaveStatus.APPROVED, decidedAt = at(-10, 12, 15),
            ),
            LeaveRequest(
                "lv-janani", "$CSE_5A-janani", "Janani K.", "JK", CSE_5A_LABEL, CSE_5A_LONG, CSE_5A, "2024503029", LeaveType.CASUAL,
                d(-20), d(-20), at(-22, 17, 45), "Passport appointment in Chennai.",
                status = LeaveStatus.APPROVED, decidedAt = at(-21, 9, 40),
            ),
        )
    }
}
