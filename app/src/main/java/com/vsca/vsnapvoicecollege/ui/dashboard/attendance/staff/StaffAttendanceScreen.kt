package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.FilterDropdown
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttNeutralTile
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttStatTile
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceFooter
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.CountTabs
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.ExportButton
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.ExportFormatSheet
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.GroupHeader
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.ListCard
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.PercentBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.RemovableFilterChip
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.ResetAllLink
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.YearMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelColor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.longDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthName
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthYear
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.Faculty
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyDirectory
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.InitialsAvatar
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.LoadState
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.timeOfDay
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.MessageCard
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.RetryButton
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.syncedLabel
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberRing
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.util.Calendar

/**
 * Staff attendance for the principal. Today: who punched in, who's late, absent or on
 * leave, from the punch-in device. Staff-wise: each staff member's month or year.
 */
@Composable
fun StaffAttendanceScreen(
    onBack: () -> Unit,
    onOpenStaff: (String) -> Unit,
    onOpenFreePeriods: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StaffAttendanceViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val directory = state.directory
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.att_title),
                subtitle = directory?.let { stringResource(R.string.att_staff_subtitle, it.institution, it.faculty.size) },
                onBack = onBack,
            )
        }
        if (!state.isOnline && state.load == LoadState.READY) {
            item(key = "offline") {
                OfflineBanner(
                    stringResource(R.string.att_offline_banner, syncedLabel(state.lastSyncedAt)),
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        item(key = "tabs") {
            CountTabs(
                labels = listOf(stringResource(R.string.att_tab_today), stringResource(R.string.att_tab_staff_wise)),
                counts = listOf(state.totalStaff.takeIf { directory != null }, state.totalStaff.takeIf { directory != null }),
                selected = state.tab.ordinal,
                onSelect = { viewModel.onTab(StaffTab.entries[it]) },
            )
        }
        when {
            state.load == LoadState.LOADING -> item(key = "loading") {
                Column {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(22.dp)).background(FieldBorder))
                        SkeletonCards(2)
                    }
                    AttendanceFooter()
                }
            }
            state.load == LoadState.ERROR || directory == null -> item(key = "error") {
                Column {
                    MessageCard {
                        StateMessage(
                            iconRes = R.drawable.ic_wifi,
                            iconTint = BadgeRed,
                            iconBackground = AlertBackground,
                            title = stringResource(R.string.att_staff_error_title),
                            subtitle = stringResource(R.string.att_error_body),
                        ) { RetryButton(viewModel::refresh) }
                    }
                    AttendanceFooter()
                }
            }
            state.tab == StaffTab.TODAY -> todayTab(state, directory, viewModel, onOpenStaff, onOpenFreePeriods)
            else -> staffWiseTab(state, directory, viewModel, onOpenStaff)
        }
    }

    state.sheet?.let { sheet ->
        ExportFormatSheet(
            title = stringResource(if (sheet.tab == StaffTab.TODAY) R.string.att_export_today_sheet else R.string.att_export_staffwise_sheet),
            subtitle = pluralStringResource(
                R.plurals.att_export_staff_count,
                if (sheet.tab == StaffTab.TODAY) state.todayShown else state.rows.size,
                if (sheet.tab == StaffTab.TODAY) state.todayShown else state.rows.size,
            ),
            fileBaseName = StaffAttendanceExport.baseName(state, sheet.tab),
            format = sheet.format,
            exporting = sheet.exporting,
            failed = sheet.failed,
            result = sheet.result,
            onFormat = viewModel::onExportFormat,
            onExport = viewModel::export,
            onDismiss = viewModel::closeExport,
        )
    }
}

// ---- Today ----

private fun LazyListScope.todayTab(
    state: StaffAttendanceUiState,
    directory: FacultyDirectory,
    vm: StaffAttendanceViewModel,
    onOpenStaff: (String) -> Unit,
    onOpenFreePeriods: () -> Unit,
) {
    item(key = "today-head") {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(longDate(state.today), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                Text(
                    stringResource(R.string.att_last_updated, clockLabel(state.lastSyncedAt)),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                )
            }
            SquareIconButton(R.drawable.ic_refresh, stringResource(R.string.att_refresh), vm::refresh, tint = BrandBlue)
        }
    }
    item(key = "today-tiles") {
        val c = state.todayCounts
        val group = state.filter.todayGroup
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttStatTile(state.totalStaff.toString(), stringResource(R.string.att_total_staff), Modifier.weight(1f), selected = group == null, onClick = { vm.onTodayGroup(null) })
                AttStatTile((c[TodayGroup.PRESENT] ?: 0).plus(c[TodayGroup.LATE] ?: 0).toString(), stringResource(R.string.att_status_present), Modifier.weight(1f), OnboardingGreenDisc, AttGreen, group == TodayGroup.PRESENT) { vm.onTodayGroup(TodayGroup.PRESENT) }
                AttStatTile((c[TodayGroup.ABSENT] ?: 0).toString(), stringResource(R.string.att_status_absent), Modifier.weight(1f), AlertBackground, BadgeRed, group == TodayGroup.ABSENT) { vm.onTodayGroup(TodayGroup.ABSENT) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttStatTile((c[TodayGroup.ON_LEAVE] ?: 0).toString(), stringResource(R.string.att_status_on_leave), Modifier.weight(1f), AmberChipBackground, OnboardingAmberCard, group == TodayGroup.ON_LEAVE) { vm.onTodayGroup(TodayGroup.ON_LEAVE) }
                AttStatTile((c[TodayGroup.LATE] ?: 0).toString(), stringResource(R.string.att_status_late), Modifier.weight(1f), AmberChipBackground, OnboardingAmberCard, group == TodayGroup.LATE) { vm.onTodayGroup(TodayGroup.LATE) }
                AttStatTile((c[TodayGroup.NOT_IN] ?: 0).toString(), stringResource(R.string.att_status_not_in), Modifier.weight(1f), AttNeutralTile, TextPrimary, group == TodayGroup.NOT_IN) { vm.onTodayGroup(TodayGroup.NOT_IN) }
            }
        }
    }
    searchAndFilters(state, directory, vm)
    item(key = "today-chips") {
        val group = state.filter.todayGroup
        if (group != null || state.filter.hasNarrowing) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                group?.let { RemovableFilterChip(stringResource(todayGroupLabel(it)), { vm.onTodayGroup(null) }) }
                ResetAllLink(vm::onResetFilters)
            }
        }
    }
    item(key = "today-count") {
        CountRow(stringResource(R.string.att_staff_today_count, state.todayShown), enabled = state.todayShown > 0, onExport = vm::openExport)
    }
    if (state.todayGroups.isEmpty()) {
        item(key = "today-empty") { NoStaffMatch(vm::onResetFilters) }
    }
    // Present staff are listed last; a group shows "present" as one list with the late ones on top.
    state.todayGroups.forEach { (group, staff) ->
        item(key = "group-" + group.name) {
            ListCard(Modifier.padding(horizontal = 16.dp, vertical = 7.dp)) {
                GroupHeader(groupColor(group), stringResource(R.string.att_group_count, stringResource(todayGroupLabel(group)), staff.size))
                staff.forEach { f ->
                    HorizontalDivider(color = FieldBorder)
                    TodayRow(f, group, directory, state.freePeriodsOf(f.id), onClick = { onOpenStaff(f.id) }, onOpenFreePeriods = onOpenFreePeriods)
                }
            }
        }
    }
    item(key = "today-footer") { AttendanceFooter(Modifier.padding(top = 16.dp)) }
}

private fun groupColor(group: TodayGroup): Color = when (group) {
    TodayGroup.ABSENT -> BadgeRed
    TodayGroup.NOT_IN -> TextSecondary
    TodayGroup.ON_LEAVE -> OnboardingAmberRing
    TodayGroup.LATE -> OnboardingAmberRing
    TodayGroup.PRESENT -> AttGreen
}

@Composable
private fun TodayRow(f: Faculty, group: TodayGroup, directory: FacultyDirectory, freePeriods: Int, onClick: () -> Unit, onOpenFreePeriods: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(f.initials, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.name, Modifier.weight(1f, fill = false), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    TodayPill(group)
                }
                Text(
                    stringResource(R.string.att_dept_designation, directory.department(f.departmentId)?.name.orEmpty(), f.designation),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                    maxLines = 2,
                )
                Text(todayDetail(f, group), color = TextSecondary, fontSize = AppTextSize.Label, lineHeight = 18.sp)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        if (freePeriods > 0) {
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = onOpenFreePeriods,
                modifier = Modifier.fillMaxWidth().padding(start = 66.dp),
                shape = RoundedCornerShape(14.dp),
                color = AmberChipBackground,
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_clock), null, tint = OnboardingAmberCard, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        pluralStringResource(R.plurals.att_periods_free_today, freePeriods, freePeriods),
                        Modifier.weight(1f),
                        color = OnboardingAmberCard,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(painterResource(R.drawable.ic_chevron_right), null, tint = OnboardingAmberCard, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
internal fun TodayPill(group: TodayGroup) {
    when (group) {
        TodayGroup.ABSENT -> LabelPill(stringResource(R.string.att_status_absent), AlertBackground, BadgeRed, iconRes = R.drawable.ic_close)
        TodayGroup.NOT_IN -> LabelPill(stringResource(R.string.att_status_not_in_short), AttNeutralTile, TextSecondary, iconRes = R.drawable.ic_clock)
        TodayGroup.ON_LEAVE -> LabelPill(stringResource(R.string.att_status_on_leave), AmberChipBackground, OnboardingAmberCard, iconRes = R.drawable.ic_calendar)
        TodayGroup.LATE -> LabelPill(stringResource(R.string.att_status_late), AmberChipBackground, OnboardingAmberCard, iconRes = R.drawable.ic_clock)
        TodayGroup.PRESENT -> LabelPill(stringResource(R.string.att_status_present), OnboardingGreenDisc, AttGreen, iconRes = R.drawable.ic_check)
    }
}

/** "Not punched in by 09:15 AM", "In 09:05 AM · Out not yet · 2h 05m so far", "Leave approved · sick leave". */
@Composable
internal fun todayDetail(f: Faculty, group: TodayGroup): String = when (group) {
    TodayGroup.ABSENT -> stringResource(R.string.att_not_punched_by, timeOfDay(StaffRules.ABSENT_CUTOFF_MINUTE))
    TodayGroup.NOT_IN -> stringResource(R.string.att_no_punch_yet, timeOfDay(StaffRules.ABSENT_CUTOFF_MINUTE))
    TodayGroup.ON_LEAVE -> f.presence.leaveReason?.let { stringResource(R.string.att_leave_approved_reason, it) } ?: stringResource(R.string.att_leave_approved)
    TodayGroup.LATE, TodayGroup.PRESENT -> {
        val punch = f.presence.punchMinute ?: 0
        stringResource(R.string.att_in_out_so_far, timeOfDay(punch), soFar(punch))
    }
}

/** "2h 05m" since [punchMinute], by the device clock. */
@Composable
internal fun soFar(punchMinute: Int): String {
    val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
    val minutes = (now - punchMinute).coerceAtLeast(0)
    return stringResource(R.string.att_duration, minutes / 60, minutes % 60)
}

// ---- Staff-wise ----

private fun LazyListScope.staffWiseTab(
    state: StaffAttendanceUiState,
    directory: FacultyDirectory,
    vm: StaffAttendanceViewModel,
    onOpenStaff: (String) -> Unit,
) {
    item(key = "period") { PeriodPickers(state, vm) }
    if (state.noDataForPeriod) {
        item(key = "no-data") {
            MessageCard {
                StateMessage(
                    iconRes = R.drawable.ic_calendar,
                    iconTint = BrandBlue,
                    iconBackground = SelectedRowBackground,
                    title = stringResource(R.string.att_no_data_title),
                    subtitle = stringResource(R.string.att_no_data_vacation, monthYear(YearMonth(state.year, state.month ?: 5))),
                )
            }
        }
        item(key = "no-data-footer") { AttendanceFooter() }
        return
    }
    item(key = "wise-tiles") {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The long "Average attendance" label gets the widest tile so it never breaks mid-word.
            AttStatTile(percentLabel(state.averagePercent), stringResource(R.string.att_average_attendance), Modifier.weight(1.35f))
            AttStatTile(
                state.belowCount.toString(), stringResource(R.string.att_staff_below_75), Modifier.weight(1.1f), AlertBackground, BadgeRed,
                selected = state.filter.belowOnly,
            ) { vm.onBelowOnly(!state.filter.belowOnly) }
            AttStatTile(state.workingDays.toString(), stringResource(R.string.att_working_days), Modifier.weight(0.95f))
            AttStatTile(state.leaveDays.toString(), stringResource(R.string.att_leave_days), Modifier.weight(0.95f), AmberChipBackground, OnboardingAmberCard)
        }
    }
    searchAndFilters(state, directory, vm)
    item(key = "wise-chips") {
        if (state.filter.belowOnly || state.filter.hasNarrowing) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.filter.belowOnly) RemovableFilterChip(stringResource(R.string.att_below_75), { vm.onBelowOnly(false) })
                ResetAllLink(vm::onResetFilters)
            }
        }
    }
    item(key = "wise-count") {
        val period = state.month?.let { monthYear(YearMonth(state.year, it)) } ?: stringResource(R.string.att_all_months_year, state.year)
        CountRow(stringResource(R.string.att_staff_period_count, state.rows.size, period), enabled = state.rows.isNotEmpty(), onExport = vm::openExport)
    }
    if (state.rows.isEmpty()) {
        item(key = "wise-empty") { NoStaffMatch(vm::onResetFilters) }
    } else {
        item(key = "wise-list") {
            ListCard(Modifier.padding(horizontal = 16.dp, vertical = 7.dp)) {
                state.rows.forEachIndexed { index, (f, summary) ->
                    if (index > 0) HorizontalDivider(color = FieldBorder)
                    StaffWiseRow(f, summary, directory, onClick = { onOpenStaff(f.id) })
                }
            }
        }
    }
    item(key = "wise-footer") { AttendanceFooter(Modifier.padding(top = 16.dp)) }
}

/** Year and Month pickers; the current month reads "October (current)". */
@Composable
private fun PeriodPickers(state: StaffAttendanceUiState, vm: StaffAttendanceViewModel) {
    val allMonths = stringResource(R.string.att_all_months)
    val current = stringResource(R.string.att_month_current, monthName(YearMonth(state.today.year, state.today.month)))
    val monthLabels = listOf<Int?>(null) + state.months
    fun shown(month: Int?): String = when {
        month == null -> allMonths
        state.year == state.today.year && month == state.today.month -> current
        else -> monthName(YearMonth(state.year, month))
    }
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(0.42f)) {
            Text(stringResource(R.string.att_year), color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            DropdownField(state.year.toString(), state.years.map { it.toString() }, { vm.onYear(it.toInt()) })
        }
        Column(Modifier.weight(0.58f)) {
            Text(stringResource(R.string.att_month), color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            DropdownField(shown(state.month), monthLabels.map(::shown), { picked -> vm.onMonth(monthLabels[monthLabels.map(::shown).indexOf(picked)]) })
        }
    }
}

@Composable
private fun StaffWiseRow(f: Faculty, summary: StaffSummary, directory: FacultyDirectory, onClick: () -> Unit) {
    val level = levelOf(summary.percent)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        InitialsAvatar(f.initials, size = 52.dp, background = OnboardingGreenDisc, content = AttGreen)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.att_dept_designation, directory.department(f.departmentId)?.name.orEmpty(), f.designation),
                color = TextSecondary,
                fontSize = AppTextSize.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PercentBar((summary.percent / 100).toFloat(), levelColor(level), Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text(percentLabel(summary.percent), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
            }
            Row {
                CountLabel(stringResource(R.string.att_short_present), summary.present, AttGreen, Modifier.weight(1f))
                CountLabel(stringResource(R.string.att_short_absent), summary.absent, BadgeRed, Modifier.weight(1f))
                CountLabel(stringResource(R.string.att_short_leave), summary.leave, OnboardingAmberCard, Modifier.weight(1f))
                CountLabel(stringResource(R.string.att_status_late), summary.late, TextPrimary, Modifier.weight(1.2f))
            }
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

/** "P 5" with the number coloured. */
@Composable
private fun CountLabel(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = TextSecondary)) { append("$label ") }
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) { append(value.toString()) }
        },
        modifier,
        fontSize = AppTextSize.Body,
    )
}

// ---- Shared ----

private fun LazyListScope.searchAndFilters(state: StaffAttendanceUiState, directory: FacultyDirectory, vm: StaffAttendanceViewModel) {
    item(key = "search") {
        SearchBar(
            query = state.filter.query,
            onQueryChange = vm::onQuery,
            hint = stringResource(R.string.att_search_staff),
            containerColor = White,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp).border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
        )
    }
    item(key = "filters") {
        val all = stringResource(R.string.att_filter_all)
        val stream = directory.stream(state.filter.streamId)
        val department = directory.department(state.filter.departmentId)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilterDropdown(
                label = stringResource(R.string.att_filter_stream),
                value = stream?.name ?: all,
                options = listOf(all to { vm.onStream(null) }) + directory.streams.map { s -> s.name to { vm.onStream(s.id) } },
                selected = stream?.name ?: all,
                highlighted = stream != null,
            )
            FilterDropdown(
                label = stringResource(R.string.att_filter_department),
                value = department?.name ?: all,
                options = listOf(all to { vm.onDepartment(null) }) + directory.departmentsIn(state.filter.streamId).map { d -> d.name to { vm.onDepartment(d.id) } },
                selected = department?.name ?: all,
                highlighted = department != null,
            )
        }
    }
}

@Composable
private fun CountRow(text: String, enabled: Boolean, onExport: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
        ExportButton(onExport, enabled)
    }
}

@Composable
private fun NoStaffMatch(onReset: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(vertical = 28.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.att_no_staff_match), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.att_try_another), color = TextSecondary, fontSize = AppTextSize.Body)
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onReset, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, FieldBorder)) {
                Text(stringResource(R.string.att_reset_filters), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
            }
        }
    }
}
