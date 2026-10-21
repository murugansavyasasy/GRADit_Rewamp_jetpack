package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.FilterDropdown
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.Segment
import com.vsca.vsnapvoicecollege.ui.components.SegmentedControl
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.pickPastDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceFooter
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.ListCard
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.PercentBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.RemovableFilterChip
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelBackground
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelColor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.mediumDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.LoadState
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.MessageCard
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.RetryButton
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.syncedLabel
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlueDark
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** The principal's Attendance tab: students by class or staff by group for a day, week or month. */
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    onOpenClass: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.att_report_title),
                onBack = onBack,
                trailing = {
                    SquareIconButton(R.drawable.ic_download, stringResource(R.string.att_export), {
                        if (state.data != null && !state.isEmpty) {
                            viewModel.prepareExport()
                            onExport()
                        }
                    }, tint = BrandBlue)
                },
            )
        }
        if (!state.isOnline && state.load == LoadState.READY) {
            item(key = "offline") {
                OfflineBanner(stringResource(R.string.att_offline_banner, syncedLabel(state.lastSyncedAt)), Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            }
        }
        item(key = "ranges") {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    ReportRange.TODAY to R.string.att_range_today,
                    ReportRange.YESTERDAY to R.string.att_range_yesterday,
                    ReportRange.THIS_WEEK to R.string.att_range_week,
                    ReportRange.THIS_MONTH to R.string.att_range_month,
                ).forEach { (range, label) ->
                    val selected = state.filter.range == range
                    Surface(
                        onClick = { viewModel.onRange(range) },
                        shape = RoundedCornerShape(50),
                        color = if (selected) BrandBlue else White,
                        border = if (selected) null else BorderStroke(1.dp, FieldBorder),
                    ) {
                        Text(
                            stringResource(label),
                            Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            color = if (selected) White else TextPrimary,
                            fontSize = AppTextSize.Body,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
        item(key = "date") {
            Surface(
                onClick = { pickPastDate(context, state.span.to.millis) { viewModel.onCustomDay(Day.of(it)) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                color = White,
                border = BorderStroke(1.dp, FieldBorder),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_calendar), null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(spanLabel(state.span), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        item(key = "filters") { Filters(state, viewModel) }
        item(key = "audience") {
            SegmentedControl(
                options = listOf(Segment(stringResource(R.string.att_students)), Segment(stringResource(R.string.att_staff))),
                selectedIndex = state.filter.audience.ordinal,
                onSelect = { viewModel.onAudience(Audience.entries[it]) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                height = 52.dp,
            )
        }
        when {
            state.load == LoadState.LOADING -> item(key = "loading") { LoadingTiles() }
            state.load == LoadState.ERROR || state.data == null -> item(key = "error") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_wifi,
                        iconTint = BadgeRed,
                        iconBackground = AlertBackground,
                        title = stringResource(R.string.att_report_error_title),
                        subtitle = stringResource(R.string.att_report_error_body),
                    ) { RetryButton(viewModel::refresh) }
                }
            }
            state.isEmpty -> item(key = "empty") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_calendar,
                        iconTint = BrandBlue,
                        iconBackground = SelectedRowBackground,
                        title = stringResource(R.string.att_report_empty_title),
                        subtitle = stringResource(R.string.att_report_empty_body, spanLabel(state.span)),
                    ) {
                        if (state.filter.range != ReportRange.TODAY) {
                            Spacer(Modifier.height(16.dp))
                            Surface(onClick = { viewModel.onRange(ReportRange.TODAY) }, shape = RoundedCornerShape(16.dp), color = SelectedRowBackground) {
                                Text(
                                    stringResource(R.string.att_show_today),
                                    Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                                    color = BrandBlue,
                                    fontSize = AppTextSize.BodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            else -> reportBody(state, viewModel, onOpenClass, onOpenGroup)
        }
    }
}

/** "Tue, 6 Oct 2026" or "5 Oct 2026 – 10 Oct 2026". */
@Composable
internal fun spanLabel(span: DateSpan): String =
    if (span.isSingleDay) mediumDate(span.from) else stringResource(R.string.att_span, mediumDate(span.from), mediumDate(span.to))

@Composable
private fun Filters(state: ReportUiState, vm: ReportViewModel) {
    val all = stringResource(R.string.att_filter_all)
    val f = state.filter
    val department = state.departments.firstOrNull { it.first == f.departmentId }?.second
    val classFilters = f.audience == Audience.STUDENTS
    Column {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterDropdown(
                label = stringResource(R.string.att_filter_department),
                value = department ?: all,
                options = listOf(all to { vm.onDepartment(null) }) + state.departments.map { (id, name) -> name to { vm.onDepartment(id) } },
                selected = department ?: all,
                highlighted = department != null,
            )
            if (classFilters) {
                FilterDropdown(
                    label = stringResource(R.string.att_filter_year_sem),
                    value = f.semester?.let { yearSemLabel(it) } ?: all,
                    options = listOf(all to { vm.onSemester(null) }) + state.semesters.map { s -> yearSemLabel(s) to { vm.onSemester(s) } },
                    selected = f.semester?.let { yearSemLabel(it) } ?: all,
                    highlighted = f.semester != null,
                )
                FilterDropdown(
                    label = stringResource(R.string.att_filter_section),
                    value = f.section ?: all,
                    options = listOf(all to { vm.onSection(null) }) + state.sections.map { s -> s to { vm.onSection(s) } },
                    selected = f.section ?: all,
                    highlighted = f.section != null,
                )
            }
        }
        if (f.hasClassFilters) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                department?.let { RemovableFilterChip(it, { vm.onDepartment(null) }) }
                if (classFilters) {
                    f.semester?.let { RemovableFilterChip(stringResource(R.string.att_semester_n, it), { vm.onSemester(null) }) }
                    f.section?.let { RemovableFilterChip(stringResource(R.string.att_section_n, it), { vm.onSection(null) }) }
                }
                Text(
                    stringResource(R.string.att_clear_all),
                    Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = vm::onClearFilters).padding(8.dp),
                    color = BrandBlue,
                    fontSize = AppTextSize.Body,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** "III · Sem 5". */
@Composable
internal fun yearSemLabel(semester: Int): String = stringResource(R.string.att_year_sem, roman((semester + 1) / 2), semester)

private fun roman(n: Int): String = listOf("", "I", "II", "III", "IV", "V").getOrElse(n) { n.toString() }

private fun LazyListScope.reportBody(
    state: ReportUiState,
    vm: ReportViewModel,
    onOpenClass: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
) {
    item(key = "summary") { SummaryCards(state) }
    item(key = "chart") { ChartCard(state, vm::onToggleTable) }
    when (state.filter.audience) {
        Audience.STUDENTS -> item(key = "classes") {
            SectionTitle(stringResource(R.string.att_classes_count, state.classes.size))
            ListCard(Modifier.padding(horizontal = 16.dp)) {
                state.classes.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(color = FieldBorder)
                    GroupRowItem(
                        title = row.reportClass.shortName,
                        subtitle = row.reportClass.mentor?.let { stringResource(R.string.att_students_mentor, row.reportClass.students.size, it) }
                            ?: stringResource(R.string.att_students_n, row.reportClass.students.size),
                        percent = row.totals.percent,
                        onClick = { onOpenClass(row.reportClass.id) },
                    )
                }
            }
        }
        Audience.STAFF -> item(key = "groups") {
            SectionTitle(stringResource(R.string.att_groups_count, state.groups.size))
            ListCard(Modifier.padding(horizontal = 16.dp)) {
                state.groups.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(color = FieldBorder)
                    GroupRowItem(
                        title = row.group.name,
                        subtitle = row.group.head?.let { stringResource(R.string.att_staff_head, row.group.members.size, it) }
                            ?: stringResource(R.string.att_staff_n, row.group.members.size),
                        percent = row.totals.percent,
                        tag = if (row.group.teaching) null else stringResource(R.string.att_non_teaching),
                        onClick = { onOpenGroup(row.group.id) },
                    )
                }
            }
        }
    }
    item(key = "footer") { AttendanceFooter(Modifier.padding(top = 20.dp), note = stringResource(R.string.att_report_footer)) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
}

// ---- Summary ----

@Composable
private fun SummaryCards(state: ReportUiState) {
    val c = state.current
    val p = state.previous
    val vs = stringResource(
        when (state.filter.range) {
            ReportRange.TODAY ->
                if (previousWorkingDay(state.span.from) == state.span.from.plusDays(-1)) R.string.att_vs_yesterday else R.string.att_vs_day_before
            ReportRange.THIS_WEEK -> R.string.att_vs_last_week
            ReportRange.THIS_MONTH -> R.string.att_vs_last_month
            else -> R.string.att_vs_day_before
        },
    )
    val multiDay = !state.span.isSingleDay
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                stringResource(R.string.att_overall_attendance), percentLabel(c.percent, 1),
                deltaText(c.percent - p.percent, higherIsBetter = true, points = true), c.percent - p.percent >= 0, vs, Modifier.weight(1f), valueColor = BrandBlue,
            )
            SummaryCard(
                stringResource(if (multiDay) R.string.att_present_avg else R.string.att_status_present), count(c.present),
                deltaText((c.present - p.present).toDouble(), higherIsBetter = true), c.present >= p.present, vs, Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                stringResource(if (multiDay) R.string.att_absent_avg else R.string.att_status_absent), count(c.absent),
                deltaText((c.absent - p.absent).toDouble(), higherIsBetter = false), c.absent <= p.absent, vs, Modifier.weight(1f), up = c.absent > p.absent,
            )
            SummaryCard(
                stringResource(R.string.att_late_on_leave), count(c.lateOrLeave),
                deltaText((c.lateOrLeave - p.lateOrLeave).toDouble(), higherIsBetter = false), c.lateOrLeave <= p.lateOrLeave, vs, Modifier.weight(1f), up = c.lateOrLeave > p.lateOrLeave,
            )
        }
    }
}

private fun count(n: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(n)

/** "2.1 pts better", "53 better", "12 worse", "No change". */
@Composable
private fun deltaText(delta: Double, higherIsBetter: Boolean, points: Boolean = false): String {
    if (abs(delta) < if (points) 0.05 else 0.5) return stringResource(R.string.att_no_change)
    val better = if (higherIsBetter) delta > 0 else delta < 0
    val amount = if (points) String.format(Locale.getDefault(), "%.1f", abs(delta)) else count(abs(delta).toInt())
    return stringResource(
        when {
            points && better -> R.string.att_pts_better
            points -> R.string.att_pts_worse
            better -> R.string.att_n_better
            else -> R.string.att_n_worse
        },
        amount,
    )
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    delta: String,
    good: Boolean,
    vs: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
    up: Boolean = good,
) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = valueColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            if (delta == stringResource(R.string.att_no_change)) {
                Text(delta, color = TextSecondary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
            } else {
                Text((if (up) "▲ " else "▼ ") + delta, color = if (good) AttGreen else BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(vs, color = TextSecondary, fontSize = AppTextSize.Body)
        }
    }
}

// ---- Chart ----

@Composable
private fun ChartCard(state: ReportUiState, onToggleTable: () -> Unit) {
    val title = when {
        state.span.isSingleDay && state.filter.audience == Audience.STUDENTS ->
            if (state.span.from == state.today) stringResource(R.string.att_by_period_today) else stringResource(R.string.att_by_period)
        state.span.isSingleDay -> stringResource(R.string.att_by_group)
        else -> stringResource(R.string.att_by_day)
    }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                OutlinedButton(onClick = onToggleTable, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, FieldBorder)) {
                    Text(
                        stringResource(if (state.showTable) R.string.att_show_chart else R.string.att_show_table),
                        color = TextPrimary,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.chart.isEmpty()) {
                Text(stringResource(R.string.att_chart_none_yet), color = TextSecondary, fontSize = AppTextSize.Body)
            } else if (state.showTable) {
                ChartTable(state.chart)
            } else {
                BarChart(state.chart)
            }
        }
    }
}

@Composable
private fun BarChart(bars: List<ChartBar>) {
    // Bars start at 60% so differences between periods are visible.
    val floor = 60.0
    val scroll = bars.size > 8
    Row(
        Modifier.fillMaxWidth().height(170.dp).then(if (scroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            val fraction = ((bar.percent - floor) / (100 - floor)).coerceIn(0.05, 1.0).toFloat()
            Column(
                (if (scroll) Modifier.width(34.dp) else Modifier.weight(1f)).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(String.format(Locale.getDefault(), "%.1f", bar.percent), color = TextPrimary, fontSize = AppTextSize.Caption, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(fraction * 0.72f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (bar.highlighted) BrandBlueDark else BrandBlue)
                        .then(if (bar.highlighted) Modifier.border(2.dp, TextPrimary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) else Modifier),
                )
                Spacer(Modifier.height(6.dp))
                Text(bar.label, color = TextSecondary, fontSize = AppTextSize.Caption, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ChartTable(bars: List<ChartBar>) {
    Column {
        Row(Modifier.padding(vertical = 6.dp)) {
            listOf(R.string.att_col_slot, R.string.att_col_present, R.string.att_col_absent, R.string.att_col_percent).forEachIndexed { i, label ->
                Text(stringResource(label), Modifier.weight(if (i == 0) 1.2f else 1f), color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold, textAlign = if (i == 0) TextAlign.Start else TextAlign.End)
            }
        }
        bars.forEach { bar ->
            HorizontalDivider(color = FieldBorder)
            Row(Modifier.padding(vertical = 10.dp)) {
                Text(bar.label, Modifier.weight(1.2f), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                Text(count(bar.present), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.Body, textAlign = TextAlign.End)
                Text(count(bar.absent), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.Body, textAlign = TextAlign.End)
                Text(percentLabel(bar.percent, 1), Modifier.weight(1f), color = levelColor(levelOf(bar.percent)), fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
        }
    }
}

// ---- Lists ----

@Composable
internal fun GroupRowItem(title: String, subtitle: String, percent: Double, onClick: () -> Unit, tag: String? = null) {
    val level = levelOf(percent)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f, fill = false), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                tag?.let {
                    Spacer(Modifier.width(8.dp))
                    LabelPill(it, SelectedRowBackground, BrandBlue)
                }
            }
            Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PercentBar((percent / 100).toFloat(), levelColor(level), Modifier.weight(1f), markerAt = 0.75f)
                Spacer(Modifier.width(10.dp))
                Text(percentLabel(percent, 1), color = levelColor(level), fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (level != com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceLevel.GOOD) {
            Spacer(Modifier.width(8.dp))
            LabelPill(levelLabel(level), levelBackground(level), levelColor(level))
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun LoadingTiles() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { Box(Modifier.weight(1f).heightIn(min = 100.dp).clip(RoundedCornerShape(20.dp)).background(FieldBorder)) }
            }
        }
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(22.dp)).background(FieldBorder))
    }
}
