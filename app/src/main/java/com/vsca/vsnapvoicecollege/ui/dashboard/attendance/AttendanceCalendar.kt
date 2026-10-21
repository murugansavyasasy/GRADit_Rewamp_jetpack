package com.vsca.vsnapvoicecollege.ui.dashboard.attendance

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A calendar date without a time, e.g. 07 Oct 2026. [month] is 1–12. */
data class Day(val year: Int, val month: Int, val day: Int) : Comparable<Day> {

    private val key: Int get() = year * 10_000 + month * 100 + day

    override fun compareTo(other: Day): Int = key.compareTo(other.key)

    fun toCalendar(): Calendar = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }

    /** Midnight at the start of the day. */
    val millis: Long get() = toCalendar().timeInMillis

    /** [Calendar.SUNDAY] … [Calendar.SATURDAY]. */
    val dayOfWeek: Int get() = toCalendar().get(Calendar.DAY_OF_WEEK)

    val yearMonth: YearMonth get() = YearMonth(year, month)

    fun plusDays(days: Int): Day = of(toCalendar().apply { add(Calendar.DAY_OF_YEAR, days) })

    /** Whole days from this day to [other]; negative when [other] is earlier. */
    fun daysUntil(other: Day): Int = Math.round((other.millis - millis) / DAY_MS.toDouble()).toInt()

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000L

        fun of(calendar: Calendar) = Day(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

        fun of(millis: Long) = of(Calendar.getInstance().apply { timeInMillis = millis })

        fun today() = of(System.currentTimeMillis())
    }
}

/** A month of a year; [month] is 1–12. */
data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {

    override fun compareTo(other: YearMonth): Int = (year * 12 + month).compareTo(other.year * 12 + other.month)

    val first: Day get() = Day(year, month, 1)

    val length: Int get() = first.toCalendar().getActualMaximum(Calendar.DAY_OF_MONTH)

    val days: List<Day> get() = (1..length).map { Day(year, month, it) }

    fun plus(months: Int): YearMonth {
        val index = year * 12 + (month - 1) + months
        return YearMonth(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1)
    }
}

/**
 * The college calendar: Sundays and listed holidays aren't working days, and May is the
 * summer vacation. Saturdays are working days.
 *
 * TODO: load holidays and vacations from the college calendar API.
 */
object AcademicCalendar {

    private val holidays: Map<Pair<Int, Int>, String> = mapOf(
        (1 to 1) to "New Year’s Day",
        (1 to 15) to "Pongal",
        (1 to 16) to "Thiruvalluvar Day",
        (1 to 26) to "Republic Day",
        (4 to 14) to "Tamil New Year",
        (8 to 15) to "Independence Day",
        (9 to 14) to "Vinayaka Chaturthi",
        (10 to 2) to "Gandhi Jayanti",
        (10 to 20) to "Ayudha Pooja",
        (12 to 25) to "Christmas",
    )

    /** The whole of May is the summer vacation. */
    private const val VACATION_MONTH = 5

    /** The odd semester runs July–November; the even one January–April. */
    fun semesterStart(today: Day): Day = if (today.month >= 7) Day(today.year, 7, 1) else Day(today.year, 1, 2)

    fun isSunday(day: Day): Boolean = day.dayOfWeek == Calendar.SUNDAY

    fun holidayName(day: Day): String? = holidays[day.month to day.day]

    fun isVacation(day: Day): Boolean = day.month == VACATION_MONTH

    fun isVacationMonth(month: YearMonth): Boolean = month.month == VACATION_MONTH

    fun isWorkingDay(day: Day): Boolean = !isSunday(day) && holidayName(day) == null && !isVacation(day)

    /** Working days from [from] to [to], both included. */
    fun workingDays(from: Day, to: Day): List<Day> {
        if (to < from) return emptyList()
        val result = mutableListOf<Day>()
        var day = from
        while (day <= to) {
            if (isWorkingDay(day)) result += day
            day = day.plusDays(1)
        }
        return result
    }
}

/**
 * A stable number in 0 until 1 for [parts], so generated sample history looks random but
 * never changes between runs or screens.
 */
fun stableUnit(vararg parts: Any): Double {
    // FNV-1a over the characters, then MurmurHash3's 64-bit finalizer so that neighbouring
    // inputs (consecutive dates) land far apart instead of in a run.
    var h = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
    parts.joinToString("|").forEach { c ->
        h = h xor c.code.toLong()
        h *= 0x100000001b3L
    }
    h = h xor (h ushr 33)
    h *= -0xae502812aa7333L // 0xff51afd7ed558ccd
    h = h xor (h ushr 33)
    h *= -0x3b314601e57a13adL // 0xc4ceb9fe1a85ec53
    h = h xor (h ushr 33)
    return (h ushr 11).toDouble() / (1L shl 53).toDouble()
}

// ---- Labels shared by the attendance screens ----

/** "Wednesday, 07 Oct 2026". */
fun longDate(day: Day): String = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date(day.millis))

/** "Tue, 6 Oct 2026". */
fun mediumDate(day: Day): String = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(day.millis))

/** "Wed, 07 Oct". */
fun shortWeekdayDate(day: Day): String = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(day.millis))

/** "07 Oct". */
fun dayMonth(day: Day): String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(day.millis))

/** "October 2026". */
fun monthYear(month: YearMonth): String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(month.first.millis))

/** "October". */
fun monthName(month: YearMonth): String = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(month.first.millis))

/** "Oct". */
fun monthShort(month: YearMonth): String = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(month.first.millis))

/** Single-letter / short weekday headers starting on [firstDay] (Calendar.SUNDAY or MONDAY). */
fun weekdayHeaders(firstDay: Int, pattern: String): List<String> = (0 until 7).map { offset ->
    val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, (firstDay - 1 + offset) % 7 + 1) }
    SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
}

/** "75.0" style one-decimal percent of [part] over [whole]; 0 when there's nothing to divide. */
fun percentOf(part: Int, whole: Int): Double = if (whole <= 0) 0.0 else part * 100.0 / whole

/** File-name-safe "07-Oct-2026" (always English month so file names stay ASCII). */
fun fileDate(day: Day): String = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(Date(day.millis))

/** File-name-safe "Oct-2026". */
fun fileMonth(month: YearMonth): String = SimpleDateFormat("MMM-yyyy", Locale.ENGLISH).format(Date(month.first.millis))
