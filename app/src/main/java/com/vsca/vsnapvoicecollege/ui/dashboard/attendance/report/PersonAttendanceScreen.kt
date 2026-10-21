package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceLevel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.ListCard
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.PercentBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.PercentRing
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.YearMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelBackground
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelColor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthShort
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.stableUnit
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.timeOfDay
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen

enum class PersonKind { STUDENT, STAFF }

/** A month's figure on the "Monthly view" strip. */
private data class MonthFigure(val month: YearMonth, val percent: Double, val soFar: Boolean)

/** An absent (or leave) day with its reason. */
private data class AbsentDay(val day: Day, val mark: Mark)

/**
 * One student or staff member: overall %, how far from the 75% minimum, subject-wise (students)
 * or punctuality (staff), month by month, and the latest absences.
 */
@Composable
fun PersonAttendanceScreen(kind: PersonKind, id: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val data = remember { ReportRepository.data() }
    val today = Day.today()
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(if (kind == PersonKind.STUDENT) R.string.att_student_detail_title else R.string.att_staff_detail_title),
            onBack = onBack,
        )
        when (kind) {
            PersonKind.STUDENT -> data.student(id)?.let { s ->
                val c = data.classById(s.classId)
                StudentBody(s, c, today)
            }
            PersonKind.STAFF -> data.staffMember(id)?.let { m -> StaffBody(m, data.groupById(m.groupId), today) }
        } ?: Text(stringResource(R.string.att_not_found), Modifier.padding(32.dp), color = TextSecondary)
    }
}

@Composable
private fun StudentBody(s: ReportStudent, c: ReportClass?, today: Day) {
    val semester = remember(s) { AcademicCalendar.workingDays(AcademicCalendar.semesterStart(today), today).map { it to studentMark(s, it, today) } }
    val totals = semester.map { it.second }.totals()
    val level = levelOf(totals.percent)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(2.dp))
        HeaderCard(
            percent = totals.percent,
            name = s.name,
            subtitle = listOfNotNull(c?.shortName, s.registerNo).joinToString(" · "),
            level = level,
        )
        Banner(
            level,
            when (level) {
                AttendanceLevel.LOW -> stringResource(R.string.att_banner_low_student, s.name.substringBefore(' '), daysToReachMinimum(totals.present, totals.marked))
                AttendanceLevel.WATCH -> stringResource(R.string.att_banner_watch_student)
                AttendanceLevel.GOOD -> stringResource(R.string.att_banner_good)
            },
        )
        // Subject-wise: each subject's hours this semester, the student's rate nudged per subject.
        c?.subjects?.takeIf { it.isNotEmpty() }?.let { subjects ->
            CardSection(stringResource(R.string.att_subject_wise)) {
                subjects.forEach { subject ->
                    val percent = (totals.percent + (stableUnit("subj", s.id, subject.name) - 0.5) * 14).coerceIn(40.0, 100.0)
                    val subjectLevel = levelOf(percent)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(subject.name, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.Body)
                        Text(
                            stringResource(R.string.att_percent_level, percentLabel(percent, 1), levelLabel(subjectLevel)),
                            color = TextPrimary,
                            fontSize = AppTextSize.Body,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    PercentBar((percent / 100).toFloat(), BrandBlue, markerAt = 0.75f)
                    Spacer(Modifier.height(8.dp))
                }
                MinimumNote()
            }
        }
        MonthlyView(monthly(semester, today))
        AbsentDates(semester.filter { it.second == Mark.ABSENT || it.second == Mark.LEAVE }.map { AbsentDay(it.first, it.second) }, student = true)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StaffBody(m: ReportStaffMember, group: ReportStaffGroup?, today: Day) {
    val semester = remember(m) { AcademicCalendar.workingDays(AcademicCalendar.semesterStart(today), today).map { it to staffMark(m, it, today) } }
    val totals = semester.map { it.second }.totals()
    val thisMonth = semester.filter { it.first.yearMonth == today.yearMonth }.map { it.second }
    val lateThisMonth = thisMonth.count { it == Mark.LATE }
    val level = levelOf(totals.percent)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(2.dp))
        HeaderCard(
            percent = totals.percent,
            name = m.name,
            subtitle = listOfNotNull(m.designation, group?.name, m.employeeId).joinToString(" · "),
            level = level,
        )
        Banner(
            level,
            when {
                level == AttendanceLevel.LOW -> stringResource(R.string.att_banner_low_staff, daysToReachMinimum(totals.present, totals.marked))
                lateThisMonth > 0 && level == AttendanceLevel.WATCH -> stringResource(R.string.att_banner_watch_late, lateThisMonth)
                level == AttendanceLevel.WATCH -> stringResource(R.string.att_banner_watch_staff)
                else -> stringResource(R.string.att_banner_good)
            },
        )
        CardSection(stringResource(R.string.att_punctuality_month)) {
            val marked = thisMonth.size.coerceAtLeast(1)
            listOf(
                stringResource(R.string.att_on_time) to thisMonth.count { it == Mark.PRESENT },
                stringResource(R.string.att_late_after, timeOfDay(9 * 60)) to lateThisMonth,
                stringResource(R.string.att_status_absent) to thisMonth.count { it == Mark.ABSENT },
                stringResource(R.string.att_status_on_leave) to thisMonth.count { it == Mark.LEAVE },
            ).forEach { (label, n) ->
                val percent = n * 100.0 / marked
                Row {
                    Text(label, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.Body)
                    Text(percentLabel(percent, 1), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                }
                PercentBar((percent / 100).toFloat(), BrandBlue)
                Spacer(Modifier.height(8.dp))
            }
        }
        MonthlyView(monthly(semester, today))
        AbsentDates(semester.filter { it.second == Mark.ABSENT || it.second == Mark.LEAVE }.map { AbsentDay(it.first, it.second) }, student = false)
        Spacer(Modifier.height(24.dp))
    }
}

private fun monthly(days: List<Pair<Day, Mark>>, today: Day): List<MonthFigure> =
    days.groupBy { it.first.yearMonth }.map { (month, list) -> MonthFigure(month, list.map { it.second }.totals().percent, month == today.yearMonth) }

@Composable
private fun HeaderCard(percent: Double, name: String, subtitle: String, level: AttendanceLevel) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            PercentRing(percent, BrandBlue, size = 92.dp, decimals = 1)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(name, color = TextPrimary, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Body)
                Spacer(Modifier.height(6.dp))
                LabelPill(
                    stringResource(
                        when (level) {
                            AttendanceLevel.LOW -> R.string.att_chip_low
                            AttendanceLevel.WATCH -> R.string.att_chip_watch
                            AttendanceLevel.GOOD -> R.string.att_chip_good
                        },
                    ),
                    levelBackground(level),
                    levelColor(level),
                )
            }
        }
    }
}

@Composable
private fun Banner(level: AttendanceLevel, text: String) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), color = levelBackground(level)) {
        Text(
            text,
            Modifier.padding(16.dp),
            color = if (level == AttendanceLevel.WATCH) OnboardingAmberCard else levelColor(level),
            fontSize = AppTextSize.Body,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun CardSection(title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MinimumNote() {
    Text(stringResource(R.string.att_dashed_minimum), color = TextSecondary, fontSize = AppTextSize.Label)
}

@Composable
private fun MonthlyView(months: List<MonthFigure>) {
    if (months.isEmpty()) return
    CardSection(stringResource(R.string.att_monthly_view)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            months.takeLast(4).forEach { m ->
                val level = levelOf(m.percent)
                val shape = RoundedCornerShape(16.dp)
                val tile = Modifier.weight(1f).background(levelBackground(level), shape).let {
                    if (level == AttendanceLevel.LOW) {
                        it.drawBehind {
                            drawRoundRect(BadgeRed, cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
                        }
                    } else {
                        it
                    }
                }
                Column(tile.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (m.soFar) stringResource(R.string.att_month_so_far, monthShort(m.month)) else monthShort(m.month),
                        color = TextPrimary,
                        fontSize = AppTextSize.Label,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(percentLabel(m.percent, 1), color = levelColor(level), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Text(levelLabel(level), color = levelColor(level), fontSize = AppTextSize.Label)
                }
            }
        }
    }
}

@Composable
private fun AbsentDates(days: List<AbsentDay>, student: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.att_absent_dates), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
        Text(
            if (days.size > LATEST) stringResource(R.string.att_absent_count_latest, days.size, LATEST) else stringResource(R.string.att_absent_count, days.size),
            color = TextSecondary,
            fontSize = AppTextSize.Label,
        )
    }
    if (days.isEmpty()) {
        Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), color = OnboardingGreenDisc) {
            Text(stringResource(R.string.att_no_absences), Modifier.padding(16.dp), color = AttGreen, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
        }
        return
    }
    ListCard(Modifier.padding(horizontal = 16.dp)) {
        days.sortedByDescending { it.day }.take(LATEST).forEachIndexed { index, (day, mark) ->
            if (index > 0) HorizontalDivider(color = FieldBorder)
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = BadgeNeutralBackground) {
                    Column(Modifier.width(56.dp).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.day.toString(), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                        Text(monthShort(day.yearMonth), color = TextSecondary, fontSize = AppTextSize.Label)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    val leave = mark == Mark.LEAVE
                    Text(
                        stringResource(
                            when {
                                leave -> R.string.att_reason_leave
                                student -> R.string.att_reason_none
                                else -> R.string.att_reason_not_recorded
                            },
                        ),
                        color = TextPrimary,
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            when {
                                leave -> R.string.att_reason_leave_detail
                                student -> R.string.att_reason_parent_alerted
                                else -> R.string.att_reason_hr_notified
                            },
                        ),
                        color = TextSecondary,
                        fontSize = AppTextSize.Label,
                    )
                }
            }
        }
    }
}

private const val LATEST = 5

