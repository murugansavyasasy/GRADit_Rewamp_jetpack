package com.vsca.vsnapvoicecollege.ui.dashboard.credits

import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Credit records for the signed-in family. Offline, the last loaded records stay
 * available (with when they were saved); with nothing loaded yet, loading fails.
 *
 * TODO: load from the academics API; the sample curriculum follows regulation 2021.
 */
object CreditRepository {

    private val _lastSyncedAt = MutableStateFlow(0L)
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    /** Children linked to the parent. TODO: take from the session. */
    val children: List<CreditRecord> by lazy { listOf(aarav(), diya()) }

    /** The signed-in student's own record. TODO: take from the session. */
    val self: CreditRecord get() = children.first()

    suspend fun load(studentId: String): CreditRecord {
        delay(LOAD_DELAY_MS)
        val record = children.firstOrNull { it.studentId == studentId } ?: throw IOException("unknown student")
        if (NetworkMonitor.isOnline.value) {
            _lastSyncedAt.value = System.currentTimeMillis()
        } else if (_lastSyncedAt.value == 0L) {
            throw IOException("offline")
        }
        return record
    }

    private const val LOAD_DELAY_MS = 700L

    // ---- Sample curriculum ----

    private fun c(code: String, name: String, sem: Int, cat: CreditCategory, credits: Int, status: CourseStatus, exam: String? = null) =
        Course(code, name, sem, cat, credits, status, exam)

    /** Status for a course in [sem] for a student currently in [current]. */
    private fun statusFor(sem: Int, current: Int) = when {
        sem < current -> CourseStatus.PASSED
        sem == current -> CourseStatus.ONGOING
        else -> CourseStatus.UPCOMING
    }

    /** Regulation 2021 B.E. / B.Tech layout: 165 credits over 8 semesters. */
    private fun curriculum(current: Int, sem3Core: Int, arrears: Map<String, String> = emptyMap()): List<Course> {
        fun s(sem: Int, code: String) = arrears[code]?.let { CourseStatus.ARREAR } ?: statusFor(sem, current)
        val list = mutableListOf(
            // Semester 1 · 22
            c("HS3151", "Professional English I", 1, CreditCategory.HS, 3, s(1, "HS3151")),
            c("MA3151", "Matrices and Calculus", 1, CreditCategory.BS, 4, s(1, "MA3151")),
            c("PH3151", "Engineering Physics", 1, CreditCategory.BS, 3, s(1, "PH3151")),
            c("CY3151", "Engineering Chemistry", 1, CreditCategory.BS, 3, s(1, "CY3151")),
            c("GE3151", "Problem Solving and Python Programming", 1, CreditCategory.ES, 3, s(1, "GE3151")),
            c("GE3171", "Python Programming Laboratory", 1, CreditCategory.ES, 3, s(1, "GE3171")),
            c("GE3172", "English Laboratory", 1, CreditCategory.EEC, 3, s(1, "GE3172")),
            // Semester 2 · 23
            c("HS3251", "Professional English II", 2, CreditCategory.HS, 3, s(2, "HS3251")),
            c("MA3251", "Statistics and Numerical Methods", 2, CreditCategory.BS, 4, s(2, "MA3251")),
            c("PH3256", "Physics for Information Science", 2, CreditCategory.BS, 3, s(2, "PH3256")),
            c("BE3251", "Basic Electrical and Electronics Engineering", 2, CreditCategory.ES, 3, s(2, "BE3251")),
            c("GE3251", "Engineering Graphics", 2, CreditCategory.ES, 4, s(2, "GE3251")),
            c("GE3271", "Engineering Practices Laboratory", 2, CreditCategory.ES, 3, s(2, "GE3271")),
            c("CS3251", "Programming in C", 2, CreditCategory.PC, 3, s(2, "CS3251")),
            // Semester 3
            c("MA3354", "Discrete Mathematics", 3, CreditCategory.BS, 4, s(3, "MA3354")),
            c("CS3351", "Digital Principles and Computer Organization", 3, CreditCategory.ES, 4, s(3, "CS3351")),
            c("CS3301", "Data Structures", 3, CreditCategory.PC, 3, s(3, "CS3301")),
            c("CS3391", "Object Oriented Programming", 3, CreditCategory.PC, 3, s(3, "CS3391")),
            c("CS3311", "Data Structures Laboratory", 3, CreditCategory.PC, sem3Core - 9, s(3, "CS3311")),
            c("CS3381", "Object Oriented Programming Laboratory", 3, CreditCategory.PC, 3, s(3, "CS3381")),
            // Semester 4 · 20
            c("GE3451", "Environmental Sciences and Sustainability", 4, CreditCategory.HS, 2, s(4, "GE3451")),
            c("MA3452", "Probability and Queueing Theory", 4, CreditCategory.BS, 4, s(4, "MA3452")),
            c("CS3452", "Theory of Computation", 4, CreditCategory.ES, 3, s(4, "CS3452")),
            c("CS3491", "Artificial Intelligence and Machine Learning", 4, CreditCategory.PC, 4, s(4, "CS3491")),
            c("CS3492", "Database Management Systems", 4, CreditCategory.PC, 3, s(4, "CS3492")),
            c("CS3401", "Algorithms", 4, CreditCategory.PC, 4, s(4, "CS3401")),
            // Semester 5 · 22
            c("CS3591", "Computer Networks", 5, CreditCategory.PC, 4, s(5, "CS3591")),
            c("CS3501", "Compiler Design", 5, CreditCategory.PC, 4, s(5, "CS3501")),
            c("CB3491", "Cryptography and Cyber Security", 5, CreditCategory.PC, 3, s(5, "CB3491")),
            c("CS3551", "Distributed Computing", 5, CreditCategory.PC, 3, s(5, "CS3551")),
            c("CS3561", "Networks Laboratory", 5, CreditCategory.PC, 2, s(5, "CS3561")),
            c("PE501", "Professional Elective I", 5, CreditCategory.PE, 3, s(5, "PE501")),
            c("CS3511", "Mini Project", 5, CreditCategory.EEC, 3, s(5, "CS3511")),
            // Semester 6 · 20
            c("HS3651", "Universal Human Values", 6, CreditCategory.HS, 2, s(6, "HS3651")),
            c("CCS356", "Object Oriented Software Engineering", 6, CreditCategory.PC, 4, s(6, "CCS356")),
            c("CS3691", "Embedded Systems and IoT", 6, CreditCategory.PC, 5, s(6, "CS3691")),
            c("PE601", "Professional Elective II", 6, CreditCategory.PE, 3, s(6, "PE601")),
            c("PE602", "Professional Elective III", 6, CreditCategory.PE, 3, s(6, "PE602")),
            c("OE601", "Open Elective I", 6, CreditCategory.OE, 3, s(6, "OE601")),
            // Semester 7 · 20
            c("GE3751", "Principles of Management", 7, CreditCategory.HS, 2, s(7, "GE3751")),
            c("CS3711", "Summer Internship", 7, CreditCategory.EEC, 2, s(7, "CS3711")),
            c("CS3701", "Cloud Computing", 7, CreditCategory.PC, 3, s(7, "CS3701")),
            c("CS3702", "Mobile Application Development", 7, CreditCategory.PC, 3, s(7, "CS3702")),
            c("PE701", "Professional Elective IV", 7, CreditCategory.PE, 3, s(7, "PE701")),
            c("OE701", "Open Elective II", 7, CreditCategory.OE, 3, s(7, "OE701")),
            c("CS3712", "Industry Training", 7, CreditCategory.EEC, 4, s(7, "CS3712")),
        )
        // Semester 8 takes up whatever is left of the 165 credits: a final elective and the project.
        val remaining = TOTAL_CREDITS - list.sumOf { it.credits }
        list += c("PE801", "Professional Elective V", 8, CreditCategory.PE, 3, s(8, "PE801"))
        list += c("CS3811", "Project Work", 8, CreditCategory.EEC, remaining - 3, s(8, "CS3811"))
        return list.map { course -> arrears[course.code]?.let { course.copy(arrearExam = it) } ?: course }
    }

    private const val TOTAL_CREDITS = 165

    private fun aarav() = CreditRecord(
        studentId = "aarav",
        name = "Aarav R.",
        initials = "AR",
        programme = "B.E. CSE",
        currentSemester = 5,
        section = "A",
        regulation = "2021",
        totalSemesters = 8,
        courses = curriculum(current = 5, sem3Core = 15, arrears = mapOf("MA3354" to "Nov 2026", "CS3351" to "Nov 2026")),
        resultMonths = mapOf(1 to "Nov 2024", 2 to "Apr 2025", 3 to "Nov 2025", 4 to "Apr 2026"),
    )

    private fun diya() = CreditRecord(
        studentId = "diya",
        name = "Diya R.",
        initials = "DR",
        programme = "B.Tech IT",
        currentSemester = 3,
        section = "B",
        regulation = "2021",
        totalSemesters = 8,
        courses = curriculum(current = 3, sem3Core = 14),
        resultMonths = mapOf(1 to "Nov 2025", 2 to "Apr 2026"),
    )
}
