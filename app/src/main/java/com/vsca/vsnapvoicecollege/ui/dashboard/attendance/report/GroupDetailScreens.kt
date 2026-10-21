package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceExport
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceFooter
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceLevel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceTable
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.ListCard
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.YearMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.fileDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelBackground
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelColor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.mediumDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthYear
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.weekdayHeaders
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportFormat
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyExport
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.InitialsAvatar
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.PeriodSlot
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.timeOfDay
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberRing
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.util.Calendar
import java.util.Locale

/** One class: today, the semester, a month heat map, who marked each period and every student. */
@Composable
fun ClassAttendanceScreen(classId: String, onBack: () -> Unit, onOpenStudent: (String) -> Unit, modifier: Modifier = Modifier) {
    val data = remember { ReportRepository.data() }
    val c = data.classById(classId)
    val today = Day.today()
    val context = LocalContext.current
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = c?.shortName ?: stringResource(R.string.att_report_title),
            subtitle = c?.let { cls -> cls.mentor?.let { stringResource(R.string.att_students_mentor, cls.students.size, it) } ?: stringResource(R.string.att_students_n, cls.students.size) },
            onBack = onBack,
            trailing = { if (c != null) SquareIconButton(R.drawable.ic_download, stringResource(R.string.att_export), { exportClass(context, c, today) }, tint = BrandBlue) },
        )
        if (c == null) {
            Text(stringResource(R.string.att_not_found), Modifier.padding(32.dp), color = TextSecondary)
            return@Column
        }
        val rows = remember(c) {
            val semester = AcademicCalendar.workingDays(AcademicCalendar.semesterStart(today), today)
            c.students.map { s -> s to semester.map { studentMark(s, it, today) }.totals().percent }.sortedBy { it.second }
        }
        val todayTotals = c.totalsOn(today, today)
        val semesterTotals = AcademicCalendar.workingDays(AcademicCalendar.semesterStart(today), today).fold(Totals()) { acc, d -> acc + c.totalsOn(d, today) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StatRow(
                stringResource(R.string.att_today) to (if (todayTotals.marked > 0) percentLabel(todayTotals.percent, 1) else "–"),
                stringResource(R.string.att_this_sem) to percentLabel(semesterTotals.percent, 1),
                stringResource(R.string.att_status_present) to "${todayTotals.present}/${c.students.size}",
            )
            HeatCalendar(today) { day -> c.totalsOn(day, today).takeIf { it.marked > 0 }?.percent }
            Title(stringResource(R.string.att_marked_today_by))
            ListCard(Modifier.padding(horizontal = 16.dp)) {
                val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
                val marked = if (AcademicCalendar.isWorkingDay(today)) PeriodSlot.entries.take(ReportViewModel.PERIODS_PER_DAY).filter { it.startMinute <= now } else emptyList()
                if (marked.isEmpty()) {
                    Text(stringResource(R.string.att_nothing_marked_yet), Modifier.padding(16.dp), color = TextSecondary, fontSize = AppTextSize.Body)
                }
                marked.forEachIndexed { index, slot ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FieldBorder)
                    val subject = c.subjectAt(today, slot.number)
                    val present = Math.round(c.students.size * c.periodPercent(today, slot.number, today) / 100).toInt()
                    MarkedRow(
                        tag = "P${slot.number}",
                        title = subject?.teacher?.ifBlank { null } ?: stringResource(R.string.att_class_teacher),
                        subtitle = stringResource(R.string.att_subject_present, subject?.name.orEmpty(), present, c.students.size),
                        time = timeOfDay(slot.startMinute + 5 + (slot.number * 7) % 30),
                    )
                }
            }
            Title(stringResource(R.string.att_students_count, c.students.size))
            ListCard(Modifier.padding(horizontal = 16.dp)) {
                rows.forEachIndexed { index, (student, percent) ->
                    if (index > 0) HorizontalDivider(color = FieldBorder)
                    PersonRow(student.initials, student.name, student.registerNo, percent) { onOpenStudent(student.id) }
                }
            }
            AttendanceFooter(Modifier.padding(top = 20.dp), note = stringResource(R.string.att_report_footer))
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A staff group: today, this month, a month heat map, the punch-in devices and every member. */
@Composable
fun StaffGroupScreen(groupId: String, onBack: () -> Unit, onOpenMember: (String) -> Unit, modifier: Modifier = Modifier) {
    val data = remember { ReportRepository.data() }
    val g = data.groupById(groupId)
    val today = Day.today()
    val context = LocalContext.current
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = g?.let { stringResource(R.string.att_group_staff_title, it.name) } ?: stringResource(R.string.att_report_title),
            subtitle = g?.let { grp -> grp.head?.let { stringResource(R.string.att_staff_head, grp.members.size, it) } ?: stringResource(R.string.att_staff_n, grp.members.size) },
            onBack = onBack,
            trailing = { if (g != null) SquareIconButton(R.drawable.ic_download, stringResource(R.string.att_export), { exportGroup(context, g, today) }, tint = BrandBlue) },
        )
        if (g == null) {
            Text(stringResource(R.string.att_not_found), Modifier.padding(32.dp), color = TextSecondary)
            return@Column
        }
        val month = AcademicCalendar.workingDays(today.yearMonth.first, today)
        val rows = remember(g) { g.members.map { m -> m to month.map { staffMark(m, it, today) }.totals().percent }.sortedBy { it.second } }
        val todayTotals = g.totalsOn(today, today)
        val monthTotals = month.fold(Totals()) { acc, d -> acc + g.totalsOn(d, today) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StatRow(
                stringResource(R.string.att_today) to (if (todayTotals.marked > 0) percentLabel(todayTotals.percent, 1) else "–"),
                stringResource(R.string.att_this_month) to percentLabel(monthTotals.percent, 1),
                stringResource(R.string.att_status_present) to "${todayTotals.present}/${g.members.size}",
            )
            HeatCalendar(today) { day -> g.totalsOn(day, today).takeIf { it.marked > 0 }?.percent }
            Title(stringResource(R.string.att_recorded_today_by))
            ListCard(Modifier.padding(horizontal = 16.dp)) {
                val onLeave = g.members.count { staffMark(it, today, today) == Mark.LEAVE }
                val checkedIn = todayTotals.present
                val office = (checkedIn * 0.1).toInt()
                val sources = listOf(
                    Triple(stringResource(R.string.att_src_gate), stringResource(R.string.att_device_main_gate), stringResource(R.string.att_checked_in_of, checkedIn - office, g.members.size)) to timeOfDay(9 * 60 + 30),
                    Triple(stringResource(R.string.att_src_office), stringResource(R.string.att_device_office), stringResource(R.string.att_checked_in, office)) to timeOfDay(9 * 60 + 12),
                    Triple(stringResource(R.string.att_src_leave), stringResource(R.string.att_marked_by_hr), stringResource(R.string.att_on_approved_leave, onLeave)) to timeOfDay(8 * 60 + 40),
                )
                if (!AcademicCalendar.isWorkingDay(today)) {
                    Text(stringResource(R.string.att_nothing_marked_yet), Modifier.padding(16.dp), color = TextSecondary, fontSize = AppTextSize.Body)
                } else {
                    sources.forEachIndexed { index, (labels, time) ->
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FieldBorder)
                        MarkedRow(labels.first, labels.second, labels.third, time)
                    }
                }
            }
            Title(stringResource(R.string.att_staff_count_title, g.members.size))
            ListCard(Modifier.padding(horizontal = 16.dp)) {
                rows.forEachIndexed { index, (member, percent) ->
                    if (index > 0) HorizontalDivider(color = FieldBorder)
                    PersonRow(member.initials, member.name, "${member.designation} · ${member.employeeId}", percent) { onOpenMember(member.id) }
                }
            }
            AttendanceFooter(Modifier.padding(top = 20.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Pieces ----

@Composable
private fun Title(text: String) {
    Text(text, Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
}

@Composable
internal fun StatRow(vararg stats: Pair<String, String>) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEach { (label, value) ->
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(label, color = TextPrimary, fontSize = AppTextSize.Body, maxLines = 1)
                    Text(value, color = TextPrimary, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MarkedRow(tag: String, title: String, subtitle: String, time: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = SelectedRowBackground) {
            Text(tag, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Label, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Text(time, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PersonRow(initials: String, name: String, subtitle: String, percent: Double, onClick: () -> Unit) {
    val level = levelOf(percent)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        InitialsAvatar(initials, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(shape = RoundedCornerShape(50), color = levelBackground(level)) {
            Text(percentLabel(percent, 1), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = levelColor(level), fontSize = AppTextSize.Label, fontWeight = FontWeight.ExtraBold)
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

/** Month heat map, Monday first: each working day shows its %, styled good / watch (striped) / low (dashed). */
@Composable
internal fun HeatCalendar(today: Day, percentOn: (Day) -> Double?) {
    var shown by rememberSaveable(stateSaver = listSaver(save = { listOf(it.year, it.month) }, restore = { YearMonth(it[0], it[1]) })) {
        mutableStateOf(today.yearMonth.let { if (AcademicCalendar.isVacationMonth(it)) it.plus(-1) else it })
    }
    Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthArrow(R.drawable.ic_chevron_left, stringResource(R.string.att_previous_month)) { shown = shown.plus(-1) }
                Text(monthYear(shown), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                MonthArrow(R.drawable.ic_chevron_right, stringResource(R.string.att_next_month), enabled = shown < today.yearMonth) { shown = shown.plus(1) }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                weekdayHeaders(Calendar.MONDAY, "EEEEE").forEach {
                    Text(it, Modifier.weight(1f), color = TextSecondary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(6.dp))
            val lead = (shown.first.dayOfWeek - Calendar.MONDAY + 7) % 7
            val cells: List<Day?> = List(lead) { null } + shown.days
            cells.chunked(7).forEach { week ->
                Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (0 until 7).forEach { i ->
                        Box(Modifier.weight(1f)) { week.getOrNull(i)?.let { day -> HeatCell(day, today, percentOn) } }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.att_heat_legend), color = TextSecondary, fontSize = AppTextSize.Label)
        }
    }
}

@Composable
private fun MonthArrow(icon: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(14.dp), color = ScreenBackground) {
        Icon(painterResource(icon), description, tint = if (enabled) TextPrimary else TextMuted, modifier = Modifier.padding(12.dp).size(20.dp))
    }
}

@Composable
private fun HeatCell(day: Day, today: Day, percentOn: (Day) -> Double?) {
    val shape = RoundedCornerShape(12.dp)
    val percent = if (day <= today && AcademicCalendar.isWorkingDay(day)) percentOn(day) else null
    val holiday = AcademicCalendar.holidayName(day) != null || AcademicCalendar.isVacation(day)
    val base = Modifier.fillMaxWidth().aspectRatio(0.85f)
    when {
        percent != null -> {
            val level = levelOf(percent)
            val color = levelColor(level)
            val cell = when (level) {
                AttendanceLevel.GOOD -> base.clip(shape).background(OnboardingGreenDisc)
                AttendanceLevel.WATCH -> base.clip(shape).background(AmberChipBackground).then(Modifier.stripes()).border(1.5.dp, OnboardingAmberRing, shape)
                AttendanceLevel.LOW -> base.clip(shape).background(AlertBackground).dashed(BadgeRed)
            }
            Column(cell, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(day.day.toString(), color = color, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
                Text(String.format(Locale.getDefault(), "%.0f%%", percent), color = color, fontSize = AppTextSize.Caption, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        holiday && !AcademicCalendar.isSunday(day) -> Column(base.clip(shape).background(SelectedRowBackground), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(day.day.toString(), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.att_short_holiday), color = BrandBlue, fontSize = AppTextSize.Caption, fontWeight = FontWeight.Bold)
        }
        else -> Column(base, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(day.day.toString(), color = if (day > today) TextMuted else TextSecondary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
            if (day <= today) Text("–", color = TextSecondary, fontSize = AppTextSize.Caption)
        }
    }
}

/** Diagonal amber stripes for "watch" days. */
private fun Modifier.stripes(): Modifier = drawBehind {
    val step = 7.dp.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(OnboardingAmberRing.copy(alpha = 0.55f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1.5.dp.toPx())
        x += step
    }
}

/** Dashed rounded outline for "low" days. */
private fun Modifier.dashed(color: Color): Modifier = drawBehind {
    val stroke = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
    drawRoundRect(color, cornerRadius = CornerRadius(12.dp.toPx()), style = stroke)
}

// ---- Quick exports from the detail screens ----

private fun exportClass(context: android.content.Context, c: ReportClass, today: Day) {
    val semester = AcademicCalendar.workingDays(AcademicCalendar.semesterStart(today), today)
    val table = AttendanceTable(
        title = context.getString(R.string.att_export_class_title, c.shortName),
        subtitle = mediumDate(today),
        header = listOf(context.getString(R.string.att_col_name), context.getString(R.string.att_col_register_no), context.getString(R.string.att_col_today), context.getString(R.string.att_col_percent)),
        rows = c.students.map { s ->
            val mark = studentMark(s, today, today)
            listOf(s.name, s.registerNo, context.getString(markLabel(mark)), String.format(Locale.US, "%.1f%%", semester.map { studentMark(s, it, today) }.totals().percent))
        },
        weights = listOf(2f, 1.4f, 1f, 1f),
    )
    saveAndOpen(context, "Attendance_${c.shortName.replace(" · ", "_").replace(' ', '_')}_${fileDate(today)}", table)
}

private fun exportGroup(context: android.content.Context, g: ReportStaffGroup, today: Day) {
    val month = AcademicCalendar.workingDays(today.yearMonth.first, today)
    val table = AttendanceTable(
        title = context.getString(R.string.att_export_group_title, g.name),
        subtitle = mediumDate(today),
        header = listOf(context.getString(R.string.att_col_name), context.getString(R.string.att_col_employee_id), context.getString(R.string.att_col_designation), context.getString(R.string.att_col_today), context.getString(R.string.att_col_percent)),
        rows = g.members.map { m ->
            listOf(m.name, m.employeeId, m.designation, context.getString(markLabel(staffMark(m, today, today))), String.format(Locale.US, "%.1f%%", month.map { staffMark(m, it, today) }.totals().percent))
        },
        weights = listOf(2f, 1.2f, 1.6f, 1f, 1f),
    )
    saveAndOpen(context, "Attendance_${g.name.replace(' ', '_')}_${fileDate(today)}", table)
}

private fun saveAndOpen(context: android.content.Context, baseName: String, table: AttendanceTable) {
    val file = runCatching { AttendanceExport.export(context, baseName, ExportFormat.PDF, table) }.getOrNull()
    if (file == null) {
        Toast.makeText(context, R.string.att_export_failed, Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, context.getString(R.string.att_saved_to_downloads, file.name), Toast.LENGTH_SHORT).show()
        FacultyExport.open(context, file)
    }
}

fun markLabel(mark: Mark): Int = when (mark) {
    Mark.PRESENT -> R.string.att_status_present
    Mark.LATE -> R.string.att_status_late
    Mark.ABSENT -> R.string.att_status_absent
    Mark.LEAVE -> R.string.att_status_on_leave
    Mark.OFF -> R.string.att_no_class
}

