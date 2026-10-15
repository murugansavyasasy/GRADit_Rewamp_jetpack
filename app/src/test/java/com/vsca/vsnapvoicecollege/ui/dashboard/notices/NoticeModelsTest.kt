package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeModelsTest {

    private val notices = NoticeRepository.notices.value
    private val now = System.currentTimeMillis()
    private val sem5 = NoticeRepository.classes.filter { it.group == "B.E. CSE Sem 5" }

    @Test
    fun staffBoardShowsTheDesignedStatusCounts() {
        assertEquals(8, notices.size)
        assertEquals(5, notices.count { it.status(now) == NoticeStatus.PUBLISHED })
        assertEquals(1, notices.count { it.status(now) == NoticeStatus.SCHEDULED })
        assertEquals(1, notices.count { it.status(now) == NoticeStatus.DRAFT })
        assertEquals(1, notices.count { it.status(now) == NoticeStatus.EXPIRED })
    }

    @Test
    fun familiesSeeTheirClassNoticesWithThreeUnread() {
        val parent = notices.filter { it.isVisibleTo(Role.PARENT, now) }
        val student = notices.filter { it.isVisibleTo(Role.STUDENT, now) }
        assertEquals(3, parent.count { !it.isReadByMe })
        assertEquals(3, student.count { !it.isReadByMe })
        // Drafts and scheduled notices never reach families; expired ones stay for reference.
        assertFalse(parent.any { it.id == "ptm" || it.id == "library-timings" })
        assertTrue(parent.any { it.id == "techfest" })
        // The fee reminder is for parents only.
        assertTrue(parent.any { it.id == "term2-fees" })
        assertFalse(student.any { it.id == "term2-fees" })
    }

    @Test
    fun recipientsAddUpPerRole_andClassesMergeIntoOneLabel() {
        assertEquals(198, recipientCount(sem5, setOf(NoticeRole.STUDENTS, NoticeRole.PARENTS)))
        assertEquals(210, recipientCount(sem5, NoticeRole.entries.toSet()))
        assertEquals("B.E. CSE Sem 5 A, B", classesLabel(sem5))
        val mixed = NoticeRepository.classes.filter { it.id == "cse5a" || it.id == "all" }
        assertEquals("B.E. CSE Sem 5 A · Whole college", classesLabel(mixed))
    }

    @Test
    fun validationListsEveryProblem_inDesignOrder() {
        val issues = validateNotice(
            title = " ",
            body = "Exams start Monday",
            attachmentCount = 0,
            classes = emptyList(),
            roles = setOf(NoticeRole.STUDENTS),
            scheduledAt = null,
            expiresAt = now - 60_000,
            now = now,
        )
        assertEquals(listOf(NoticeIssue.TITLE_MISSING, NoticeIssue.NO_AUDIENCE, NoticeIssue.EXPIRY_BEFORE_PUBLISH), issues)
    }

    @Test
    fun validationChecksScheduleAndEmptyContent() {
        val issues = validateNotice("Title", "", 0, sem5, setOf(NoticeRole.PARENTS), scheduledAt = now - 1, expiresAt = null, now = now)
        assertEquals(listOf(NoticeIssue.MESSAGE_MISSING, NoticeIssue.SCHEDULE_IN_PAST), issues)
        // An attachment alone is enough content, and expiry must come after a future schedule.
        val ok = validateNotice("Title", "", 1, sem5, setOf(NoticeRole.PARENTS), scheduledAt = now + 3_600_000, expiresAt = now + 7_200_000, now = now)
        assertTrue(ok.isEmpty())
        val early = validateNotice("Title", "Body", 0, sem5, setOf(NoticeRole.PARENTS), scheduledAt = now + 7_200_000, expiresAt = now + 3_600_000, now = now)
        assertEquals(listOf(NoticeIssue.EXPIRY_BEFORE_PUBLISH), early)
    }

    @Test
    fun onlyLiveNoticesArePinned() {
        val pinnedIds = notices.filter { it.isPinnedNow(now) }.map { it.id }.toSet()
        assertEquals(setOf("model-exams", "ia2-hall-tickets", "college-closed"), pinnedIds)
        val expiredPinned = notices.first { it.id == "techfest" }.copy(pinned = true)
        assertFalse(expiredPinned.isPinnedNow(now))
    }

    @Test
    fun readingUpdatesTheReport_andMarkUnreadKeepsTheCount() {
        val before = NoticeRepository.byId("college-closed")!!.readCount
        NoticeRepository.markRead("college-closed")
        NoticeRepository.markRead("college-closed")
        val read = NoticeRepository.byId("college-closed")!!
        assertTrue(read.isReadByMe)
        assertEquals(before + 1, read.readCount)
        NoticeRepository.markUnread("college-closed")
        assertFalse(NoticeRepository.byId("college-closed")!!.isReadByMe)
        assertEquals(before + 1, NoticeRepository.byId("college-closed")!!.readCount)
    }

    @Test
    fun resendCountsPeopleWhoHaveNotRead() {
        val ia2 = NoticeRepository.byId("ia2-hall-tickets")!!
        assertEquals(ia2.recipientCount - ia2.readCount, NoticeRepository.resend(ia2.id))
        assertEquals(48, NoticeRepository.resend(ia2.id))
    }

    @Test
    fun sortsByReadShareAndTitle() {
        val share = { n: Notice -> if (n.recipientCount > 0) n.readCount.toFloat() / n.recipientCount else 0f }
        val mostRead = NoticeSort.MOST_READ.apply(notices.filter { it.status(now) == NoticeStatus.PUBLISHED })
        assertTrue(mostRead.zipWithNext().all { (a, b) -> share(a) >= share(b) })
        assertEquals("model-exams", mostRead.last().id)
        val titles = NoticeSort.TITLE.apply(notices).map { it.title.lowercase() }
        assertEquals(titles.sorted(), titles)
    }
}
