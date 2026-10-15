package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageModelsTest {

    private val sender = MessageSender("Controller of Exams", "EC", SenderDepartment.EXAM_CELL)

    private fun message(type: MessageType, isRead: Boolean) = InboxMessage(
        id = "$type-$isRead",
        childId = "aarav",
        sender = sender,
        type = type,
        title = "Title",
        summary = "Summary",
        body = "Body",
        sentAt = 0L,
        sentTo = "CSE",
        isRead = isRead,
    )

    @Test
    fun inboxFilters_matchByReadStateAndType() {
        val unreadVoice = message(MessageType.VOICE, isRead = false)
        val readText = message(MessageType.TEXT, isRead = true)

        assertTrue(InboxFilter.ALL.matches(unreadVoice) && InboxFilter.ALL.matches(readText))
        assertTrue(InboxFilter.UNREAD.matches(unreadVoice))
        assertFalse(InboxFilter.UNREAD.matches(readText))
        assertTrue(InboxFilter.VOICE.matches(unreadVoice))
        assertFalse(InboxFilter.VOICE.matches(readText))
        assertTrue(InboxFilter.TEXT.matches(readText))
    }

    @Test
    fun composer_countsRecipientsAndNamesTheAudience() {
        val parentsA = RecipientGroup("a", "Parents · A", Audience.PARENTS, 62)
        val parentsB = RecipientGroup("b", "Parents · B", Audience.PARENTS, 58)
        val students = RecipientGroup("c", "Students · A", Audience.STUDENTS, 64)

        val parentsOnly = ComposeMessageUiState(recipients = listOf(parentsA, parentsB))
        assertEquals(120, parentsOnly.recipientCount)
        assertEquals(Audience.PARENTS, parentsOnly.audience)

        val mixed = ComposeMessageUiState(recipients = listOf(parentsA, students))
        assertEquals(Audience.MIXED, mixed.audience)
    }

    @Test
    fun composer_needsRecipientsSubjectAndContentToSend() {
        val group = RecipientGroup("a", "Parents · A", Audience.PARENTS, 62)
        val ready = ComposeMessageUiState(recipients = listOf(group), subject = "IA 2", message = TextFieldValue("Hello"))
        assertTrue(ready.canSend)
        assertFalse(ready.copy(recipients = emptyList()).canSend)
        assertFalse(ready.copy(subject = " ").canSend)
        assertFalse(ready.copy(message = TextFieldValue("")).canSend)
        assertTrue(ready.copy(message = TextFieldValue(""), voice = VoiceClip("uri", 1_000)).canSend)
    }

    @Test
    fun composer_suggestsMatchingGroupsNotAlreadyAdded() {
        val added = MessageRepository.recipientGroups.first()
        val state = ComposeMessageUiState(recipients = listOf(added), recipientQuery = "cse")
        assertFalse(added in state.suggestions)
        assertTrue(state.suggestions.isNotEmpty())
        assertTrue(state.suggestions.all { it.label.contains("CSE") })
    }

    @Test
    fun clipTime_formatsMinutesAndSeconds() {
        assertEquals("0:12", formatClipTime(12_400))
        assertEquals("1:05", formatClipTime(65_000))
        assertEquals("00:12", formatClipTimePadded(12_000))
    }
}
