package com.vsca.vsnapvoicecollege.ui.dashboard.placement

import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.Calendar

/**
 * In-memory source of placement events posted by the placement cell, shared by the
 * family list, the student list and the event detail screen.
 *
 * TODO: replace the seeded sample data with the placement events API.
 */
object PlacementRepository {

    // ---- The signed-in family and sample constants — declared before the seeded events use them. ----

    private val AARAV = PlacementChild("aarav", "Aarav", "AR", "B.E. CSE", batch = 2024)
    private val DIYA = PlacementChild("diya", "Diya", "DR", "B.Tech IT", batch = 2025)

    private const val ONLINE = "Online · video meeting"
    private val CSE_IT = listOf("B.E. CSE", "B.Tech IT")

    /**
     * Whose eligibility is shown: the parent's children, or the student themself.
     * TODO: take the linked children / the student's course from the session.
     */
    fun familyFor(role: Role): List<PlacementChild> = when (role) {
        Role.PARENT -> listOf(AARAV, DIYA)
        Role.STUDENT -> listOf(AARAV)
        Role.FACULTY, Role.MANAGEMENT -> emptyList()
    }

    private val _events = MutableStateFlow(sampleEvents())
    val events: StateFlow<List<PlacementEvent>> = _events.asStateFlow()

    private var hasLoaded = false
    private val _lastSyncedAt = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    /**
     * Fetches the latest events. Offline, the last loaded list stays on screen; with
     * nothing loaded yet this throws so the screen can show "Couldn't load".
     * TODO: call the placement events API instead of simulating latency.
     */
    suspend fun refresh() {
        delay(LOAD_DELAY_MS)
        if (!NetworkMonitor.isOnline.value) {
            if (!hasLoaded) throw IOException("offline")
            return
        }
        hasLoaded = true
        _lastSyncedAt.value = System.currentTimeMillis()
    }

    fun byId(id: String): PlacementEvent? = _events.value.firstOrNull { it.id == id }

    // ---- Sample data ----

    private const val LOAD_DELAY_MS = 700L

    /** Epoch millis for [hour]:[minute] on the day [dayOffset] days from today. */
    private fun at(hour: Int, minute: Int, dayOffset: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun company(name: String, initials: String) = PlacementCompany(name, initials)

    private fun sampleEvents(): List<PlacementEvent> = listOf(
        PlacementEvent(
            id = "freshworks-ppt", title = "Freshworks Pre-placement Talk",
            subtitle = "Summer Internship 2027 for pre-final year students",
            type = PlacementEventType.PRE_PLACEMENT_TALK, mode = PlacementMode.VIRTUAL,
            startAt = at(10, 0, 1), endAt = at(11, 0, 1), venue = ONLINE,
            companies = listOf(company("Freshworks", "FW")), eligibleCourses = CSE_IT,
            criteria = listOf("Pre-final year students (2028 batch)", "Attendance is mandatory for the internship drive"),
            offer = "Summer intern · ₹40,000 a month",
            joinUrl = "https://meet.google.com/fwk-ppt-2027",
        ),
        PlacementEvent(
            id = "infosys-drive", title = "Infosys Campus Drive 2026",
            subtitle = "InStep Internship and Systems Engineer roles",
            type = PlacementEventType.CAMPUS_DRIVE, mode = PlacementMode.ON_CAMPUS,
            startAt = at(9, 0, 11), endAt = at(17, 0, 11), venue = "Main auditorium, Block A",
            companies = listOf(company("Infosys", "IN")), eligibleCourses = CSE_IT + "B.E. ECE",
            criteria = listOf("60% and above in 10th, 12th and degree", "No standing arrears", "2027 passing-out batch"),
            offer = "Systems engineer · ₹3.6 LPA",
        ),
        PlacementEvent(
            id = "deloitte-analyst", title = "Deloitte Analyst Hiring",
            subtitle = "Audit and tax analyst roles for commerce and MBA graduates",
            type = PlacementEventType.PLACEMENT_EVENT, mode = PlacementMode.ON_CAMPUS,
            startAt = at(10, 30, 15), endAt = at(16, 0, 15), venue = "Seminar hall, Block B",
            companies = listOf(company("Deloitte", "DE")), eligibleCourses = listOf("B.Com", "MBA"),
            criteria = listOf("CGPA 6.5 and above", "No standing arrears"),
            offer = "Audit analyst · ₹6.5 LPA",
        ),
        PlacementEvent(
            id = "chennai-job-fair", title = "Chennai IT Job Fair 2026",
            subtitle = "Internships and graduate roles from 12 companies",
            type = PlacementEventType.JOB_FAIR, mode = PlacementMode.OFF_CAMPUS,
            startAt = at(10, 0, 24), endAt = at(16, 0, 24), venue = "Chennai Trade Centre, Nandambakkam",
            companies = listOf(
                company("Zoho", "ZO"), company("Freshworks", "FW"), company("TCS", "TC"),
                company("Infosys", "IN"), company("Wipro", "WI"), company("HCLTech", "HC"),
            ),
            eligibleCourses = CSE_IT + listOf("B.E. ECE", "B.E. EEE"),
            criteria = listOf("Carry 5 printed copies of your resume", "College ID card is required at the entrance"),
        ),
        PlacementEvent(
            id = "tata-electronics", title = "Placement on campus",
            subtitle = "Computer science placement",
            type = PlacementEventType.PLACEMENT_EVENT, mode = PlacementMode.ON_CAMPUS,
            startAt = at(10, 40, -8), endAt = null, venue = "Campus",
            companies = listOf(company("Tata Electronics", "TE")), eligibleCourses = CSE_IT,
            criteria = listOf("CGPA 7.0 and above"),
        ),
        PlacementEvent(
            id = "zoho-internship", title = "Zoho Summer Internship Drive",
            subtitle = "Member Technical Staff interns for summer 2027",
            type = PlacementEventType.CAMPUS_DRIVE, mode = PlacementMode.ON_CAMPUS,
            startAt = at(9, 30, -25), endAt = at(15, 0, -25), venue = "Main auditorium, Block A",
            companies = listOf(company("Zoho", "ZO")), eligibleCourses = CSE_IT,
            criteria = listOf("No standing arrears", "7.0 CGPA and above"),
        ),
        PlacementEvent(
            id = "edutech-fair", title = "EduTech Fair",
            subtitle = "Internship program for graduates",
            type = PlacementEventType.PLACEMENT_EVENT, mode = PlacementMode.VIRTUAL,
            startAt = at(10, 0, -30), endAt = null, venue = "Chennai",
            companies = listOf(company("Vuram", "VU"), company("AVM Studios", "AV"), company("Deloitte", "DE")),
            eligibleCourses = CSE_IT,
        ),
    )
}
