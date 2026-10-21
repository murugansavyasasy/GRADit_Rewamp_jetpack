package com.vsca.vsnapvoicecollege.ui.dashboard.placement.training

import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TrainingModelsTest {

    /** Mon 14 Sep 2026, [hour]:[minute] plus [days]. */
    private fun at(days: Int, hour: Int = 14, minute: Int = 0): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.SEPTEMBER, 14, hour, minute)
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    private fun training(repeat: TrainingRepeat, until: Long?, joinUrl: String? = null) = PlacementTraining(
        id = "t", title = "Data Structures and Algorithms", description = "",
        mode = PlacementMode.ON_CAMPUS, firstSessionAt = at(0), durationMinutes = 90,
        repeat = repeat, until = until, venue = "Lab 3", joinUrl = joinUrl,
        trainer = TrainingTrainer("Dr. Lakshmi Sundar", "LS", "Associate Professor, CSE"), batches = listOf(2024),
    )

    private fun dayOfWeek(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    @Test
    fun weeklyOnMondayAndWednesdayUntilFriday16Oct() {
        val t = training(TrainingRepeat.Weekly(listOf(Calendar.WEDNESDAY, Calendar.MONDAY)), until = at(32, 0, 0))
        // 14 Sep → 16 Oct: five Mondays and five Wednesdays.
        assertEquals(10, t.sessions.size)
        assertTrue(t.sessions.all { dayOfWeek(it) == Calendar.MONDAY || dayOfWeek(it) == Calendar.WEDNESDAY })
        assertEquals(at(0), t.firstStart)
        assertEquals(at(30), t.lastStart)
        assertEquals(listOf(Calendar.MONDAY, Calendar.WEDNESDAY), (t.repeat as TrainingRepeat.Weekly).sortedDays)
    }

    @Test
    fun weekdaysSkipTheWeekend() {
        val t = training(TrainingRepeat.Weekdays, until = at(11))
        assertEquals(10, t.sessions.size)
        assertFalse(t.sessions.any { dayOfWeek(it) == Calendar.SATURDAY || dayOfWeek(it) == Calendar.SUNDAY })
    }

    @Test
    fun onceHasASingleSession() {
        val t = training(TrainingRepeat.Once, until = null)
        assertEquals(listOf(at(0)), t.sessions)
    }

    @Test
    fun nextSessionProgressAndEnd() {
        val t = training(TrainingRepeat.Weekly(listOf(Calendar.MONDAY, Calendar.WEDNESDAY)), until = at(9))
        // Before the first session.
        assertEquals(at(0), t.nextSession(at(-1)))
        assertFalse(t.hasStarted(at(-1)))
        // During Monday's session it's still the "next" one.
        assertEquals(at(0), t.nextSession(at(0, 15, 0)))
        // After Monday's session ends, Wednesday is next and one session is done.
        assertEquals(at(2), t.nextSession(at(0, 16, 0)))
        assertEquals(1, t.sessionsDone(at(0, 16, 0)))
        assertEquals(3, t.upcomingSessions(at(0, 16, 0)).size)
        // After the last session (Wed 23 Sep) ends, the training has ended.
        assertNull(t.nextSession(at(9, 16, 0)))
        assertTrue(t.hasEnded(at(9, 16, 0)))
        assertEquals(4, t.sessionsDone(at(9, 16, 0)))
    }

    @Test
    fun joinLinkOpensTenMinutesBeforeTheNextSession() {
        val t = training(TrainingRepeat.Weekly(listOf(Calendar.MONDAY)), until = at(7), joinUrl = "https://meet.google.com/x")
        assertFalse(t.isJoinOpen(at(0, 13, 49)))
        assertTrue(t.isJoinOpen(at(0, 13, 50)))
        assertTrue(t.isJoinOpen(at(0, 15, 29)))
        assertFalse(t.isJoinOpen(at(1)))
        assertFalse(training(TrainingRepeat.Once, until = null).isJoinOpen(at(0)))
    }

    @Test
    fun batchAndSearch() {
        val t = training(TrainingRepeat.Once, until = null)
        assertTrue(t.isFor(2024))
        assertFalse(t.isFor(2025))
        assertTrue(t.matches("lakshmi"))
        assertTrue(t.matches("lab 3"))
        assertFalse(t.matches("python"))
    }
}
