package com.vsca.vsnapvoicecollege.ui.dashboard.attendance

import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.DateSpan
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.Mark
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportFilter
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportRange
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.daysToReachMinimum
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.previousSpan
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.previousWorkingDay
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.spanFor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.totals
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff.StaffDayStatus
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff.StaffSummary
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff.TodayGroup
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff.staffDayStatus
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff.todayGroupOf
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceModelsTest {

    private val faculty = FacultyRepository.directory.value.faculty

    @Test
    fun staffTodayMatchesTheDesign() {
        val groups = faculty.groupingBy(::todayGroupOf).eachCount()
        assertEquals(102, faculty.size)
        assertEquals(3, groups[TodayGroup.ABSENT])
        assertEquals(3, groups[TodayGroup.ON_LEAVE])
        assertEquals(2, groups[TodayGroup.NOT_IN])
        assertEquals(7, groups[TodayGroup.LATE])
        // Present on the tile includes the late arrivals.
        assertEquals(94, (groups[TodayGroup.PRESENT] ?: 0) + (groups[TodayGroup.LATE] ?: 0))
        assertEquals(TodayGroup.LATE, todayGroupOf(faculty.first { it.id == "gayathri" }))
        assertEquals(TodayGroup.ABSENT, todayGroupOf(faculty.first { it.id == "priya" }))
    }

    @Test
    fun calendarRules() {
        assertTrue(AcademicCalendar.isSunday(Day(2026, 10, 4)))
        assertFalse(AcademicCalendar.isWorkingDay(Day(2026, 10, 2)))
        assertTrue(AcademicCalendar.isWorkingDay(Day(2026, 10, 3)))
        assertTrue(AcademicCalendar.isVacation(Day(2026, 5, 12)))
        // 1–7 Oct 2026: Thu 1, Sat 3, Mon 5, Tue 6, Wed 7 (2nd is a holiday, 4th a Sunday).
        assertEquals(5, AcademicCalendar.workingDays(Day(2026, 10, 1), Day(2026, 10, 7)).size)
        assertEquals(Day(2026, 7, 1), AcademicCalendar.semesterStart(Day(2026, 10, 7)))
        assertEquals(YearMonth(2027, 1), YearMonth(2026, 12).plus(1))
        assertEquals(YearMonth(2025, 12), YearMonth(2026, 1).plus(-1))
    }

    @Test
    fun pastStaffDaysAreStableAndSkipHolidays() {
        val f = faculty.first { it.id == "meena" }
        val today = Day(2026, 10, 7)
        assertEquals(StaffDayStatus.HOLIDAY, staffDayStatus(f, Day(2026, 10, 2), today))
        assertEquals(StaffDayStatus.SUNDAY, staffDayStatus(f, Day(2026, 10, 4), today))
        assertEquals(StaffDayStatus.UPCOMING, staffDayStatus(f, Day(2026, 10, 8), today))
        val day = Day(2026, 9, 22)
        assertEquals(staffDayStatus(f, day, today), staffDayStatus(f, day, today))
    }

    @Test
    fun stableUnitIsUniformAndDoesNotClusterOnConsecutiveDays() {
        // Same value as the reference implementation (FNV-1a + MurmurHash3 fmix64).
        assertEquals(0.608, stableUnit("staff", "abirami", 2026, 10, 1), 0.0005)
        var low = 0
        var longestRun = 0
        repeat(100) { person ->
            var run = 0
            for (day in 1..28) {
                if (stableUnit("staff", "id$person", 2026, 9, day) < 0.05) {
                    low++
                    run++
                    longestRun = maxOf(longestRun, run)
                } else {
                    run = 0
                }
            }
        }
        assertEquals(0.05, low / 2800.0, 0.015)
        assertTrue("absences shouldn't come in long runs", longestRun <= 3)
    }

    @Test
    fun summaryPercentCountsLateAsPresent() {
        val s = StaffSummary(present = 4, absent = 0, leave = 1, late = 1, workingDays = 5)
        assertEquals(80.0, s.percent, 0.001)
        assertFalse(s.isBelowMinimum)
        assertTrue(StaffSummary(2, 2, 0, 0, 4).isBelowMinimum)
    }

    @Test
    fun levels() {
        assertEquals(AttendanceLevel.LOW, levelOf(74.9))
        assertEquals(AttendanceLevel.WATCH, levelOf(75.0))
        assertEquals(AttendanceLevel.WATCH, levelOf(84.9))
        assertEquals(AttendanceLevel.GOOD, levelOf(85.0))
    }

    @Test
    fun daysNeededToReachTheMinimum() {
        // 60 of 88 = 68.2%: 24 more days in a row gets to 84/112 = 75%.
        assertEquals(24, daysToReachMinimum(60, 88))
        assertEquals(0, daysToReachMinimum(80, 100))
    }

    @Test
    fun reportSpans() {
        val today = Day(2026, 10, 7) // Wednesday
        assertEquals(DateSpan(today, today), spanFor(ReportFilter(), today))
        assertEquals(DateSpan(Day(2026, 10, 5), today), spanFor(ReportFilter(range = ReportRange.THIS_WEEK), today))
        val month = ReportFilter(range = ReportRange.THIS_MONTH)
        assertEquals(DateSpan(Day(2026, 10, 1), today), spanFor(month, today))
        assertEquals(DateSpan(Day(2026, 9, 1), Day(2026, 9, 7)), previousSpan(month, spanFor(month, today)))
        // 21 Oct is compared with 19 Oct: the 20th is Ayudha Pooja.
        val day = ReportFilter()
        assertEquals(DateSpan(Day(2026, 10, 19), Day(2026, 10, 19)), previousSpan(day, spanFor(day, Day(2026, 10, 21))))
        // Monday 5 Oct is compared with Saturday 3 Oct.
        assertEquals(Day(2026, 10, 3), previousWorkingDay(Day(2026, 10, 5)))
    }

    @Test
    fun totalsSeparateAbsentFromLeave() {
        val t = listOf(Mark.PRESENT, Mark.LATE, Mark.ABSENT, Mark.LEAVE, Mark.OFF).totals()
        assertEquals(2, t.present)
        assertEquals(1, t.absent)
        assertEquals(2, t.lateOrLeave)
        assertEquals(4, t.marked)
        assertEquals(50.0, t.percent, 0.001)
    }

    @Test
    fun designedStudentsAndLeavesLineUp() {
        val data = ReportRepository.data()
        val cse5a = data.classById("cse-5-A")
        assertNotNull(cse5a)
        assertEquals("Dr. Meena Krishnan", cse5a!!.mentor)
        // Every sample leave request belongs to a real student of the class.
        LeaveRepository.requests.value.forEach { assertNotNull(it.id, data.student(it.studentId)) }
        assertNotNull(data.student("it-3-B-diya"))
        data.classes.forEach { c ->
            assertEquals(c.id, c.students.size, c.students.map { it.name }.toSet().size)
            assertEquals(c.id, c.subjects.size, c.subjects.map { it.name }.toSet().size)
        }
        assertNotNull(data.groupById("admin")?.members?.firstOrNull { it.employeeId == "EMP-2214" })
    }
}
