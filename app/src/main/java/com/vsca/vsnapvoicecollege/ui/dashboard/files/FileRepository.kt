package com.vsca.vsnapvoicecollege.ui.dashboard.files

import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.UUID

/**
 * In-memory source of shared files and folders for every Files screen, so publishing,
 * moving, renaming and deleting stay in sync while the app is running.
 *
 * TODO: replace the seeded sample data and the local mutations with the files API.
 */
object FileRepository {

    /** Matches the app's applicationId; used to address bundled sample files. */
    private const val PACKAGE = "com.vsca.vsnapvoicecollege"

    private fun raw(resId: Int) = "android.resource://$PACKAGE/$resId"

    // ---- Audience groups — declared before the seeded files that reference them. ----

    private val CSE5_A = AudienceGroup("cse5a", "B.E. CSE · Sem 5 · Division A", students = 60, parents = 57, staff = 6, includesMyClass = true)
    private val CSE5_AB = AudienceGroup("cse5ab", "Sem 5 · Division A, B", students = 120, parents = 112, staff = 9, includesMyClass = true)
    private val SEM3_ALL = AudienceGroup("sem3", "Sem 3 · All divisions", students = 280, parents = 262, staff = 30, includesMyClass = false)
    private val HOSTEL = AudienceGroup("hostel", "Hostel residents", students = 410, parents = 398, staff = 12, includesMyClass = true)
    private val EVERYONE = AudienceGroup("all", "All", students = 2010, parents = 1870, staff = 186, includesMyClass = true)
    private val STAFF_ONLY = AudienceGroup("staff", "All staff", students = 0, parents = 0, staff = 186, includesMyClass = false)

    /** Groups offered when uploading. TODO: load the signed-in user's classes. */
    val audienceGroups = listOf(CSE5_A, CSE5_AB, SEM3_ALL, HOSTEL, EVERYONE, STAFF_ONLY)

    private val _folders = MutableStateFlow(
        listOf(
            FileFolder("exams", "Exams"),
            FileFolder("cse5", "CSE · Sem 5"),
            FileFolder("labs", "Labs"),
            FileFolder("events", "Events 2026"),
            FileFolder("hostel", "Hostel"),
            FileFolder("forms", "Forms"),
            FileFolder("syllabus", "Syllabus"),
            FileFolder("staff", "Staff resources"),
        ),
    )
    val folders: StateFlow<List<FileFolder>> = _folders.asStateFlow()

    private val _files = MutableStateFlow(sampleFiles())
    val files: StateFlow<List<SharedFile>> = _files.asStateFlow()

    /** When the file list last loaded successfully (shown on the offline banner). */
    private val _lastSyncedAt = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    /** The child a parent sees files for. TODO: take from the selected child in the session. */
    const val CHILD_NAME = "Aarav R."

    /**
     * Fetches the latest file list. Keeps the cached list when offline so the screens can
     * show the offline banner instead of an error. TODO: call the files API.
     */
    suspend fun refresh() {
        delay(LOAD_DELAY_MS)
        if (NetworkMonitor.isOnline.value) _lastSyncedAt.value = System.currentTimeMillis()
    }

    fun fileById(id: String): SharedFile? = _files.value.firstOrNull { it.id == id }

    fun folderName(id: String?): String? = _folders.value.firstOrNull { it.id == id }?.name

    /** Who appears as the sender of files the signed-in staff member publishes. TODO: take from the session. */
    fun senderName(role: Role): String = if (role == Role.MANAGEMENT) "Principal’s office" else "Dr. Meena Krishnan"

    fun senderInitials(role: Role): String = if (role == Role.MANAGEMENT) "PO" else "MK"

    // ---- Folders ----

    fun createFolder(name: String): FileFolder {
        val folder = FileFolder(UUID.randomUUID().toString(), name.trim())
        _folders.update { it + folder }
        return folder
    }

    fun renameFolder(id: String, name: String) {
        _folders.update { folders -> folders.map { if (it.id == id) it.copy(name = name.trim()) else it } }
    }

    /** Deletes a folder; its files stay published and move to "Unsorted". */
    fun deleteFolder(id: String) {
        _folders.update { folders -> folders.filterNot { it.id == id } }
        _files.update { files -> files.map { if (it.folderId == id) it.copy(folderId = null) else it } }
    }

    // ---- Files ----

    fun moveFile(fileId: String, folderId: String?) = updateFile(fileId) { it.copy(folderId = folderId) }

    fun deleteFile(fileId: String) {
        _files.update { files -> files.filterNot { it.id == fileId } }
    }

    fun updateDetails(fileId: String, title: String, description: String, category: FileCategory, downloadAllowed: Boolean) =
        updateFile(fileId) {
            it.copy(title = title.trim(), description = description.trim(), category = category, downloadAllowed = downloadAllowed)
        }

    /** Swaps the file's content, keeping its title, audience and settings. */
    fun replaceContent(fileId: String, fileName: String, format: FileFormat, sizeBytes: Long, uri: String) =
        updateFile(fileId) {
            it.copy(fileName = fileName, format = format, sizeBytes = sizeBytes, uri = uri, pageCount = null, durationMs = null)
        }

    fun markSeen(fileId: String) = updateFile(fileId) { it.copy(isNew = false) }

    /** Publishes (or schedules) one shared file per uploaded file. TODO: send to the files API. */
    fun publish(files: List<SharedFile>) {
        _files.update { files + it }
    }

    private fun updateFile(fileId: String, transform: (SharedFile) -> SharedFile) {
        _files.update { files -> files.map { if (it.id == fileId) transform(it) else it } }
    }

    // ---- Upload draft ----

    /** The unsent upload form, restored the next time "Upload files" opens. */
    var draft: UploadDraft? = null

    // ---- Lesson videos (Video quick action for students and parents) ----

    val lessonVideos: List<LessonVideo> = listOf(
        LessonVideo("v-normalisation", "Normalisation in 20 minutes", "DBMS", "Dr. Meena Iyer", (21 * 60 + 40) * 1000L, at(10, 0, -10)),
        LessonVideo("v-cpu", "CPU scheduling walkthrough", "Operating Systems", "Prof. K. Rao", (34 * 60 + 12) * 1000L, at(10, 0, -13)),
        LessonVideo("v-subnet", "Subnetting practice", "Computer Networks", "Dr. S. Nair", (18 * 60 + 5) * 1000L, at(10, 0, -16)),
        LessonVideo("v-annual", "Annual Day 2025 highlights", "Cultural committee", null, (6 * 60 + 30) * 1000L, at(10, 0, -25)),
    )

    // ---- Sample data ----

    private const val LOAD_DELAY_MS = 700L
    private const val KB = 1024L
    private const val MB = 1024L * 1024L

    /** Epoch millis for [hour]:[minute] on the day [dayOffset] days from today. */
    private fun at(hour: Int, minute: Int, dayOffset: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sampleFiles(): List<SharedFile> {
        val principal = "Principal’s office"
        val both = setOf(AudienceRole.STUDENTS, AudienceRole.PARENTS)
        return listOf(
            SharedFile(
                id = "hostel-rules", title = "Hostel rules 2026–27", fileName = "Hostel_rules_2026-27.pdf",
                format = FileFormat.PDF, sizeBytes = (1.2 * MB).toLong(), category = FileCategory.CIRCULAR, folderId = "hostel",
                sender = principal, description = "Updated hostel timings and visitor rules for the new academic year.",
                publishedAt = at(8, 0, 2), scheduledAt = at(8, 0, 2), audience = HOSTEL, roles = both,
                pageCount = 1, uri = raw(R.raw.sample_hostel_rules),
            ),
            SharedFile(
                id = "ia2-timetable", title = "IA 2 timetable and seating – Sem 5 A", fileName = "IA2_timetable_Sem5A.pdf",
                format = FileFormat.PDF, sizeBytes = 640 * KB, category = FileCategory.CIRCULAR, folderId = "exams",
                sender = principal, description = "IA 2 starts Mon 12 Oct. Please check your hall and seat number. Bring your ID card.",
                publishedAt = at(9, 40), expiresAt = at(23, 59, 16), audience = CSE5_A, roles = both, viewedCount = 74,
                pageCount = 3, uri = raw(R.raw.sample_ia2_timetable), isNew = true,
            ),
            SharedFile(
                id = "seating-plan", title = "Seating plan – Hall B-204", fileName = "Seating_plan_B-204.jpg",
                format = FileFormat.JPG, sizeBytes = (2.1 * MB).toLong(), category = FileCategory.CIRCULAR, folderId = "exams",
                sender = principal, description = "Seat numbers are in roll number order, rows A to F.",
                publishedAt = at(9, 40), expiresAt = at(23, 59, 16), audience = CSE5_A, roles = both, viewedCount = 69,
                uri = raw(R.raw.sample_seating_plan), isNew = true,
            ),
            SharedFile(
                id = "exam-rules", title = "Exam rules – IA 2", fileName = "Exam_rules_IA2.docx",
                format = FileFormat.DOCX, sizeBytes = 180 * KB, category = FileCategory.CIRCULAR, folderId = "forms",
                sender = principal, description = "Please read the exam rules before IA 2.",
                publishedAt = at(9, 40), expiresAt = at(23, 59, 16), audience = CSE5_A, roles = both, viewedCount = 51,
                uri = raw(R.raw.sample_exam_rules), isNew = true,
            ),
            SharedFile(
                id = "dbms-unit3", title = "DBMS Unit 3 – Normalisation notes", fileName = "DBMS_Unit3_Normalisation_notes.pdf",
                format = FileFormat.PDF, sizeBytes = (2.4 * MB).toLong(), category = FileCategory.STUDY_MATERIAL, folderId = "cse5",
                sender = "Dr. Meena Krishnan",
                description = "Read pages 1–12 before Thursday’s class. View only: the teacher has turned off downloads.",
                publishedAt = at(11, 30, -9), audience = CSE5_AB, roles = setOf(AudienceRole.STUDENTS), viewedCount = 102,
                downloadAllowed = false, pageCount = 18, uri = raw(R.raw.sample_dbms_notes),
            ),
            SharedFile(
                id = "lab-safety", title = "Lab safety walkthrough", fileName = "Lab_safety_walkthrough.mp4",
                format = FileFormat.MP4, sizeBytes = 86 * MB, category = FileCategory.STUDY_MATERIAL, folderId = "labs",
                sender = principal, description = "Watch before your first lab session.",
                publishedAt = at(10, 0, -10), expiresAt = at(23, 59, 44), expiryIsEndOfSemester = true, audience = SEM3_ALL,
                roles = setOf(AudienceRole.STUDENTS, AudienceRole.STAFF), viewedCount = 241, durationMs = (4 * 60 + 32) * 1000L,
            ),
            SharedFile(
                id = "annual-day", title = "Annual Day rehearsal – Main stage", fileName = "Annual_Day_rehearsal_main_stage.jpg",
                format = FileFormat.JPG, sizeBytes = (3.1 * MB).toLong(), category = FileCategory.GALLERY, folderId = "events",
                sender = "Cultural committee", description = "Photos from the full dress rehearsal.",
                publishedAt = at(16, 15, -10), audience = EVERYONE, roles = both, viewedCount = 2140,
                uri = raw(R.raw.sample_annual_day),
            ),
            SharedFile(
                id = "placement", title = "Placement orientation – Sem 5", fileName = "Placement_orientation_Sem5.pptx",
                format = FileFormat.PPTX, sizeBytes = (8.6 * MB).toLong(), category = FileCategory.STUDY_MATERIAL, folderId = "events",
                sender = "Placement cell",
                description = "Slides from Friday’s orientation: eligibility, companies visiting and the training calendar.",
                publishedAt = at(15, 0, -11), audience = CSE5_AB, roles = both, viewedCount = 188, pageCount = 24,
            ),
            SharedFile(
                id = "principal-address", title = "Principal’s address – Teachers’ Day", fileName = "Principals_address_Teachers_Day.mp4",
                format = FileFormat.MP4, sizeBytes = 164 * MB, category = FileCategory.CIRCULAR, folderId = "events",
                sender = principal, description = "Message from the Principal to students and parents.",
                publishedAt = at(11, 0, -40), audience = EVERYONE, roles = both, viewedCount = 1960,
                durationMs = (12 * 60 + 8) * 1000L,
            ),
            SharedFile(
                id = "sports-day", title = "Sports Day schedule", fileName = "Sports_Day_schedule.pdf",
                format = FileFormat.PDF, sizeBytes = 320 * KB, category = FileCategory.CIRCULAR, folderId = "events",
                sender = "Physical education", description = "Events and timings for Sports Day.",
                publishedAt = at(12, 0, -12), audience = EVERYONE, roles = both, viewedCount = 2480,
                pageCount = 1, uri = raw(R.raw.sample_sports_day),
            ),
            SharedFile(
                id = "bonafide", title = "Bonafide certificate request form", fileName = "Bonafide_request_form.docx",
                format = FileFormat.DOCX, sizeBytes = 96 * KB, category = FileCategory.FORMS, folderId = "forms",
                sender = "Office", description = "Fill in and submit at the office with your ID card.",
                publishedAt = at(10, 30, -20), audience = EVERYONE, roles = setOf(AudienceRole.STUDENTS), viewedCount = 412,
                uri = raw(R.raw.sample_bonafide_form),
            ),
            SharedFile(
                id = "syllabus", title = "B.E. CSE Sem 5 syllabus", fileName = "BE_CSE_Sem5_syllabus.pdf",
                format = FileFormat.PDF, sizeBytes = (1.1 * MB).toLong(), category = FileCategory.SYLLABUS, folderId = "syllabus",
                sender = "Dr. Meena Krishnan", description = "Subjects and units for this semester.",
                publishedAt = at(9, 0, -60), audience = CSE5_AB, roles = both, viewedCount = 214,
                pageCount = 1, uri = raw(R.raw.sample_syllabus),
            ),
            SharedFile(
                id = "staff-minutes", title = "Staff meeting minutes – September", fileName = "Staff_meeting_minutes_Sept.pdf",
                format = FileFormat.PDF, sizeBytes = 410 * KB, category = FileCategory.OTHER, folderId = "staff",
                sender = principal, description = "Minutes of the September staff meeting.",
                publishedAt = at(17, 0, -15), audience = STAFF_ONLY, roles = setOf(AudienceRole.STAFF), viewedCount = 160,
                pageCount = 1, uri = raw(R.raw.sample_staff_minutes),
            ),
        )
    }
}
