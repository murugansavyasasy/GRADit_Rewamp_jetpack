package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report

import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff.StaffDayStatus
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff.staffDayStatus
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.stableUnit
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.Division
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.Faculty
import java.util.Calendar

enum class ReportRange { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, CUSTOM }

enum class Audience { STUDENTS, STAFF }

/** Dates and filters the report is built for. */
data class ReportFilter(
    val range: ReportRange = ReportRange.TODAY,
    /** The picked day when [range] is [ReportRange.CUSTOM]. */
    val customDay: Day? = null,
    val departmentId: String? = null,
    val semester: Int? = null,
    val section: String? = null,
    val audience: Audience = Audience.STUDENTS,
) {
    val hasClassFilters: Boolean get() = departmentId != null || semester != null || section != null
}

/** An inclusive run of days. */
data class DateSpan(val from: Day, val to: Day) {
    val isSingleDay: Boolean get() = from == to

    val days: List<Day>
        get() {
            val result = mutableListOf<Day>()
            var d = from
            while (d <= to) {
                result += d
                d = d.plusDays(1)
            }
            return result
        }

    val workingDays: List<Day> get() = AcademicCalendar.workingDays(from, to)
}

fun spanFor(filter: ReportFilter, today: Day): DateSpan = when (filter.range) {
    ReportRange.TODAY -> DateSpan(today, today)
    ReportRange.YESTERDAY -> today.plusDays(-1).let { DateSpan(it, it) }
    ReportRange.THIS_WEEK -> {
        // Weeks start on Monday.
        val back = (today.dayOfWeek - Calendar.MONDAY + 7) % 7
        DateSpan(today.plusDays(-back), today)
    }
    ReportRange.THIS_MONTH -> DateSpan(today.yearMonth.first, today)
    ReportRange.CUSTOM -> (filter.customDay ?: today).let { DateSpan(it, it) }
}

/** The span to compare against: the day, week or month before. */
fun previousSpan(filter: ReportFilter, span: DateSpan): DateSpan = when (filter.range) {
    // Compare a day with the last working day before it, skipping Sundays and holidays.
    ReportRange.TODAY, ReportRange.YESTERDAY, ReportRange.CUSTOM -> previousWorkingDay(span.from).let { DateSpan(it, it) }
    ReportRange.THIS_WEEK -> DateSpan(span.from.plusDays(-7), span.to.plusDays(-7))
    ReportRange.THIS_MONTH -> {
        val previous = span.from.yearMonth.plus(-1)
        DateSpan(previous.first, Day(previous.year, previous.month, minOf(span.to.day, previous.length)))
    }
}

/** The closest working day before [day] (looks back up to a month, past vacations). */
fun previousWorkingDay(day: Day): Day {
    var d = day.plusDays(-1)
    repeat(MAX_LOOKBACK_DAYS) {
        if (AcademicCalendar.isWorkingDay(d)) return d
        d = d.plusDays(-1)
    }
    return day.plusDays(-1)
}

private const val MAX_LOOKBACK_DAYS = 40

/** How a person's day was marked. */
enum class Mark { PRESENT, LATE, ABSENT, LEAVE, OFF }

val Mark.isPresent: Boolean get() = this == Mark.PRESENT || this == Mark.LATE

/** Present / absent / late-or-leave counts for a group over one or more days. */
data class Totals(val present: Int = 0, val absent: Int = 0, val lateOrLeave: Int = 0, val marked: Int = 0) {
    val percent: Double get() = percentOf(present, marked)

    operator fun plus(other: Totals) = Totals(present + other.present, absent + other.absent, lateOrLeave + other.lateOrLeave, marked + other.marked)

    /** Average per working day, for multi-day spans. */
    fun perDay(days: Int): Totals = if (days <= 1) this else Totals(present / days, absent / days, lateOrLeave / days, marked / days)
}

fun List<Mark>.totals(): Totals = Totals(
    present = count { it.isPresent },
    absent = count { it == Mark.ABSENT },
    lateOrLeave = count { it == Mark.LATE || it == Mark.LEAVE },
    marked = count { it != Mark.OFF },
)

// ---- Students ----

data class ReportStudent(
    val id: String,
    val name: String,
    val initials: String,
    val registerNo: String,
    val classId: String,
    /** Chance of being present on a working day. */
    val rate: Double,
)

/** A subject taught to a class and who teaches it. */
data class ClassSubject(val name: String, val teacher: String, val periodsPerWeek: Int)

data class ReportClass(
    val division: Division,
    val departmentName: String,
    val programme: String,
    val students: List<ReportStudent>,
    val mentor: String?,
    val subjects: List<ClassSubject>,
) {
    val id: String get() = division.id

    /** "CSE · Sem 5 A". */
    val shortName: String get() = "$departmentName · Sem ${division.semester} ${division.section}"
}

/**
 * A student's mark for [day]. Earlier working days come from the student's history; days
 * after [today] and non-working days are [Mark.OFF].
 *
 * TODO: read marks from the attendance API instead of generating them.
 */
fun studentMark(student: ReportStudent, day: Day, today: Day): Mark {
    if (day > today || !AcademicCalendar.isWorkingDay(day)) return Mark.OFF
    val roll = stableUnit("stu", student.id, day.year, day.month, day.day)
    return when {
        roll >= student.rate -> if (stableUnit("lv", student.id, day.month, day.day) < 0.25) Mark.LEAVE else Mark.ABSENT
        stableUnit("lt", student.id, day.month, day.day) < 0.03 -> Mark.LATE
        else -> Mark.PRESENT
    }
}

fun ReportClass.totalsOn(day: Day, today: Day): Totals = students.map { studentMark(it, day, today) }.totals()

/** A period's attendance for the class: the day's figure nudged a little per period. */
fun ReportClass.periodPercent(day: Day, period: Int, today: Day): Double {
    val dayPercent = totalsOn(day, today).percent
    return (dayPercent + (stableUnit("per", id, day.month, day.day, period) - 0.55) * 6).coerceIn(0.0, 100.0)
}

/** Subject in period [period] (1-based) on [day], following the class's weekly plan. */
fun ReportClass.subjectAt(day: Day, period: Int): ClassSubject? =
    subjects.takeIf { it.isNotEmpty() }?.let { it[(day.dayOfWeek + period) % it.size] }

// ---- Staff ----

data class ReportStaffMember(
    val id: String,
    val name: String,
    val initials: String,
    val designation: String,
    val employeeId: String,
    val groupId: String,
    /** Teaching staff come from the faculty directory and share its punch-ins. */
    val faculty: Faculty? = null,
    val rate: Double = 0.95,
)

/** A department's teaching staff, or a non-teaching office. */
data class ReportStaffGroup(
    val id: String,
    val name: String,
    val head: String?,
    val members: List<ReportStaffMember>,
    val teaching: Boolean,
    /** Teaching department this group belongs to, for the department filter. */
    val departmentId: String? = null,
)

fun staffMark(member: ReportStaffMember, day: Day, today: Day): Mark {
    member.faculty?.let { f ->
        return when (staffDayStatus(f, day, today)) {
            StaffDayStatus.PRESENT -> Mark.PRESENT
            StaffDayStatus.LATE -> Mark.LATE
            StaffDayStatus.ABSENT -> Mark.ABSENT
            StaffDayStatus.LEAVE -> Mark.LEAVE
            else -> Mark.OFF
        }
    }
    if (day > today || !AcademicCalendar.isWorkingDay(day)) return Mark.OFF
    val roll = stableUnit("nts", member.id, day.year, day.month, day.day)
    return when {
        roll >= member.rate + 0.03 -> Mark.ABSENT
        roll >= member.rate -> Mark.LEAVE
        roll >= member.rate - 0.12 -> Mark.LATE
        else -> Mark.PRESENT
    }
}

fun ReportStaffGroup.totalsOn(day: Day, today: Day): Totals = members.map { staffMark(it, day, today) }.totals()

// ---- Person detail (student or staff member) ----

/** Days needed in a row, without absence, to lift [present] / [working] to the minimum. */
fun daysToReachMinimum(present: Int, working: Int, minimum: Int = 75): Int {
    val target = minimum / 100.0
    if (working == 0 || present.toDouble() / working >= target) return 0
    return Math.ceil((target * working - present) / (1 - target)).toInt()
}
