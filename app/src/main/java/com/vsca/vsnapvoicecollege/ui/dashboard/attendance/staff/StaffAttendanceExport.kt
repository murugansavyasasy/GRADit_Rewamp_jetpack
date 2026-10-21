package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff

import android.content.Context
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceExport
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceTable
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.YearMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.fileDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.fileMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.longDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthYear
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportFormat
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportedFile
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.Faculty
import java.util.Locale

/** Today's punch-ins or the staff-wise summary, as on screen with the current filters. */
object StaffAttendanceExport {

    /** "Attendance_07-Oct-2026", "Attendance_Oct-2026" or "Attendance_2026". */
    fun baseName(state: StaffAttendanceUiState, tab: StaffTab): String = "Attendance_" + when {
        tab == StaffTab.TODAY -> fileDate(state.today)
        state.month != null -> fileMonth(YearMonth(state.year, state.month))
        else -> state.year.toString()
    }

    fun export(context: Context, state: StaffAttendanceUiState, tab: StaffTab, format: ExportFormat): ExportedFile {
        val directory = state.directory ?: error("not loaded")
        val institution = directory.institution
        fun dept(f: Faculty) = directory.department(f.departmentId)?.name.orEmpty()
        val table = when (tab) {
            StaffTab.TODAY -> AttendanceTable(
                title = context.getString(R.string.att_export_today_title, institution),
                subtitle = longDate(state.today),
                header = listOf(
                    context.getString(R.string.att_col_name), context.getString(R.string.att_col_employee_id),
                    context.getString(R.string.att_col_department), context.getString(R.string.att_col_designation),
                    context.getString(R.string.att_col_status), context.getString(R.string.att_col_in_time),
                ),
                rows = state.todayGroups.flatMap { (group, staff) ->
                    staff.map { f ->
                        listOf(
                            f.name, f.employeeId, dept(f), f.designation,
                            context.getString(todayGroupLabel(group)),
                            f.presence.punchMinute?.let { minuteLabel(context, it) }.orEmpty(),
                        )
                    }
                },
                weights = listOf(2.2f, 1.2f, 1.4f, 1.6f, 1.2f, 0.9f),
            )
            StaffTab.STAFF_WISE -> AttendanceTable(
                title = context.getString(R.string.att_export_staffwise_title, institution),
                subtitle = state.month?.let { monthYear(YearMonth(state.year, it)) } ?: context.getString(R.string.att_all_months_year, state.year),
                header = listOf(
                    context.getString(R.string.att_col_name), context.getString(R.string.att_col_employee_id),
                    context.getString(R.string.att_col_department), context.getString(R.string.att_col_designation),
                    context.getString(R.string.att_col_present), context.getString(R.string.att_col_absent),
                    context.getString(R.string.att_col_leave), context.getString(R.string.att_col_late),
                    context.getString(R.string.att_col_working_days), context.getString(R.string.att_col_percent),
                ),
                rows = state.rows.map { (f, s) ->
                    listOf(
                        f.name, f.employeeId, dept(f), f.designation,
                        s.present.toString(), s.absent.toString(), s.leave.toString(), s.late.toString(), s.workingDays.toString(),
                        String.format(Locale.US, "%.0f%%", s.percent),
                    )
                },
                weights = listOf(2.2f, 1.2f, 1.4f, 1.6f, 0.7f, 0.7f, 0.7f, 0.7f, 0.9f, 0.8f),
            )
        }
        return AttendanceExport.export(context, baseName(state, tab), format, table)
    }

    private fun minuteLabel(context: Context, minute: Int): String {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, minute / 60)
            set(java.util.Calendar.MINUTE, minute % 60)
        }
        return android.text.format.DateFormat.getTimeFormat(context).format(cal.time)
    }
}

fun todayGroupLabel(group: TodayGroup): Int = when (group) {
    TodayGroup.ABSENT -> R.string.att_status_absent
    TodayGroup.NOT_IN -> R.string.att_status_not_in
    TodayGroup.ON_LEAVE -> R.string.att_status_on_leave
    TodayGroup.LATE -> R.string.att_status_late
    TodayGroup.PRESENT -> R.string.att_status_present
}
