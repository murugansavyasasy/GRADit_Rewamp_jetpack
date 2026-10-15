package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary

enum class NoticeCategory(@param:StringRes val label: Int, val background: Color, val content: Color) {
    GENERAL(R.string.notice_cat_general, BadgeNeutralBackground, TextPrimary),
    ACADEMIC(R.string.notice_cat_academic, Color(0xFFEDE9FE), Color(0xFF5B34B8)),
    EXAM(R.string.notice_cat_exam, AmberChipBackground, OnboardingAmberCard),
    FEES(R.string.notice_cat_fees, Color(0xFFE0F2F1), Color(0xFF0F6E66)),
    HOLIDAY(R.string.notice_cat_holiday, SelectedRowBackground, BrandBlue),
    EVENT(R.string.notice_cat_event, OnboardingGreenDisc, BrandGreen),
    URGENT(R.string.notice_cat_urgent, AlertBackground, BadgeRed),
}

enum class NoticePriority(@param:StringRes val label: Int) {
    NORMAL(R.string.notice_priority_normal),
    URGENT(R.string.notice_priority_urgent),
}

enum class NoticeStatus(@param:StringRes val label: Int, @param:StringRes val tab: Int, val background: Color, val content: Color) {
    PUBLISHED(R.string.notice_status_published, R.string.notice_tab_published, OnboardingGreenDisc, BrandGreen),
    SCHEDULED(R.string.notice_status_scheduled, R.string.notice_tab_scheduled, AmberChipBackground, OnboardingAmberCard),
    DRAFT(R.string.notice_status_draft, R.string.notice_tab_drafts, BadgeNeutralBackground, TextPrimary),
    EXPIRED(R.string.notice_status_expired, R.string.notice_tab_expired, BadgeNeutralBackground, TextSecondary),
}

enum class NoticeRole(@param:StringRes val label: Int) {
    STUDENTS(R.string.notice_role_students),
    PARENTS(R.string.notice_role_parents),
    STAFF(R.string.notice_role_staff),
}

/**
 * A class (or larger group) a notice can be sent to, with how many people of each
 * role it has. [group] + [section] make labels like "B.E. CSE Sem 5 A, B".
 */
data class NoticeClass(
    val id: String,
    val group: String,
    val section: String?,
    val students: Int,
    val parents: Int,
    val staff: Int,
    /** True when the signed-in family belongs to this class (or it is everyone). */
    val includesMyClass: Boolean,
) {
    val label: String get() = listOfNotNull(group, section).joinToString(" ")

    fun count(role: NoticeRole): Int = when (role) {
        NoticeRole.STUDENTS -> students
        NoticeRole.PARENTS -> parents
        NoticeRole.STAFF -> staff
    }
}

data class NoticeAuthor(val name: String, val initials: String, val title: String)

data class Notice(
    val id: String,
    val title: String,
    /** Lightweight markup: **bold** and "• " bullet lines. */
    val body: String,
    val category: NoticeCategory,
    val priority: NoticePriority,
    val pinned: Boolean,
    val author: NoticeAuthor,
    val classes: List<NoticeClass>,
    val roles: Set<NoticeRole>,
    val isDraft: Boolean,
    val createdAt: Long,
    /** When it went (or goes) live; null for drafts that were never published. */
    val publishedAt: Long?,
    /** Set while the notice is scheduled for later. */
    val scheduledAt: Long? = null,
    /** End of visibility; null = no expiry. */
    val expiresAt: Long? = null,
    val smsEnabled: Boolean = true,
    val attachments: List<FileAttachment> = emptyList(),
    val readCount: Int = 0,
    /** Whether the signed-in parent / student has read it. */
    val isReadByMe: Boolean = false,
) {
    fun status(now: Long = System.currentTimeMillis()): NoticeStatus = when {
        isDraft -> NoticeStatus.DRAFT
        scheduledAt != null && scheduledAt > now -> NoticeStatus.SCHEDULED
        expiresAt != null && expiresAt < now -> NoticeStatus.EXPIRED
        else -> NoticeStatus.PUBLISHED
    }

    /** When receivers see it: the schedule, the publish time, or (for drafts) the last edit. */
    val effectiveTime: Long get() = scheduledAt ?: publishedAt ?: createdAt

    val recipientCount: Int get() = recipientCount(classes, roles)

    /** Pinned notices stay at the top only while they're live. */
    fun isPinnedNow(now: Long = System.currentTimeMillis()): Boolean = pinned && status(now) == NoticeStatus.PUBLISHED

    /**
     * Whether a parent / student of the signed-in family receives this notice. Expired
     * notices stay visible for reference; drafts and scheduled ones don't.
     */
    fun isVisibleTo(role: Role, now: Long = System.currentTimeMillis()): Boolean {
        val status = status(now)
        if (status == NoticeStatus.DRAFT || status == NoticeStatus.SCHEDULED) return false
        val roleNeeded = when (role) {
            Role.PARENT -> NoticeRole.PARENTS
            Role.STUDENT -> NoticeRole.STUDENTS
            else -> return false
        }
        return roleNeeded in roles && classes.any { it.includesMyClass }
    }
}

fun recipientCount(classes: List<NoticeClass>, roles: Set<NoticeRole>): Int =
    classes.sumOf { group -> roles.sumOf { group.count(it) } }

/** "B.E. CSE Sem 5 A, B" — sections of the same group are merged. */
fun classesLabel(classes: List<NoticeClass>): String =
    classes.groupBy { it.group }.entries.joinToString(" · ") { (group, members) ->
        val sections = members.mapNotNull { it.section }
        if (sections.isEmpty()) group else "$group ${sections.joinToString(", ")}"
    }

/** Problems that stop a notice from being published, in the order they're shown. */
enum class NoticeIssue(@param:StringRes val label: Int) {
    TITLE_MISSING(R.string.notice_issue_title),
    MESSAGE_MISSING(R.string.notice_issue_message),
    NO_AUDIENCE(R.string.notice_issue_audience),
    SCHEDULE_IN_PAST(R.string.notice_issue_schedule_past),
    EXPIRY_BEFORE_PUBLISH(R.string.notice_issue_expiry),
}

fun validateNotice(
    title: String,
    body: String,
    attachmentCount: Int,
    classes: List<NoticeClass>,
    roles: Set<NoticeRole>,
    scheduledAt: Long?,
    expiresAt: Long?,
    now: Long = System.currentTimeMillis(),
): List<NoticeIssue> = buildList {
    if (title.isBlank()) add(NoticeIssue.TITLE_MISSING)
    if (body.isBlank() && attachmentCount == 0) add(NoticeIssue.MESSAGE_MISSING)
    if (classes.isEmpty() || roles.isEmpty()) add(NoticeIssue.NO_AUDIENCE)
    if (scheduledAt != null && scheduledAt <= now) add(NoticeIssue.SCHEDULE_IN_PAST)
    if (expiresAt != null && expiresAt <= (scheduledAt ?: now)) add(NoticeIssue.EXPIRY_BEFORE_PUBLISH)
}

enum class NoticeSort(@param:StringRes val label: Int) {
    NEWEST(R.string.notice_sort_newest),
    OLDEST(R.string.notice_sort_oldest),
    MOST_READ(R.string.notice_sort_most_read),
    TITLE(R.string.notice_sort_title),
    ;

    fun apply(notices: List<Notice>): List<Notice> = when (this) {
        NEWEST -> notices.sortedByDescending { it.effectiveTime }
        OLDEST -> notices.sortedBy { it.effectiveTime }
        MOST_READ -> notices.sortedByDescending { if (it.recipientCount > 0) it.readCount.toFloat() / it.recipientCount else 0f }
        TITLE -> notices.sortedBy { it.title.lowercase() }
    }
}
