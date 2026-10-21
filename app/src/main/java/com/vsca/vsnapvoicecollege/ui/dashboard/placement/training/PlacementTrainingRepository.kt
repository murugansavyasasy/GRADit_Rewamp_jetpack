package com.vsca.vsnapvoicecollege.ui.dashboard.placement.training

import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.Calendar

/**
 * In-memory source of the placement cell's training programmes, shared by the family
 * list, the student list and the training detail screen.
 *
 * TODO: replace the seeded sample data with the placement training API.
 */
object PlacementTrainingRepository {

    // ---- Sample trainers — declared before the seeded trainings use them. ----

    private val LAKSHMI = TrainingTrainer("Dr. Lakshmi Sundar", "LS", "Associate Professor, CSE")
    private val PRIYA = TrainingTrainer("Priya", "P", "Full stack trainer")
    private val PANEL = TrainingTrainer("Placement cell panel", "PC", "Faculty and alumni from partner companies")

    private val _trainings = MutableStateFlow(sampleTrainings())
    val trainings: StateFlow<List<PlacementTraining>> = _trainings.asStateFlow()

    private var hasLoaded = false
    private val _lastSyncedAt = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    /**
     * Fetches the latest trainings. Offline, the last loaded list stays on screen; with
     * nothing loaded yet this throws so the screen can show "Couldn't load".
     * TODO: call the placement training API instead of simulating latency.
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

    fun byId(id: String): PlacementTraining? = _trainings.value.firstOrNull { it.id == id }

    // ---- Sample data ----

    private const val LOAD_DELAY_MS = 700L
    private const val GOOGLE_MEET = "Google Meet"

    /** Epoch millis for [hour]:[minute] on the day [dayOffset] days from today. */
    private fun at(hour: Int, minute: Int, dayOffset: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Weekday of the day [dayOffset] days from today, so the sample schedules line up with "today". */
    private fun weekday(dayOffset: Int): Int =
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }.get(Calendar.DAY_OF_WEEK)

    private fun weekly(vararg dayOffsets: Int) = TrainingRepeat.Weekly(dayOffsets.map(::weekday))

    private fun sampleTrainings(): List<PlacementTraining> = listOf(
        PlacementTraining(
            id = "dsa-coding", title = "Data Structures and Algorithms – Coding Track",
            shortTitle = "Data Structures and Algorithms",
            description = "Problem solving for coding rounds: arrays, trees, graphs and dynamic programming",
            mode = PlacementMode.ON_CAMPUS, firstSessionAt = at(14, 0, -21), durationMinutes = 90,
            repeat = weekly(-2, 0), until = at(0, 0, 9), venue = "Computer centre, Lab 3, Block C",
            trainer = LAKSHMI, batches = listOf(2024),
        ),
        PlacementTraining(
            id = "spoken-english", title = "Spoken English and Communication", shortTitle = "Spoken English",
            description = "Group discussions, self-introduction and email etiquette for interviews",
            mode = PlacementMode.ON_CAMPUS, firstSessionAt = at(15, 5, -12), durationMinutes = 60,
            repeat = weekly(2), until = at(0, 0, 30), venue = "Language lab, Block B",
            trainer = TrainingTrainer("Ms. Kavitha Raman", "KR", "Communication trainer, English department"),
            batches = listOf(2024, 2025),
        ),
        PlacementTraining(
            id = "mock-gd", title = "Mock Group Discussion",
            description = "Timed group discussions on current topics, scored by the placement cell",
            mode = PlacementMode.ON_CAMPUS, firstSessionAt = at(10, 0, 3), durationMinutes = 120,
            repeat = TrainingRepeat.Once, until = null, venue = "Seminar hall, Block B",
            trainer = PANEL, batches = listOf(2024), registration = TrainingRegistration.REGISTERED,
        ),
        PlacementTraining(
            id = "aptitude-bootcamp", title = "Aptitude Bootcamp",
            description = "Quantitative aptitude and logical reasoning for campus drives",
            mode = PlacementMode.VIRTUAL, firstSessionAt = at(15, 5, 5), durationMinutes = 85,
            repeat = weekly(5, 7, 9), until = at(0, 0, 23), venue = GOOGLE_MEET, platform = GOOGLE_MEET,
            joinUrl = "https://meet.google.com/apt-boot-camp",
            trainer = TrainingTrainer("Ravi Shankar", "RS", "Senior Aptitude Trainer, TalentSprint Academy"),
            batches = listOf(2025, 2026),
        ),
        PlacementTraining(
            id = "full-stack", title = "Full Stack Web Development Course",
            description = "HTML, CSS, JavaScript, React and Node.js with a capstone project",
            mode = PlacementMode.VIRTUAL, firstSessionAt = at(16, 40, 12), durationMinutes = 180,
            repeat = weekly(12), until = at(0, 0, 54), venue = GOOGLE_MEET, platform = GOOGLE_MEET,
            joinUrl = "https://meet.google.com/fsd-web-2026",
            trainer = PRIYA, batches = listOf(2025, 2026),
        ),
        PlacementTraining(
            id = "mock-interview", title = "Mock Interview Week",
            description = "One-to-one technical and HR mock interviews with feedback",
            mode = PlacementMode.ON_CAMPUS, firstSessionAt = at(10, 0, 19), durationMinutes = 180,
            repeat = TrainingRepeat.Weekdays, until = at(0, 0, 23), venue = "Placement cell, Block C, 2nd floor",
            trainer = PANEL, batches = listOf(2024, 2025), registration = TrainingRegistration.SLOT_BOOKING,
        ),
        // ---- Ended ----
        PlacementTraining(
            id = "python-problem-solving", title = "Python for Problem Solving",
            description = "Python basics to intermediate problem solving",
            mode = PlacementMode.ON_CAMPUS, firstSessionAt = at(15, 5, -65), durationMinutes = 50,
            repeat = TrainingRepeat.Weekdays, until = at(0, 0, -40), venue = "Computer centre, Lab 2, Block C",
            trainer = TrainingTrainer("Mr. Arul Doss", "AD", "Assistant Professor, IT"), batches = listOf(2024),
        ),
        PlacementTraining(
            id = "resume-workshop", title = "Resume and LinkedIn Workshop",
            description = "Writing a one-page resume and a recruiter-ready LinkedIn profile",
            mode = PlacementMode.VIRTUAL, firstSessionAt = at(11, 0, -95), durationMinutes = 90,
            repeat = TrainingRepeat.Once, until = null, venue = "Zoom", platform = "Zoom",
            trainer = TrainingTrainer("Ms. Deepa Menon", "DM", "Career coach"), batches = listOf(2024, 2025),
        ),
        PlacementTraining(
            id = "full-stack-2025", title = "Full Stack Web Development Course", description = "",
            mode = PlacementMode.VIRTUAL, firstSessionAt = at(16, 40, -357), durationMinutes = 180,
            repeat = weekly(-357), until = at(0, 0, -355), venue = GOOGLE_MEET, platform = GOOGLE_MEET,
            trainer = PRIYA, batches = listOf(2024, 2025, 2026, 2027),
        ),
    )
}
