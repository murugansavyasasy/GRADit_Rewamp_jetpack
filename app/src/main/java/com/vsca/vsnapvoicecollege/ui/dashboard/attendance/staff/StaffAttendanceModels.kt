package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff

import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.YearMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.stableUnit
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.Faculty
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.PresenceStatus

/** What one staff member's day looked like. */
enum class StaffDayStatus {
    PRESENT,

    /** Present, but punched in after [StaffRules.LATE_AFTER_MINUTE]. */
    LATE,
    ABSENT,
    LEAVE,

    /** Today, before the cut-off, with no punch yet. */
    NOT_IN,
    HOLIDAY,
    SUNDAY,
    VACATION,
    UPCOMING,
    ;

    val isPresent: Boolean get() = this == PRESENT || this == LATE

    /** Counts towards working days (and so towards the attendance %). */
    val isCounted: Boolean get() = this == PRESENT || this == LATE || this == ABSENT || this == LEAVE
}

/** Today's status from the punch-in device, mapped onto the day statuses. */
enum class TodayGroup { ABSENT, NOT_IN, ON_LEAVE, LATE, PRESENT }

object StaffRules {
    /** Punch-ins after 09:00 AM are late. */
    const val LATE_AFTER_MINUTE = 9 * 60

    /** Staff not punched in by 09:15 AM are marked absent. */
    const val ABSENT_CUTOFF_MINUTE = 9 * 60 + 15

    /** Below this attendance % a staff member is flagged. */
    const val MINIMUM_PERCENT = 75
}

/** Counts for a staff member over a month or a year. */
data class StaffSummary(
    val present: Int,
    val absent: Int,
    val leave: Int,
    val late: Int,
    val workingDays: Int,
) {
    /** Present days (late included) over working days. */
    val percent: Double get() = percentOf(present, workingDays)

    val isBelowMinimum: Boolean get() = workingDays > 0 && percent < StaffRules.MINIMUM_PERCENT

    operator fun plus(other: StaffSummary) = StaffSummary(
        present + other.present, absent + other.absent, leave + other.leave, late + other.late, workingDays + other.workingDays,
    )

    companion object {
        val EMPTY = StaffSummary(0, 0, 0, 0, 0)
    }
}

fun todayGroupOf(f: Faculty): TodayGroup = when (f.presence.status) {
    PresenceStatus.ABSENT -> TodayGroup.ABSENT
    PresenceStatus.NOT_IN -> TodayGroup.NOT_IN
    PresenceStatus.ON_LEAVE -> TodayGroup.ON_LEAVE
    PresenceStatus.PRESENT ->
        if ((f.presence.punchMinute ?: 0) > StaffRules.LATE_AFTER_MINUTE) TodayGroup.LATE else TodayGroup.PRESENT
}

/**
 * A staff member's status on [day]. Today comes from the punch-in device; earlier working
 * days come from the attendance history.
 *
 * TODO: read past days from the staff attendance API instead of generating them.
 */
fun staffDayStatus(f: Faculty, day: Day, today: Day): StaffDayStatus = when {
    day > today -> StaffDayStatus.UPCOMING
    AcademicCalendar.isVacation(day) -> StaffDayStatus.VACATION
    AcademicCalendar.isSunday(day) -> StaffDayStatus.SUNDAY
    AcademicCalendar.holidayName(day) != null -> StaffDayStatus.HOLIDAY
    day == today -> when (todayGroupOf(f)) {
        TodayGroup.ABSENT -> StaffDayStatus.ABSENT
        TodayGroup.NOT_IN -> StaffDayStatus.NOT_IN
        TodayGroup.ON_LEAVE -> StaffDayStatus.LEAVE
        TodayGroup.LATE -> StaffDayStatus.LATE
        TodayGroup.PRESENT -> StaffDayStatus.PRESENT
    }
    else -> {
        val roll = stableUnit("staff", f.id, day.year, day.month, day.day)
        when {
            roll < 0.012 -> StaffDayStatus.ABSENT
            roll < 0.035 -> StaffDayStatus.LEAVE
            roll < 0.085 -> StaffDayStatus.LATE
            else -> StaffDayStatus.PRESENT
        }
    }
}

/** Counts for [days]; days that don't count (Sundays, holidays, not yet in) are skipped. */
fun staffSummary(f: Faculty, days: List<Day>, today: Day): StaffSummary {
    var present = 0
    var absent = 0
    var leave = 0
    var late = 0
    var working = 0
    days.forEach { day ->
        val status = staffDayStatus(f, day, today)
        if (status.isCounted) working++
        when (status) {
            StaffDayStatus.PRESENT -> present++
            StaffDayStatus.LATE -> { present++; late++ }
            StaffDayStatus.ABSENT -> absent++
            StaffDayStatus.LEAVE -> leave++
            else -> Unit
        }
    }
    return StaffSummary(present, absent, leave, late, working)
}

/** The days of [month] up to today, or of the whole year up to today when [month] is null. */
fun staffPeriodDays(year: Int, month: Int?, today: Day): List<Day> {
    val months = if (month != null) listOf(YearMonth(year, month)) else (1..12).map { YearMonth(year, it) }
    return months.flatMap { it.days }.filter { it <= today }
}
