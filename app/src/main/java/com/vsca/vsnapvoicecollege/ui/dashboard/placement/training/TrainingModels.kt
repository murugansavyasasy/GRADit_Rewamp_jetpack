package com.vsca.vsnapvoicecollege.ui.dashboard.placement.training

import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementMode
import java.util.Calendar

/** How a training's sessions recur after the first one. */
sealed interface TrainingRepeat {
    fun occursOn(dayOfWeek: Int): Boolean

    /** A single session. */
    data object Once : TrainingRepeat {
        override fun occursOn(dayOfWeek: Int) = false
    }

    /** Every day, Monday to Friday. */
    data object Weekdays : TrainingRepeat {
        override fun occursOn(dayOfWeek: Int) = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    }

    /** Every week on [days] ([Calendar.DAY_OF_WEEK] values). */
    data class Weekly(val days: List<Int>) : TrainingRepeat {
        /** Monday first, as the placement cell lists them ("Monday and Wednesday"). */
        val sortedDays: List<Int> get() = days.distinct().sortedBy { (it + 5) % 7 }

        override fun occursOn(dayOfWeek: Int) = dayOfWeek in days
    }
}

/** Where the student stands on a training that needs signing up; shown on the student's list. */
enum class TrainingRegistration { REGISTERED, SLOT_BOOKING }

data class TrainingTrainer(val name: String, val initials: String, val designation: String)

data class PlacementTraining(
    val id: String,
    val title: String,
    /** Used in the "sessions in the next 2 days" reminder. */
    val shortTitle: String = title,
    val description: String,
    val mode: PlacementMode,
    /** Start of the first session; later sessions keep the same time of day. */
    val firstSessionAt: Long,
    val durationMinutes: Int,
    val repeat: TrainingRepeat,
    /** Last day sessions can fall on; ignored for [TrainingRepeat.Once]. */
    val until: Long?,
    /** Venue for in-person training. */
    val venue: String,
    /** Video platform for virtual training, e.g. "Google Meet". */
    val platform: String? = null,
    val joinUrl: String? = null,
    val trainer: TrainingTrainer,
    /** Passing-out years the placement cell planned this training for. */
    val batches: List<Int>,
    val registration: TrainingRegistration? = null,
) {
    /** Start of every session, in order. */
    val sessions: List<Long> = buildSessions()

    private val durationMs: Long get() = durationMinutes * MINUTE_MS

    val firstStart: Long get() = sessions.firstOrNull() ?: firstSessionAt
    val lastStart: Long get() = sessions.lastOrNull() ?: firstSessionAt

    fun endOf(sessionStart: Long): Long = sessionStart + durationMs

    /** The session in progress or the next one to come; null once every session is over. */
    fun nextSession(now: Long = System.currentTimeMillis()): Long? = sessions.firstOrNull { endOf(it) > now }

    fun hasEnded(now: Long = System.currentTimeMillis()): Boolean = nextSession(now) == null

    fun hasStarted(now: Long = System.currentTimeMillis()): Boolean = firstStart <= now

    fun sessionsDone(now: Long = System.currentTimeMillis()): Int = sessions.count { endOf(it) <= now }

    fun upcomingSessions(now: Long = System.currentTimeMillis()): List<Long> = sessions.filter { endOf(it) > now }

    fun isFor(batch: Int): Boolean = batch in batches

    fun matches(needle: String): Boolean = needle.isEmpty() ||
        title.contains(needle, true) || description.contains(needle, true) ||
        trainer.name.contains(needle, true) || venue.contains(needle, true)

    /** The join link opens [JOIN_OPENS_BEFORE_MS] before a session and closes when it ends. */
    fun isJoinOpen(now: Long = System.currentTimeMillis()): Boolean {
        val next = nextSession(now) ?: return false
        return joinUrl != null && now >= next - JOIN_OPENS_BEFORE_MS
    }

    private fun buildSessions(): List<Long> {
        if (repeat == TrainingRepeat.Once || until == null) return listOf(firstSessionAt)
        val day = Calendar.getInstance().apply { timeInMillis = firstSessionAt }
        val lastDay = Calendar.getInstance().apply {
            timeInMillis = until
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis
        val result = mutableListOf<Long>()
        while (day.timeInMillis <= lastDay && result.size < MAX_SESSIONS) {
            if (repeat.occursOn(day.get(Calendar.DAY_OF_WEEK))) result += day.timeInMillis
            day.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    companion object {
        const val JOIN_OPENS_BEFORE_MS = 10 * 60 * 1000L
        private const val MINUTE_MS = 60 * 1000L

        /** Guards against a mistyped end date producing years of sessions. */
        private const val MAX_SESSIONS = 400
    }
}
