package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory source of discussion posts shared by the board, thread and new-post
 * screens, so posts, replies and likes stay in sync while the app is running.
 *
 * TODO: replace the seeded sample data and the local mutations with the discussion API.
 */
object DiscussionRepository {

    /** Subjects the user can post under. TODO: load the signed-in user's subjects. */
    val subjects = listOf(
        "Data Structures",
        "Operating Systems",
        "Database Systems",
        "Computer Networks",
        "Library",
        "General",
    )

    /** The class teacher alerted about new posts. TODO: take from the user's class. */
    const val CLASS_TEACHER = "Dr. Meena Iyer"

    // Sample authors — declared before the seeded posts that reference them.
    private val PRIYA = DiscussionAuthor("Priya R", "PR", DiscussionAuthorRole.STUDENT, AvatarAccent.BLUE)
    private val RAVI = DiscussionAuthor("Mr. Ravi", "MR", DiscussionAuthorRole.TEACHER, AvatarAccent.GREEN)
    private val ARUN = DiscussionAuthor("Arun K", "AK", DiscussionAuthorRole.STUDENT, AvatarAccent.AMBER)
    private val KAVYA = DiscussionAuthor("Kavya S", "KS", DiscussionAuthorRole.STUDENT, AvatarAccent.BLUE)

    private val _posts = MutableStateFlow(samplePosts())
    val posts: StateFlow<List<DiscussionPost>> = _posts.asStateFlow()

    /** The signed-in user as a discussion author. TODO: take name / initials from the session. */
    fun currentUser(role: Role): DiscussionAuthor = when (role) {
        Role.STUDENT -> PRIYA
        Role.PARENT -> DiscussionAuthor("Rajesh Sharma", "RS", DiscussionAuthorRole.PARENT, AvatarAccent.BLUE)
        Role.FACULTY -> RAVI
        Role.MANAGEMENT -> DiscussionAuthor("Dr. K. Suresh", "KS", DiscussionAuthorRole.PRINCIPAL, AvatarAccent.GREEN)
    }

    fun addPost(
        author: DiscussionAuthor,
        subject: String,
        title: String,
        body: String,
        attachment: FileAttachment?,
    ) {
        val post = DiscussionPost(
            id = UUID.randomUUID().toString(),
            author = author,
            subject = subject,
            title = title,
            body = body,
            postedAt = System.currentTimeMillis(),
            isMine = true,
            attachment = attachment,
        )
        _posts.update { listOf(post) + it }
    }

    fun addReply(postId: String, author: DiscussionAuthor, text: String, attachment: FileAttachment?) {
        val reply = DiscussionReply(
            id = UUID.randomUUID().toString(),
            author = author,
            text = text,
            postedAt = System.currentTimeMillis(),
            isMine = true,
            attachment = attachment,
        )
        updatePost(postId) { it.copy(replies = it.replies + reply) }
    }

    fun togglePostLike(postId: String) {
        updatePost(postId) { it.copy(likedByMe = !it.likedByMe, likes = it.likes + if (it.likedByMe) -1 else 1) }
    }

    fun toggleReplyLike(postId: String, replyId: String) {
        updatePost(postId) { post ->
            post.copy(
                replies = post.replies.map { reply ->
                    if (reply.id != replyId) {
                        reply
                    } else {
                        reply.copy(likedByMe = !reply.likedByMe, likes = reply.likes + if (reply.likedByMe) -1 else 1)
                    }
                },
            )
        }
    }

    private fun updatePost(postId: String, transform: (DiscussionPost) -> DiscussionPost) {
        _posts.update { posts -> posts.map { if (it.id == postId) transform(it) else it } }
    }

    /** Epoch millis for [hour]:[minute], [daysAgo] days before today. */
    private fun at(hour: Int, minute: Int, daysAgo: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun samplePosts(): List<DiscussionPost> = listOf(
        DiscussionPost(
            id = "linked-lists",
            author = PRIYA,
            subject = "Data Structures",
            title = "Doubt in Unit 3 – Linked Lists",
            body = "I am confused about inserting a node at the middle of a singly linked list. " +
                "How do we handle the previous pointer?",
            postedAt = at(10, 20),
            likes = 12,
            isMine = true,
            attachment = FileAttachment("Notes.pdf", 420 * 1024L, AttachmentType.PDF),
            replies = listOf(
                DiscussionReply(
                    id = "linked-lists-1",
                    author = RAVI,
                    text = "Traverse to the node before the position, then set new.next = prev.next and " +
                        "prev.next = new. I have shared a diagram in class notes.",
                    postedAt = at(10, 42),
                    likes = 9,
                    isBestAnswer = true,
                ),
                DiscussionReply(
                    id = "linked-lists-2",
                    author = PRIYA,
                    text = "Thank you sir, got it!",
                    postedAt = at(10, 50),
                    likes = 2,
                    isMine = true,
                ),
                DiscussionReply(
                    id = "linked-lists-3",
                    author = ARUN,
                    text = "Same doubt, this helped.",
                    postedAt = at(11, 5),
                ),
            ),
        ),
        DiscussionPost(
            id = "library-timing",
            author = RAVI,
            subject = "Library",
            title = "Library timing during exams",
            body = "The central library will stay open from 8 AM to 9 PM from 12 to 17 Oct. " +
                "Reference books can be borrowed overnight during this period.",
            postedAt = at(9, 5),
            likes = 28,
            replies = listOf(
                DiscussionReply("library-timing-1", ARUN, "Is the reading hall also open till 9 PM?", at(9, 20), likes = 3),
                DiscussionReply("library-timing-2", RAVI, "Yes, the reading hall follows the same timing.", at(9, 32), likes = 6),
                DiscussionReply("library-timing-3", KAVYA, "How many books can we borrow overnight?", at(9, 40), likes = 1),
                DiscussionReply("library-timing-4", RAVI, "Up to three reference books per student.", at(9, 48), likes = 4),
                DiscussionReply("library-timing-5", KAVYA, "Thank you sir!", at(9, 55)),
            ),
        ),
        DiscussionPost(
            id = "hackathon-team",
            author = ARUN,
            subject = "General",
            title = "Hackathon team formation",
            body = "Looking for two teammates for the Smart India Hackathon internal round. " +
                "Can a teacher confirm if inter-department teams are allowed?",
            postedAt = at(18, 40, daysAgo = 1),
            likes = 4,
        ),
    )
}
