package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.family

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.Segment
import com.vsca.vsnapvoicecollege.ui.components.SegmentedControl
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttStatTile
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceExport
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceLevel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceTable
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.MINIMUM_PERCENT
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.PercentBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.PercentRing
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.fileDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveDates
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRequest
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveStatusPill
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.appliedLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelColor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.mediumDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthShort
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthYear
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.teacher.BadgeTabs
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.weekdayHeaders
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportFormat
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyExport
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultySheet
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.LoadState
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.SheetHeader
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.timeOfDay
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.MessageCard
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.RetryButton
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.util.Calendar
import java.util.Locale

/** Light lavender behind on-leave days. */
private val LeaveTint = Color(0xFFE3E8FB)

/** Attendance for students and parents: overall %, the calendar or subject-wise hours, and leave history. */
@Composable
fun FamilyAttendanceScreen(
    onBack: () -> Unit,
    onApplyLeave: (studentId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyAttendanceViewModel = viewModel(),
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val child = s.child
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "header") {
                TitledTopBar(
                    title = stringResource(R.string.att_title),
                    subtitle = child?.let { "${it.student.name} · ${it.reportClass.programme} · Sem ${it.reportClass.division.semester} ${it.reportClass.division.section}" },
                    onBack = onBack,
                    trailing = { if (child != null) SquareIconButton(R.drawable.ic_download, stringResource(R.string.att_export), { exportChild(context, s) }, tint = BrandBlue) },
                )
            }
            if (!s.isOnline && s.load == LoadState.READY) {
                item(key = "offline") { OfflineBanner(stringResource(R.string.att_family_offline), Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
            }
            if (s.showChildSwitcher) {
                item(key = "children") {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        s.children.forEach { c ->
                            val selected = c.student.id == child?.student?.id
                            Surface(
                                onClick = { viewModel.onChild(c.student.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                color = if (selected) SelectedRowBackground else White,
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BrandBlue else FieldBorder),
                            ) {
                                Text(
                                    c.student.name,
                                    Modifier.padding(vertical = 10.dp),
                                    color = if (selected) BrandBlue else TextPrimary,
                                    fontSize = AppTextSize.Body,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "tabs") {
                BadgeTabs(
                    labels = listOf(stringResource(R.string.att_tab_attendance), stringResource(R.string.att_tab_leave_history)),
                    badges = listOf(0, s.leaves.size),
                    selected = s.tab.ordinal,
                    onSelect = { viewModel.onTab(FamilyTab.entries[it]) },
                )
            }
            when {
                s.load == LoadState.LOADING -> item(key = "loading") { SkeletonCards(2, Modifier.padding(16.dp)) }
                s.load == LoadState.ERROR || child == null -> item(key = "error") {
                    MessageCard {
                        StateMessage(
                            iconRes = R.drawable.ic_wifi,
                            iconTint = BadgeRed,
                            iconBackground = AlertBackground,
                            title = stringResource(R.string.att_family_error_title),
                            subtitle = stringResource(R.string.att_error_body),
                        ) { RetryButton(viewModel::refresh) }
                    }
                }
                s.tab == FamilyTab.ATTENDANCE -> attendanceTab(s, viewModel)
                else -> leavesTab(s)
            }
        }
        if (s.tab == FamilyTab.LEAVES && child != null) {
            Surface(Modifier.fillMaxWidth(), color = White, shadowElevation = 8.dp) {
                Button(
                    onClick = { onApplyLeave(child.student.id) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.att_apply_leave), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    s.openDay?.let { day -> if (child != null) DaySheet(child, day, s.today, viewModel::closeDay) }
}

private fun LazyListScope.attendanceTab(s: FamilyUiState, vm: FamilyAttendanceViewModel) {
    item(key = "overall") { OverallCard(s) }
    item(key = "views") {
        SegmentedControl(
            options = listOf(Segment(stringResource(R.string.att_view_calendar)), Segment(stringResource(R.string.att_view_subjects))),
            selectedIndex = s.view.ordinal,
            onSelect = { vm.onView(FamilyView.entries[it]) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            height = 52.dp,
        )
    }
    when (s.view) {
        FamilyView.CALENDAR -> {
            item(key = "calendar") { FamilyCalendar(s, vm) }
            item(key = "month-counts") {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AttStatTile(s.monthCount(DayKind.PRESENT).toString(), stringResource(R.string.att_present_days_month, monthShort(s.month)), Modifier.weight(1f), OnboardingGreenDisc, AttGreen)
                    AttStatTile(s.monthCount(DayKind.ABSENT).toString(), stringResource(R.string.att_absent_days_month, monthShort(s.month)), Modifier.weight(1f), AlertBackground, BadgeRed)
                    AttStatTile(s.monthCount(DayKind.LEAVE).toString(), stringResource(R.string.att_leave_days_month, monthShort(s.month)), Modifier.weight(1f), LeaveTint, BrandBlue)
                }
            }
        }
        FamilyView.SUBJECTS -> items(s.subjects, key = { it.subject }) { SubjectCard(it) }
    }
}

@Composable
private fun OverallCard(s: FamilyUiState) {
    val level = levelOf(s.percent)
    val start = s.semesterStart.yearMonth
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            PercentRing(s.percent, levelColor(level), size = 92.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(R.string.att_overall_span, monthShort(start), monthShort(start.plus(4)), start.plus(4).year).uppercase(Locale.getDefault()),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                )
                Text(stringResource(R.string.att_present_of_days, s.presentDays, s.workingDays), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                val above = s.percent >= MINIMUM_PERCENT
                LabelPill(
                    stringResource(if (above) R.string.att_above_required else R.string.att_below_required),
                    if (above) OnboardingGreenDisc else AlertBackground,
                    if (above) AttGreen else BadgeRed,
                    iconRes = if (above) R.drawable.ic_check else R.drawable.ic_info_outline,
                )
            }
        }
    }
}

@Composable
private fun FamilyCalendar(s: FamilyUiState, vm: FamilyAttendanceViewModel) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val canBack = s.month > s.semesterStart.yearMonth
                val canForward = s.month < s.today.yearMonth
                Icon(
                    painterResource(R.drawable.ic_chevron_left), stringResource(R.string.att_previous_month), tint = if (canBack) TextPrimary else TextMuted,
                    modifier = Modifier.clip(CircleShape).clickable(enabled = canBack) { vm.onMonth(-1) }.padding(8.dp).size(22.dp),
                )
                Text(monthYear(s.month), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Icon(
                    painterResource(R.drawable.ic_chevron_right), stringResource(R.string.att_next_month), tint = if (canForward) TextPrimary else TextMuted,
                    modifier = Modifier.clip(CircleShape).clickable(enabled = canForward) { vm.onMonth(1) }.padding(8.dp).size(22.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                weekdayHeaders(Calendar.SUNDAY, "EEEEE").forEach {
                    Text(it, Modifier.weight(1f), color = TextSecondary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(6.dp))
            val lead = s.month.first.dayOfWeek - Calendar.SUNDAY
            val cells: List<Day?> = List(lead) { null } + s.month.days
            cells.chunked(7).forEach { week ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    (0 until 7).forEach { i ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            week.getOrNull(i)?.let { day -> DayCircle(day, s.days[day], day == s.today) { vm.openDay(day) } }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FieldBorder)
            Legend()
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.att_tap_day_hint), color = TextSecondary, fontSize = AppTextSize.Label)
        }
    }
}

@Composable
private fun DayCircle(day: Day, kind: DayKind?, isToday: Boolean, onClick: () -> Unit) {
    val (background, content) = when (kind) {
        DayKind.PRESENT -> OnboardingGreenDisc to AttGreen
        DayKind.PARTLY -> AmberChipBackground to OnboardingAmberCard
        DayKind.ABSENT -> AlertBackground to BadgeRed
        DayKind.LEAVE -> LeaveTint to BrandBlue
        DayKind.HOLIDAY -> BadgeNeutralBackground to TextSecondary
        DayKind.NONE, null -> Color.Transparent to TextPrimary
    }
    val marked = kind == DayKind.PRESENT || kind == DayKind.PARTLY || kind == DayKind.ABSENT || kind == DayKind.LEAVE
    Box(
        Modifier.padding(3.dp).fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(background)
            .then(if (isToday) Modifier.border(2.dp, BrandBlue, CircleShape) else Modifier)
            .clickable(enabled = marked, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(day.day.toString(), color = content, fontSize = AppTextSize.BodyLarge, fontWeight = if (marked) FontWeight.ExtraBold else FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    val items = listOf(
        stringResource(R.string.att_status_present) to OnboardingGreenDisc,
        stringResource(R.string.att_status_absent) to BadgeRed,
        stringResource(R.string.att_partly_present) to AmberChipBackground,
        stringResource(R.string.att_status_on_leave) to LeaveTint,
        stringResource(R.string.att_holiday) to BadgeNeutralBackground,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, color = TextPrimary, fontSize = AppTextSize.Body)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).border(2.dp, BrandBlue, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.att_today), color = TextPrimary, fontSize = AppTextSize.Body)
        }
    }
}

@Composable
private fun SubjectCard(f: SubjectFigure) {
    val level = levelOf(f.percent)
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, if (level == AttendanceLevel.LOW) BadgeRed.copy(alpha = 0.5f) else FieldBorder),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SelectedRowBackground), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_book), null, tint = BrandBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.subject, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                    if (f.teacher.isNotBlank()) Text(f.teacher, color = TextSecondary, fontSize = AppTextSize.Body)
                }
                Text(percentLabel(f.percent), color = levelColor(level), fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(10.dp))
            PercentBar((f.percent / 100).toFloat(), levelColor(level), markerAt = MINIMUM_PERCENT / 100f, height = 10.dp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelPill(stringResource(R.string.att_hours_attended, f.attended), OnboardingGreenDisc, AttGreen, iconRes = R.drawable.ic_check)
                if (f.absent > 0) LabelPill(stringResource(R.string.att_hours_absent, f.absent), AlertBackground, BadgeRed, iconRes = R.drawable.ic_close)
            }
        }
    }
}

private fun LazyListScope.leavesTab(s: FamilyUiState) {
    if (s.leaves.isEmpty()) {
        item(key = "no-leaves") {
            MessageCard {
                StateMessage(
                    iconRes = R.drawable.ic_calendar,
                    iconTint = BrandBlue,
                    iconBackground = SelectedRowBackground,
                    title = stringResource(R.string.att_no_leave_history_title),
                    subtitle = stringResource(R.string.att_no_leave_history_body),
                )
            }
        }
    }
    items(s.leaves, key = { it.id }) { LeaveHistoryCard(it) }
}

@Composable
private fun LeaveHistoryCard(r: LeaveRequest) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelPill(stringResource(r.type.label), SelectedRowBackground, BrandBlue)
                Spacer(Modifier.width(10.dp))
                Text(appliedLabel(r.appliedAt), Modifier.weight(1f), color = TextSecondary, fontSize = AppTextSize.Label)
                LeaveStatusPill(r.status)
            }
            Spacer(Modifier.height(10.dp))
            LeaveDates(r)
            Spacer(Modifier.height(10.dp))
            Text(r.reason, color = TextPrimary, fontSize = AppTextSize.Body, lineHeight = 21.sp)
            r.decisionNote?.let {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.att_reject_reason_shown, it), color = BadgeRed, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Period-wise attendance for one day. */
@Composable
private fun DaySheet(child: FamilyChild, day: Day, today: Day, onDismiss: () -> Unit) {
    val periods = periodMarks(child, day, today)
    val present = periods.count { it.mark == PeriodMark.PRESENT }
    FacultySheet(onDismiss) {
        SheetHeader(mediumDate(day), null, onDismiss)
        Spacer(Modifier.height(6.dp))
        val (bg, fg) = when {
            periods.all { it.mark == PeriodMark.LEAVE } -> LeaveTint to BrandBlue
            present == periods.size -> OnboardingGreenDisc to AttGreen
            present == 0 -> AlertBackground to BadgeRed
            else -> AmberChipBackground to OnboardingAmberCard
        }
        LabelPill(
            if (periods.all { it.mark == PeriodMark.LEAVE }) stringResource(R.string.att_on_leave_all_day) else stringResource(R.string.att_periods_present, present, periods.size),
            bg,
            fg,
        )
        Spacer(Modifier.height(12.dp))
        periods.forEachIndexed { index, p ->
            if (index > 0) HorizontalDivider(color = FieldBorder)
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = SelectedRowBackground) {
                    Text("P${p.slot.number}", Modifier.padding(horizontal = 14.dp, vertical = 12.dp), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.subject, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.att_span, timeOfDay(p.slot.startMinute), timeOfDay(p.slot.endMinute)), color = TextSecondary, fontSize = AppTextSize.Body)
                }
                when (p.mark) {
                    PeriodMark.PRESENT -> LabelPill(stringResource(R.string.att_status_present), OnboardingGreenDisc, AttGreen)
                    PeriodMark.ABSENT -> LabelPill(stringResource(R.string.att_status_absent), AlertBackground, BadgeRed)
                    PeriodMark.LEAVE -> LabelPill(stringResource(R.string.att_status_on_leave), LeaveTint, BrandBlue)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Saves the child's semester attendance (one row per working day) as a PDF and opens it. */
private fun exportChild(context: Context, s: FamilyUiState) {
    val child = s.child ?: return
    val rows = s.days.toSortedMap().filter { (_, kind) -> kind != DayKind.HOLIDAY && kind != DayKind.NONE }.map { (day, kind) ->
        val periods = periodMarks(child, day, s.today)
        listOf(
            mediumDate(day),
            context.getString(
                when (kind) {
                    DayKind.PRESENT -> R.string.att_status_present
                    DayKind.PARTLY -> R.string.att_partly_present
                    DayKind.ABSENT -> R.string.att_status_absent
                    else -> R.string.att_status_on_leave
                },
            ),
            "${periods.count { it.mark == PeriodMark.PRESENT }}/${periods.size}",
        )
    }
    val table = AttendanceTable(
        title = context.getString(R.string.att_export_child_title, child.student.name),
        subtitle = context.getString(R.string.att_present_of_days, s.presentDays, s.workingDays) + " · " + String.format(Locale.getDefault(), "%.0f%%", s.percent),
        header = listOf(context.getString(R.string.att_col_date), context.getString(R.string.att_col_status), context.getString(R.string.att_col_periods)),
        rows = rows,
        weights = listOf(2f, 1.5f, 1f),
    )
    val file = runCatching { AttendanceExport.export(context, "Attendance_${child.student.name.replace(".", "").replace(' ', '_')}_${fileDate(s.today)}", ExportFormat.PDF, table) }.getOrNull()
    if (file == null) {
        Toast.makeText(context, R.string.att_export_failed, Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, context.getString(R.string.att_saved_to_downloads, file.name), Toast.LENGTH_SHORT).show()
        FacultyExport.open(context, file)
    }
}

