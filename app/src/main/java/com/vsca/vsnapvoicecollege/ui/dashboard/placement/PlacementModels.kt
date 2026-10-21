package com.vsca.vsnapvoicecollege.ui.dashboard.placement

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import java.util.Calendar

enum class PlacementEventType(@param:StringRes val label: Int) {
    PRE_PLACEMENT_TALK(R.string.placement_type_ppt),
    CAMPUS_DRIVE(R.string.placement_type_campus_drive),
    PLACEMENT_EVENT(R.string.placement_type_event),
    JOB_FAIR(R.string.placement_type_job_fair),
    ONLINE_TEST(R.string.placement_type_online_test),
}

/** Where the event happens; picks the pill colours and the venue icon. */
enum class PlacementMode(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val background: Color,
    val content: Color,
) {
    VIRTUAL(R.string.placement_mode_virtual, R.drawable.ic_videocam_outline, OnboardingGreenDisc, BrandGreen),
    ON_CAMPUS(R.string.placement_mode_on_campus, R.drawable.ic_apartment, Color(0xFFDDE4FB), BrandBlue),
    OFF_CAMPUS(R.string.placement_mode_off_campus, R.drawable.ic_place_outline, AmberChipBackground, OnboardingAmberCard),
    ;

    /** In-person events get "Open in Maps"; virtual ones a join link. */
    val isInPerson: Boolean get() = this != VIRTUAL
}

/** Light blue tint behind the event-type pill. */
val PlacementTypeBackground = SelectedRowBackground

data class PlacementCompany(val name: String, val initials: String)

/** A child of the signed-in parent (or the student themself), matched against eligible courses. */
data class PlacementChild(val id: String, val firstName: String, val initials: String, val course: String)

data class PlacementEvent(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: PlacementEventType,
    val mode: PlacementMode,
    val startAt: Long,
    /** Null when the placement cell only gave a start time. */
    val endAt: Long?,
    /** Venue, or "Online · video meeting" for virtual events. */
    val venue: String,
    val companies: List<PlacementCompany>,
    val eligibleCourses: List<String>,
    /** Further criteria from the placement cell (CGPA, arrears, batch …). */
    val criteria: List<String> = emptyList(),
    /** Role and package, shown on the student list, e.g. "Software developer · ₹8.4 LPA". */
    val offer: String? = null,
    val joinUrl: String? = null,
) {
    /** Ends at [endAt], or at the end of the start day when no end time was given. */
    val effectiveEnd: Long get() = endAt ?: endOfDay(startAt)

    fun hasEnded(now: Long = System.currentTimeMillis()): Boolean = effectiveEnd < now

    fun isEligible(course: String): Boolean = eligibleCourses.any { it.equals(course, ignoreCase = true) }

    fun matches(needle: String): Boolean = needle.isEmpty() ||
        title.contains(needle, true) || subtitle.contains(needle, true) ||
        companies.any { it.name.contains(needle, true) } ||
        eligibleCourses.any { it.contains(needle, true) }

    /** The join link opens [JOIN_OPENS_BEFORE_MS] before the start and closes when the event ends. */
    fun isJoinOpen(now: Long = System.currentTimeMillis()): Boolean =
        joinUrl != null && now >= startAt - JOIN_OPENS_BEFORE_MS && now <= effectiveEnd

    companion object {
        const val JOIN_OPENS_BEFORE_MS = 10 * 60 * 1000L
    }
}

/** Whole calendar days from today to [millis]; negative in the past. */
fun daysFromToday(millis: Long, now: Long = System.currentTimeMillis()): Int {
    fun dayStart(at: Long) = Calendar.getInstance().apply {
        timeInMillis = at
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return Math.round((dayStart(millis) - dayStart(now)) / DAY_MS.toDouble()).toInt()
}

private fun endOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
}.timeInMillis

private const val DAY_MS = 24 * 60 * 60 * 1000L
