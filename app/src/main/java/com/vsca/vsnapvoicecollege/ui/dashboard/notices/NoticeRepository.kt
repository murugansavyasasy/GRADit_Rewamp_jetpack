package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.Calendar
import java.util.UUID

/**
 * In-memory source of notices for the staff board, the family board and the compose /
 * detail screens, so publishing, pinning, expiring and read state stay in sync.
 *
 * TODO: replace the seeded sample data and the local mutations with the notices API.
 */
object NoticeRepository {

    /** Matches the app's applicationId; used to address bundled sample attachments. */
    private const val PACKAGE = "com.vsca.vsnapvoicecollege"
    private fun raw(resId: Int) = "android.resource://$PACKAGE/$resId"

    /** College name shown on the family board. TODO: take from the session. */
    const val COLLEGE = "UCE Chennai"

    // ---- People and classes — declared before the seeded notices that use them. ----

    private val PRINCIPAL = NoticeAuthor("Dr. Suresh Kumar", "SK", "Principal")
    private val TEACHER = NoticeAuthor("Dr. Meena Krishnan", "MK", "CSE")
    private val HOD = NoticeAuthor("Dr. Prakash V.", "PV", "HoD · CSE")
    private val PLACEMENT = NoticeAuthor("Ms. Kavitha R.", "KR", "Placement officer")
    private val ACCOUNTS = NoticeAuthor("Mr. Ramesh S.", "RS", "Accounts office")

    private val CSE5_A = NoticeClass("cse5a", "B.E. CSE Sem 5", "A", students = 50, parents = 49, staff = 6, includesMyClass = true)
    private val CSE5_B = NoticeClass("cse5b", "B.E. CSE Sem 5", "B", students = 50, parents = 49, staff = 6, includesMyClass = false)
    private val CSE3_A = NoticeClass("cse3a", "B.E. CSE Sem 3", "A", students = 55, parents = 52, staff = 5, includesMyClass = false)
    private val IT5_A = NoticeClass("it5a", "B.Tech IT Sem 5", "A", students = 48, parents = 46, staff = 5, includesMyClass = false)
    private val FINAL_YEAR = NoticeClass("final", "Final year (all)", null, students = 420, parents = 401, staff = 30, includesMyClass = false)
    private val COLLEGE_ALL = NoticeClass("all", "Whole college", null, students = 2010, parents = 1870, staff = 186, includesMyClass = true)

    /** Classes the user can address. TODO: load the signed-in staff member's classes. */
    val classes = listOf(CSE5_A, CSE5_B, CSE3_A, IT5_A, FINAL_YEAR, COLLEGE_ALL)

    /** The signed-in staff member as an author. TODO: take name and title from the session. */
    fun currentAuthor(role: Role): NoticeAuthor = if (role == Role.FACULTY) TEACHER else PRINCIPAL

    private val _notices = MutableStateFlow(sampleNotices())
    val notices: StateFlow<List<Notice>> = _notices.asStateFlow()

    private var hasLoaded = false
    private val _lastSyncedAt = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    /**
     * Fetches the latest notices. Offline, the last loaded list stays on screen; with
     * nothing loaded yet this throws so the screen can show "Couldn't load".
     * TODO: call the notices API instead of simulating latency.
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

    fun byId(id: String): Notice? = _notices.value.firstOrNull { it.id == id }

    /** Adds or replaces a notice. TODO: send to the notices API (publish / schedule / draft). */
    fun save(notice: Notice) {
        _notices.update { list ->
            if (list.any { it.id == notice.id }) list.map { if (it.id == notice.id) notice else it } else listOf(notice) + list
        }
    }

    fun delete(id: String) = _notices.update { list -> list.filterNot { it.id == id } }

    fun setPinned(id: String, pinned: Boolean) = update(id) { it.copy(pinned = pinned) }

    /** Ends visibility now; the notice stays in the Expired tab. */
    fun expireNow(id: String) = update(id) { it.copy(expiresAt = System.currentTimeMillis() - 1) }

    /** Publishes a scheduled notice straight away. */
    fun publishNow(id: String) = update(id) { it.copy(scheduledAt = null, publishedAt = System.currentTimeMillis()) }

    /** Opening a notice marks it read for this user and counts towards the read report. */
    fun markRead(id: String) = update(id) { if (it.isReadByMe) it else it.copy(isReadByMe = true, readCount = it.readCount + 1) }

    fun markUnread(id: String) = update(id) { it.copy(isReadByMe = false) }

    /** Re-notifies people who haven't read it; returns how many. TODO: trigger push / SMS via the API. */
    fun resend(id: String): Int = byId(id)?.let { (it.recipientCount - it.readCount).coerceAtLeast(0) } ?: 0

    private fun update(id: String, transform: (Notice) -> Notice) {
        _notices.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    fun newId(): String = UUID.randomUUID().toString()

    // ---- Sample data ----

    private const val LOAD_DELAY_MS = 700L
    private const val KB = 1024L

    /** Epoch millis for [hour]:[minute] on the day [dayOffset] days from today. */
    private fun at(hour: Int, minute: Int, dayOffset: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sampleNotices(): List<Notice> {
        val family = setOf(NoticeRole.STUDENTS, NoticeRole.PARENTS)
        return listOf(
            Notice(
                id = "model-exams", title = "Mid-semester model exams – timetable",
                body = "Model exams for B.E. CSE Sem 5 (Sections A and B) start on Monday, 26 Oct 2026.\n" +
                    "**Students must have at least 75% attendance to sit the model exams.**\n" +
                    "• Exams run from 10:00 AM to 01:00 PM.\n• Carry your hall ticket and ID card.\n• No electronic devices in the exam hall.",
                category = NoticeCategory.EXAM, priority = NoticePriority.NORMAL, pinned = true, author = PRINCIPAL,
                classes = listOf(CSE5_A, CSE5_B), roles = family, isDraft = false,
                createdAt = at(14, 0), publishedAt = at(14, 30), expiresAt = at(23, 59, 19),
                attachments = listOf(
                    FileAttachment("Model_exam_timetable.pdf", 420 * KB, AttachmentType.PDF, raw(R.raw.sample_model_exam_timetable)),
                    FileAttachment("Model_exam_seating.pdf", 180 * KB, AttachmentType.PDF, raw(R.raw.sample_model_exam_seating)),
                ),
                readCount = 0,
            ),
            Notice(
                id = "ia2-hall-tickets", title = "Internal Assessment 2 – hall tickets and timetable",
                body = "Internal Assessment 2 for all B.E. CSE Sem 5 students will be held from Mon, 12 Oct to Fri, 16 Oct 2026.\n" +
                    "**Hall tickets are mandatory. Students without a hall ticket will not be allowed into the exam hall.**\n" +
                    "• Download your hall ticket from the student portal before Sat, 10 Oct.\n" +
                    "• Report to your exam hall 15 minutes before the start time.\n" +
                    "• Bring your college ID card and a blue or black pen.\n" +
                    "The timetable and seating plan are attached.",
                category = NoticeCategory.EXAM, priority = NoticePriority.NORMAL, pinned = true, author = PRINCIPAL,
                classes = listOf(CSE5_A, CSE5_B), roles = family, isDraft = false,
                createdAt = at(8, 40), publishedAt = at(9, 0), expiresAt = at(23, 59, 9),
                attachments = listOf(
                    FileAttachment("IA2_timetable_Sem5.pdf", 640 * KB, AttachmentType.PDF, raw(R.raw.sample_ia2_timetable)),
                    FileAttachment("Seating_plan_B-204.jpg", 2150 * KB, AttachmentType.IMAGE, raw(R.raw.sample_seating_plan)),
                ),
                readCount = 150,
            ),
            Notice(
                id = "college-closed", title = "College closed on 8 Oct due to heavy rain",
                body = "Following the IMD red alert for Chennai, the college will remain closed on Thursday, 8 Oct 2026.\n" +
                    "**Online classes will run as per the regular timetable.**\n" +
                    "College buses will not operate. Hostel students should stay inside the campus.",
                category = NoticeCategory.HOLIDAY, priority = NoticePriority.URGENT, pinned = true, author = PRINCIPAL,
                classes = listOf(COLLEGE_ALL), roles = NoticeRole.entries.toSet(), isDraft = false,
                createdAt = at(7, 20), publishedAt = at(7, 30), expiresAt = at(23, 59, 1),
                readCount = 3120,
            ),
            Notice(
                id = "zoho-drive", title = "Placement drive: Zoho registration closes 10 Oct",
                body = "Final-year and pre-final-year CSE and IT students can register for the Zoho campus drive on the placement portal.\n" +
                    "• Eligibility: no standing arrears, 7.0 CGPA and above.\n• Online test on 14 Oct, interviews on 15 Oct.",
                category = NoticeCategory.EVENT, priority = NoticePriority.NORMAL, pinned = false, author = PLACEMENT,
                classes = listOf(FINAL_YEAR, CSE5_A, CSE5_B, IT5_A), roles = setOf(NoticeRole.STUDENTS, NoticeRole.PARENTS),
                isDraft = false, createdAt = at(8, 0), publishedAt = at(8, 15), expiresAt = at(23, 59, 3),
                readCount = 612, isReadByMe = true,
            ),
            Notice(
                id = "term2-fees", title = "Term 2 fee payment reminder",
                body = "The last date to pay the Term 2 tuition fee is 20 Oct 2026. Pay online through the Fee Details section or at the accounts office between 10 AM and 4 PM.",
                category = NoticeCategory.FEES, priority = NoticePriority.NORMAL, pinned = false, author = ACCOUNTS,
                classes = listOf(COLLEGE_ALL), roles = setOf(NoticeRole.PARENTS), isDraft = false,
                createdAt = at(16, 0, -1), publishedAt = at(16, 10, -1), expiresAt = at(23, 59, 5),
                readCount = 1440, isReadByMe = true,
            ),
            Notice(
                id = "ptm", title = "Parent–teacher meeting on Saturday",
                body = "The parent–teacher meeting for B.E. CSE Sem 5 is on Saturday at 10 AM in Block B. We will discuss IA 2 results and attendance.",
                category = NoticeCategory.GENERAL, priority = NoticePriority.NORMAL, pinned = false, author = PRINCIPAL,
                classes = listOf(CSE5_A, CSE5_B), roles = setOf(NoticeRole.PARENTS), isDraft = false,
                createdAt = at(11, 0), publishedAt = null, scheduledAt = at(9, 0, 2), expiresAt = at(23, 59, 4),
            ),
            Notice(
                id = "library-timings", title = "Library timings during model exams",
                body = "The central library will stay open from 8 AM to 9 PM during the model exams.",
                category = NoticeCategory.ACADEMIC, priority = NoticePriority.NORMAL, pinned = false, author = PRINCIPAL,
                classes = emptyList(), roles = setOf(NoticeRole.STUDENTS), isDraft = true,
                createdAt = at(12, 5), publishedAt = null,
            ),
            Notice(
                id = "techfest", title = "Techfest ’26 – call for papers",
                body = "Submit abstracts for the national-level technical symposium Techfest ’26 by Monday, 5 Oct 2026.\n" +
                    "• Teams of up to 3 students\n• Best paper in each track wins ₹5,000",
                category = NoticeCategory.EVENT, priority = NoticePriority.NORMAL, pinned = false, author = HOD,
                classes = listOf(CSE5_A, CSE5_B, CSE3_A), roles = setOf(NoticeRole.STUDENTS, NoticeRole.PARENTS), isDraft = false,
                createdAt = at(14, 30, -17), publishedAt = at(15, 0, -17), expiresAt = at(23, 59, -10),
                attachments = listOf(
                    FileAttachment("Techfest26_brochure.pdf", 1434 * KB, AttachmentType.PDF, raw(R.raw.sample_techfest_brochure)),
                ),
                readCount = 241, isReadByMe = true,
            ),
        )
    }
}
