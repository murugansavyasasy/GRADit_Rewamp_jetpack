package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory source for college text / voice messages, shared by the inbox, search,
 * message, "New message" and composer screens so read / seen state and sent history
 * stay in sync while the app is running.
 *
 * TODO: replace the seeded sample data and local mutations with the messaging API.
 */
object MessageRepository {

    /** Matches the app's applicationId; used to address bundled sample media. */
    private const val PACKAGE = "com.vsca.vsnapvoicecollege"

    private fun rawUri(resId: Int) = "android.resource://$PACKAGE/$resId"

    // ---- Sample people — declared before the seeded data that references them. ----

    private val EXAM_CELL = MessageSender("Controller of Exams", "EC", SenderDepartment.EXAM_CELL)
    private val ACCOUNTS = MessageSender("Accounts Office", "AO", SenderDepartment.OFFICE)
    private val PRINCIPAL = MessageSender("Dr. K. Suresh", "KS", SenderDepartment.PRINCIPAL)
    private val RAVI = MessageSender("Mr. Ravi", "MR", SenderDepartment.CLASS_TEACHER)
    private val PRIYA_NAIR = MessageSender("Ms. Priya Nair", "PN", SenderDepartment.CLASS_TEACHER)

    private val HOLIDAY_CLIP = VoiceClip(rawUri(R.raw.voice_holiday_announcement), 14_000)
    private val LAB_RECORD_CLIP = VoiceClip(rawUri(R.raw.voice_lab_record_reminder), 11_000)

    /** Students linked to the signed-in parent. TODO: load from the session / profile API. */
    val children = listOf(
        LinkedChild("aarav", "Aarav", "AS", "B.E. CSE · Sem 5"),
        LinkedChild("isha", "Isha", "IS", "B.Com · Sem 1"),
    )

    /** The child whose messages a student sees (their own). TODO: take from the session. */
    val studentSelf: LinkedChild get() = children.first()

    private val _inbox = MutableStateFlow(sampleInbox())
    val inbox: StateFlow<List<InboxMessage>> = _inbox.asStateFlow()

    private val _history = MutableStateFlow(sampleHistory())
    val history: StateFlow<List<SentMessage>> = _history.asStateFlow()

    /** Content prepared on "New message" for the composer to pick up. */
    private var pendingDraft: ComposeDraft? = null

    /**
     * Fetches the latest messages. Throws when the network call fails so the inbox can
     * show its error state. TODO: call the messaging API instead of simulating latency.
     */
    suspend fun refreshInbox() {
        delay(600)
    }

    fun markRead(messageId: String) = updateMessage(messageId) { it.copy(isRead = true) }

    fun markUnread(messageId: String) = updateMessage(messageId) { it.copy(isRead = false) }

    fun markAllRead(childId: String) {
        _inbox.update { messages -> messages.map { if (it.childId == childId) it.copy(isRead = true) else it } }
    }

    /** Records the "Got it" confirmation. TODO: report the acknowledgement to the college. */
    fun acknowledge(messageId: String) =
        updateMessage(messageId) { it.copy(isRead = true, acknowledgedAt = System.currentTimeMillis()) }

    private fun updateMessage(messageId: String, transform: (InboxMessage) -> InboxMessage) {
        _inbox.update { messages -> messages.map { if (it.id == messageId) transform(it) else it } }
    }

    // ---- Sending (management & faculty) ----

    /** Name used to sign templates. TODO: take from the session. */
    fun senderName(role: Role): String = when (role) {
        Role.MANAGEMENT -> PRINCIPAL.name
        else -> "Dr. Meena Iyer"
    }

    /** Classes / groups the user can message. TODO: load the user's groups from the API. */
    val recipientGroups = listOf(
        RecipientGroup("parents-cse-5a", "Parents · CSE Sem 5 A", Audience.PARENTS, 62),
        RecipientGroup("students-cse-5a", "Students · CSE Sem 5 A", Audience.STUDENTS, 64),
        RecipientGroup("parents-cse-5b", "Parents · CSE Sem 5 B", Audience.PARENTS, 58),
        RecipientGroup("students-cse-5b", "Students · CSE Sem 5 B", Audience.STUDENTS, 60),
        RecipientGroup("parents-ece-3a", "Parents · ECE Sem 3 A", Audience.PARENTS, 55),
        RecipientGroup("students-ece-3a", "Students · ECE Sem 3 A", Audience.STUDENTS, 57),
        RecipientGroup("staff-all", "All staff", Audience.STAFF, 48),
    )

    /** The group pre-selected in the composer — the user's own class. */
    val defaultRecipientGroup: RecipientGroup get() = recipientGroups.first()

    fun templates(signature: String) = listOf(
        MessageTemplate(
            title = "IA 2 preparation",
            subject = "IA 2 preparation",
            body = "Dear parents,\nIA 2 begins on 12 Oct. Please make sure your ward revises DBMS units 3 and 4. " +
                "The revision schedule is attached.\n— $signature",
        ),
        MessageTemplate(
            title = "Fee reminder",
            subject = "Fee payment reminder",
            body = "Dear parents,\nThis is a reminder that the Term 1 fee is due on 20 Oct. " +
                "Please pay through the app or at the accounts office.\n— $signature",
        ),
        MessageTemplate(
            title = "Parent-teacher meeting",
            subject = "Parent-teacher meeting",
            body = "Dear parents,\nThe parent-teacher meeting is on Saturday at 10 AM in Block B. " +
                "We look forward to seeing you.\n— $signature",
        ),
        MessageTemplate(
            title = "Holiday notice",
            subject = "Holiday notice",
            body = "Dear students and parents,\nThe college will remain closed tomorrow on account of the holiday. " +
                "Classes resume as usual the day after.\n— $signature",
        ),
    )

    fun setPendingDraft(draft: ComposeDraft) {
        pendingDraft = draft
    }

    /** Returns and clears the draft prepared on "New message", if any. */
    fun consumePendingDraft(): ComposeDraft? = pendingDraft.also { pendingDraft = null }

    /**
     * Sends (or schedules) a message and records it in the sender's history.
     * TODO: post to the messaging API with the recipients, flags and schedule.
     */
    fun send(subject: String, body: String, voice: VoiceClip?) {
        val sent = SentMessage(
            id = UUID.randomUUID().toString(),
            type = if (voice != null) MessageType.VOICE else MessageType.TEXT,
            title = subject,
            body = body,
            sentAt = System.currentTimeMillis(),
            voice = voice,
        )
        _history.update { listOf(sent) + it }
    }

    // ---- Sample data ----

    /** Epoch millis for [hour]:[minute], [daysAgo] days before today. */
    private fun at(hour: Int, minute: Int, daysAgo: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sampleInbox(): List<InboxMessage> = listOf(
        InboxMessage(
            id = "aarav-ia2-timetable",
            childId = "aarav",
            sender = EXAM_CELL,
            type = MessageType.TEXT,
            title = "Mid-semester exam timetable",
            summary = "IA 2 runs from 12 to 17 Oct. Download the timetable attached below.",
            body = "Dear students and parents,\n\n" +
                "Internal Assessment 2 will be held from Monday, 12 October to Saturday, 17 October 2026. " +
                "Exams start at 10:00 AM and last 90 minutes. Please reach your exam hall by 9:40 AM with " +
                "your college ID card.\n\n" +
                "Hall tickets can be downloaded from 9 October. The full subject-wise timetable is attached below.\n\n" +
                "— Controller of Examinations",
            sentAt = at(15, 13),
            sentTo = "B.E. CSE, Year 3 · 122 students and parents",
            attachment = FileAttachment("Timetable.pdf", 240 * 1024L, AttachmentType.PDF, rawUri(R.raw.sample_timetable)),
        ),
        InboxMessage(
            id = "aarav-holiday",
            childId = "aarav",
            sender = PRINCIPAL,
            type = MessageType.VOICE,
            title = "Holiday announcement",
            summary = "Voice message",
            body = "",
            sentAt = at(12, 13),
            sentTo = "All students and parents",
            voice = HOLIDAY_CLIP,
        ),
        InboxMessage(
            id = "aarav-fee-extended",
            childId = "aarav",
            sender = ACCOUNTS,
            type = MessageType.TEXT,
            title = "Fee due date extended",
            summary = "The last date for the Term 1 hostel fee is now 20 October.",
            body = "Dear parents,\n\nThe last date for the Term 1 hostel fee is now 20 October 2026. " +
                "You can pay online from the Fee Details section of the app or at the accounts office " +
                "between 10 AM and 4 PM.\n\n— Accounts Office",
            sentAt = at(16, 30, daysAgo = 1),
            sentTo = "Hostel students and parents",
            isRead = true,
        ),
        InboxMessage(
            id = "aarav-ptm",
            childId = "aarav",
            sender = RAVI,
            type = MessageType.TEXT,
            title = "Parent-teacher meeting on Saturday",
            summary = "Meeting on Sat, 10 Oct at 10 AM in Block B, Room 204.",
            body = "Dear parents,\n\nThe parent-teacher meeting for CSE Sem 5 A is on Saturday, 10 October at " +
                "10 AM in Block B, Room 204. We will discuss IA 1 results and attendance.\n\n— Mr. Ravi, Class teacher",
            sentAt = at(11, 0, daysAgo = 3),
            sentTo = "Parents · CSE Sem 5 A",
            isRead = true,
            acknowledgedAt = at(11, 20, daysAgo = 3),
        ),
        InboxMessage(
            id = "aarav-lab-record",
            childId = "aarav",
            sender = RAVI,
            type = MessageType.VOICE,
            title = "Lab record submission reminder",
            summary = "Voice message",
            body = "",
            sentAt = at(9, 30, daysAgo = 5),
            sentTo = "Parents · CSE Sem 5 A",
            voice = LAB_RECORD_CLIP,
            isRead = true,
        ),
        InboxMessage(
            id = "aarav-ia1-results",
            childId = "aarav",
            sender = EXAM_CELL,
            type = MessageType.TEXT,
            title = "IA 1 exam results published",
            summary = "IA 1 marks are now available in the Academics tab.",
            body = "Dear students and parents,\n\nIA 1 marks are now available in the Academics tab. " +
                "Students who missed the exam on medical grounds should contact the exam cell by 5 October.\n\n" +
                "— Controller of Examinations",
            sentAt = at(14, 0, daysAgo = 17),
            sentTo = "B.E. CSE, Year 3 · 122 students and parents",
            isRead = true,
        ),
        InboxMessage(
            id = "isha-holiday",
            childId = "isha",
            sender = PRINCIPAL,
            type = MessageType.VOICE,
            title = "Holiday announcement",
            summary = "Voice message",
            body = "",
            sentAt = at(12, 13),
            sentTo = "All students and parents",
            voice = HOLIDAY_CLIP,
        ),
        InboxMessage(
            id = "isha-orientation",
            childId = "isha",
            sender = PRIYA_NAIR,
            type = MessageType.TEXT,
            title = "B.Com orientation programme",
            summary = "Orientation for first-year B.Com students is on Monday at 9:30 AM.",
            body = "Dear parents,\n\nThe orientation programme for first-year B.Com students is on Monday at " +
                "9:30 AM in the main auditorium. Parents are welcome to attend.\n\n— Ms. Priya Nair, Class teacher",
            sentAt = at(10, 15, daysAgo = 2),
            sentTo = "Parents · B.Com Sem 1",
            isRead = true,
        ),
    )

    private fun sampleHistory(): List<SentMessage> = listOf(
        SentMessage(
            id = "history-midsem",
            type = MessageType.TEXT,
            title = "Mid-semester exam timetable",
            body = "The timetable for mid-semester exams has been released. Please check the notice board " +
                "and the attached PDF.",
            sentAt = at(15, 13, daysAgo = 1),
        ),
        SentMessage(
            id = "history-holiday",
            type = MessageType.VOICE,
            title = "Holiday announcement",
            body = "",
            sentAt = at(12, 13, daysAgo = 3),
            voice = HOLIDAY_CLIP,
        ),
        SentMessage(
            id = "history-staff-meeting",
            type = MessageType.TEXT,
            title = "Staff meeting reminder",
            body = "Staff meeting today at 4 PM in the conference hall.",
            sentAt = at(10, 43, daysAgo = 3),
        ),
    )
}
