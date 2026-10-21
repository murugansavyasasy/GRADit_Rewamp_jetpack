package com.vsca.vsnapvoicecollege.ui.dashboard.resume

import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.Calendar
import java.util.UUID

/**
 * In-memory resumes shared by the builder, the section editor and the preview. The
 * student edits their own; parents only view and download.
 *
 * TODO: replace the seeded sample data with the resume API; college-sourced sections come from student records.
 */
object ResumeRepository {

    /** Sample entry ids; declared before the seeded resumes use it. */
    private var nextId = 0

    private val _resumes = MutableStateFlow(sampleResumes())
    val resumes: StateFlow<List<Resume>> = _resumes.asStateFlow()

    private var hasLoaded = false
    private val _lastSyncedAt = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    /**
     * Whose resume is shown: the parent's children, or the student themself.
     * TODO: take the linked children / the signed-in student from the session.
     */
    fun studentIdsFor(role: Role): List<String> = when (role) {
        Role.PARENT -> listOf(AARAV, DIYA)
        Role.STUDENT -> listOf(AARAV)
        Role.FACULTY, Role.MANAGEMENT -> emptyList()
    }

    /**
     * Fetches the latest resumes. Offline, the last loaded copy stays on screen; with
     * nothing loaded yet this throws so the screen can show "Couldn't load".
     * TODO: call the resume API instead of simulating latency.
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

    fun resume(studentId: String): Resume? = _resumes.value.firstOrNull { it.studentId == studentId }

    fun setTemplate(studentId: String, template: ResumeTemplate) = edit(studentId) { it.copy(template = template) }

    /** Adds an entry to a section the student fills in. TODO: save through the resume API. */
    fun addEntry(studentId: String, section: ResumeSection, title: String, detail: String?) {
        require(!section.fromCollege) { "$section comes from college records" }
        val entry = ResumeEntry(UUID.randomUUID().toString(), title.trim(), detail?.trim()?.takeIf { it.isNotEmpty() })
        edit(studentId) { it.copy(entries = it.entries + (section to it.entries(section) + entry)) }
    }

    fun updateEntry(studentId: String, section: ResumeSection, entryId: String, title: String, detail: String?) {
        require(!section.fromCollege) { "$section comes from college records" }
        edit(studentId) { resume ->
            val updated = resume.entries(section).map {
                if (it.id == entryId) it.copy(title = title.trim(), detail = detail?.trim()?.takeIf(String::isNotEmpty)) else it
            }
            resume.copy(entries = resume.entries + (section to updated))
        }
    }

    fun removeEntry(studentId: String, section: ResumeSection, entryId: String) {
        require(!section.fromCollege) { "$section comes from college records" }
        edit(studentId) { it.copy(entries = it.entries + (section to it.entries(section).filterNot { e -> e.id == entryId })) }
    }

    private fun edit(studentId: String, change: (Resume) -> Resume) {
        _resumes.update { list ->
            list.map { if (it.studentId == studentId) change(it).copy(updatedAt = System.currentTimeMillis()) else it }
        }
    }

    // ---- Sample data ----

    private const val LOAD_DELAY_MS = 700L
    private const val AARAV = "aarav"
    private const val DIYA = "diya"

    private fun todayAt(hour: Int, minute: Int, dayOffset: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun entry(title: String, detail: String? = null, short: String? = null) = ResumeEntry("e${nextId++}", title, detail, short)

    private fun sampleResumes(): List<Resume> = listOf(
        Resume(
            studentId = AARAV, name = "Aarav Sharma", firstName = "Aarav", initials = "AR",
            contact = ResumeContact(
                degree = "B.E. Computer Science",
                college = "University College of Engineering, Chennai",
                collegeShort = "UCE Chennai",
                email = "aarav.sharma@ucechennai.edu.in",
                phone = "+91 98765 43210",
                city = "Chennai",
            ),
            template = ResumeTemplate.CLASSIC,
            entries = mapOf(
                ResumeSection.EDUCATION to listOf(
                    entry("B.E. Computer Science, UCE Chennai", "2023–2027 · CGPA 8.4", short = "B.E. CSE · CGPA 8.4"),
                    entry("Class XII, Kendriya Vidyalaya, Chennai", "2023 · 94.2%", short = "Class XII 94.2%"),
                ),
                ResumeSection.SKILLS to listOf("Python", "SQL", "React", "Cloud Computing", "Data Structures", "Java").map { entry(it) },
                ResumeSection.PROJECTS to listOf(
                    entry("Smart attendance kiosk", "Python, OpenCV · Best project, Techfest ’26"),
                    entry("Campus lost-and-found app", "React Native · 600+ users"),
                ),
                ResumeSection.CERTIFICATIONS to listOf(
                    "AWS Cloud Practitioner", "NPTEL Data Structures (Elite)", "Coursera SQL for Data Science",
                ).map { entry(it) },
                ResumeSection.ACHIEVEMENTS to listOf(
                    entry("Finalist, Smart India Hackathon internal round 2025"),
                    entry("Class representative, CSE 2024–25"),
                ),
            ),
            updatedAt = todayAt(9, 42),
        ),
        Resume(
            studentId = DIYA, name = "Diya Sharma", firstName = "Diya", initials = "DR",
            contact = ResumeContact(
                degree = "B.Tech Information Technology",
                college = "University College of Engineering, Chennai",
                collegeShort = "UCE Chennai",
                email = "diya.sharma@ucechennai.edu.in",
                phone = "+91 98765 43211",
                city = "Chennai",
            ),
            template = ResumeTemplate.MODERN,
            entries = mapOf(
                ResumeSection.EDUCATION to listOf(
                    entry("B.Tech Information Technology, UCE Chennai", "2024–2028 · CGPA 8.9", short = "B.Tech IT · CGPA 8.9"),
                    entry("Class XII, Kendriya Vidyalaya, Chennai", "2024 · 95.6%", short = "Class XII 95.6%"),
                ),
                ResumeSection.SKILLS to listOf("Java", "HTML", "CSS").map { entry(it) },
                ResumeSection.CERTIFICATIONS to listOf(entry("NPTEL Programming in Java")),
            ),
            updatedAt = todayAt(18, 15, dayOffset = -6),
        ),
    )
}
