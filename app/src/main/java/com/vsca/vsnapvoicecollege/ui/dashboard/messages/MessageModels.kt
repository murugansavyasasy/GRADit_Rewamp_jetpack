package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary

enum class MessageType { TEXT, VOICE }

/** Tint shared by a sender's avatar and department badge. */
enum class MessageAccent(val background: Color, val content: Color) {
    BLUE(SelectedRowBackground, BrandBlue),
    GRAY(BadgeNeutralBackground, TextPrimary),
    AMBER(AmberChipBackground, AmberText),
    GREEN(OnboardingGreenDisc, BrandGreen),
}

/** The office / role a college message comes from ("Exam Cell", "Principal", …). */
enum class SenderDepartment(@param:StringRes val label: Int, val accent: MessageAccent) {
    EXAM_CELL(R.string.msg_dept_exam_cell, MessageAccent.BLUE),
    OFFICE(R.string.msg_dept_office, MessageAccent.GRAY),
    PRINCIPAL(R.string.msg_dept_principal, MessageAccent.AMBER),
    CLASS_TEACHER(R.string.msg_dept_class_teacher, MessageAccent.GREEN),
}

data class MessageSender(
    val name: String,
    val initials: String,
    val department: SenderDepartment,
)

/** A recorded voice clip, playable from a content / resource URI. */
data class VoiceClip(
    val uri: String,
    val durationMs: Long,
)

/** A student a parent receives messages about. */
data class LinkedChild(
    val id: String,
    val name: String,
    val initials: String,
    val course: String,
)

/** A message received by a parent / student. Recipients cannot reply. */
data class InboxMessage(
    val id: String,
    val childId: String,
    val sender: MessageSender,
    val type: MessageType,
    val title: String,
    /** One-line preview shown in the list. */
    val summary: String,
    /** Full text shown on the message screen. */
    val body: String,
    val sentAt: Long,
    val sentTo: String,
    val voice: VoiceClip? = null,
    val attachment: FileAttachment? = null,
    val isRead: Boolean = false,
    /** When the user tapped "Got it"; null until then. */
    val acknowledgedAt: Long? = null,
)

enum class InboxFilter(@param:StringRes val label: Int, @param:StringRes val screenTitle: Int) {
    ALL(R.string.msg_filter_all, R.string.msg_title_messages),
    UNREAD(R.string.msg_filter_unread, R.string.msg_title_messages),
    TEXT(R.string.msg_filter_text, R.string.msg_title_text),
    VOICE(R.string.msg_filter_voice, R.string.msg_title_voice),
    ;

    fun matches(message: InboxMessage): Boolean = when (this) {
        ALL -> true
        UNREAD -> !message.isRead
        TEXT -> message.type == MessageType.TEXT
        VOICE -> message.type == MessageType.VOICE
    }
}

/** List section a message falls under, by the day it was sent. */
enum class InboxSection(@param:StringRes val label: Int) {
    TODAY(R.string.msg_section_today),
    YESTERDAY(R.string.msg_section_yesterday),
    EARLIER(R.string.msg_section_earlier),
}

// ---- Sending (management & faculty) ----

/** How a sent message is flagged to recipients. */
enum class SendAs(@param:StringRes val label: Int) {
    MESSAGE(R.string.msg_send_as_message),
    ANNOUNCEMENT(R.string.msg_send_as_announcement),
    URGENT(R.string.msg_send_as_urgent),
}

/** Who a recipient group is made of; drives "62 parents will receive this". */
enum class Audience(
    @param:PluralsRes val receiveCount: Int,
    @param:PluralsRes val sendTo: Int,
) {
    PARENTS(R.plurals.msg_receive_parents, R.plurals.msg_send_to_parents),
    STUDENTS(R.plurals.msg_receive_students, R.plurals.msg_send_to_students),
    STAFF(R.plurals.msg_receive_staff, R.plurals.msg_send_to_staff),
    MIXED(R.plurals.msg_receive_recipients, R.plurals.msg_send_to_recipients),
}

data class RecipientGroup(
    val id: String,
    val label: String,
    val audience: Audience,
    val memberCount: Int,
)

/** A message the user sent earlier; can be re-sent from "From history". */
data class SentMessage(
    val id: String,
    val type: MessageType,
    val title: String,
    val body: String,
    val sentAt: Long,
    val voice: VoiceClip? = null,
)

data class MessageTemplate(
    val title: String,
    val subject: String,
    val body: String,
)

/** Content handed from "New message" to the composer. */
data class ComposeDraft(
    val subject: String = "",
    val body: String = "",
    val voice: VoiceClip? = null,
)
