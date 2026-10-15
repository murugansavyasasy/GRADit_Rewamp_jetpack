package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground

/**
 * The role badge shown next to a discussion author. Staff roles (teacher / principal)
 * are the ones whose reply answers a question.
 */
enum class DiscussionAuthorRole(@param:StringRes val label: Int, val isStaff: Boolean) {
    STUDENT(R.string.discussion_role_student, isStaff = false),
    PARENT(R.string.discussion_role_parent, isStaff = false),
    TEACHER(R.string.discussion_role_teacher, isStaff = true),
    PRINCIPAL(R.string.discussion_role_principal, isStaff = true),
    ;

    companion object {
        fun from(role: Role): DiscussionAuthorRole = when (role) {
            Role.STUDENT -> STUDENT
            Role.PARENT -> PARENT
            Role.FACULTY -> TEACHER
            Role.MANAGEMENT -> PRINCIPAL
        }
    }
}

/** Avatar tint per author. */
enum class AvatarAccent(val background: Color, val content: Color) {
    BLUE(SelectedRowBackground, BrandBlue),
    GREEN(OnboardingGreenDisc, BrandGreen),
    AMBER(AmberChipBackground, OnboardingAmberCard),
}

data class DiscussionAuthor(
    val name: String,
    val initials: String,
    val role: DiscussionAuthorRole,
    val accent: AvatarAccent,
)

data class DiscussionReply(
    val id: String,
    val author: DiscussionAuthor,
    val text: String,
    val postedAt: Long,
    val likes: Int = 0,
    val likedByMe: Boolean = false,
    val isMine: Boolean = false,
    val isBestAnswer: Boolean = false,
    val attachment: FileAttachment? = null,
)

data class DiscussionPost(
    val id: String,
    val author: DiscussionAuthor,
    val subject: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    val likes: Int = 0,
    val likedByMe: Boolean = false,
    val isMine: Boolean = false,
    val attachment: FileAttachment? = null,
    val replies: List<DiscussionReply> = emptyList(),
) {
    /** The reply that answers the question: the best answer, else the first staff reply. */
    private val answeringReply: DiscussionReply?
        get() = replies.firstOrNull { it.isBestAnswer } ?: replies.firstOrNull { it.author.role.isStaff }

    /** A post is answered once staff reply to it; posts written by staff count as answered. */
    val isAnswered: Boolean
        get() = author.role.isStaff || answeringReply != null

    /** Who answered, when it was someone other than the author (drives "Answered by …"). */
    val answeredBy: String?
        get() = answeringReply?.author?.name?.takeIf { it != author.name }
}

enum class DiscussionFilter(@param:StringRes val label: Int) {
    ALL(R.string.discussion_filter_all),
    MINE(R.string.discussion_filter_mine),
    UNANSWERED(R.string.discussion_filter_unanswered),
    ;

    fun matches(post: DiscussionPost): Boolean = when (this) {
        ALL -> true
        MINE -> post.isMine
        UNANSWERED -> !post.isAnswered
    }
}
