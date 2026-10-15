package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Punch-in cut-off: staff not in by then are marked absent. */
private const val PUNCH_CUTOFF_MINUTE = 9 * 60 + 15

/**
 * Free periods today: who is away, which periods they leave uncovered, and the
 * substitutes allocated to them. Ended periods are locked.
 */
@Composable
fun FreePeriodsScreen(
    onBack: () -> Unit,
    onOpenFaculty: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPunchIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FreePeriodsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val directory = uiState.directory
    val now = FacultyRepository.NOW_MINUTE

    val allocatedMessage = uiState.notice?.let { n ->
        if (n.teacherName != null) stringResource(R.string.faculty_allocated_toast, n.period, n.teacherName)
        else stringResource(R.string.faculty_unallocated_toast, n.period)
    }
    LaunchedEffect(allocatedMessage) {
        if (allocatedMessage != null) {
            Toast.makeText(context, allocatedMessage, Toast.LENGTH_SHORT).show()
            viewModel.onNoticeShown()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(ScreenBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.faculty_free_title),
                subtitle = stringResource(R.string.faculty_free_subtitle, SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())),
                onBack = onBack,
                trailing = { SquareIconButton(R.drawable.ic_notifications, stringResource(R.string.faculty_notifications), onOpenNotifications) },
            )
        }
        when (uiState.load) {
            LoadState.LOADING -> item(key = "loading") { LoadingPlaceholder() }
            LoadState.ERROR -> item(key = "error") { LoadError(viewModel::load) }
            LoadState.READY -> if (directory != null) {
                if (!uiState.isOnline) {
                    item(key = "offline") { OfflineBanner(stringResource(R.string.faculty_free_offline), Modifier.padding(horizontal = 16.dp)) }
                }
                item(key = "stats") {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(directory.absentWithPeriods.size, stringResource(R.string.faculty_staff_absent), Modifier.weight(1f), background = Tiles.awayBg, valueColor = BadgeRed)
                        StatTile(uiState.periods.size, stringResource(R.string.faculty_stat_free_periods), Modifier.weight(1f))
                        StatTile(
                            uiState.unallocated, stringResource(R.string.faculty_still_unallocated), Modifier.weight(1f),
                            background = Tiles.amberBg, valueColor = OnboardingAmberCard, labelColor = OnboardingAmberCard, border = Tiles.amberBorder,
                        )
                    }
                }
                item(key = "absent") { AbsentTodayCard(directory, onOpenPunchIn, onOpenFaculty) }
                item(key = "tabs") {
                    SegmentedControl(
                        options = listOf(
                            Segment(stringResource(R.string.faculty_tab_all, uiState.periods.size)),
                            Segment(stringResource(R.string.faculty_tab_unallocated, uiState.unallocated)),
                            Segment(stringResource(R.string.faculty_tab_allocated, uiState.allocated)),
                        ),
                        selectedIndex = uiState.tab.ordinal,
                        onSelect = { viewModel.onTab(FreeTab.entries[it]) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        height = 52.dp,
                    )
                }
                item(key = "classes") {
                    val all = stringResource(R.string.faculty_all_classes)
                    val classes = uiState.periods.mapNotNull { directory.division(it.divisionId) }.distinct()
                    val selected = directory.division(uiState.divisionId)?.let { stringResource(R.string.faculty_class_label, it.programme, it.semester, it.section) }
                    FilterDropdown(
                        label = null,
                        value = selected ?: all,
                        selected = selected ?: all,
                        options = listOf<Pair<String, () -> Unit>>(all to { viewModel.onDivision(null) }) +
                            classes.map { d -> stringResource(R.string.faculty_class_label, d.programme, d.semester, d.section) to { viewModel.onDivision(d.id) } },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        highlighted = selected != null,
                    )
                }
                val shown = uiState.shown
                if (shown.isEmpty()) {
                    item(key = "none") {
                        FacultyCard(Modifier.padding(horizontal = 16.dp)) {
                            StateMessage(
                                iconRes = R.drawable.ic_check,
                                iconTint = PresentGreen,
                                iconBackground = PresentBackground,
                                title = stringResource(R.string.faculty_free_none_title),
                                subtitle = stringResource(R.string.faculty_free_none_body),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                            )
                        }
                    }
                } else {
                    items(shown, key = { it.id }) { period ->
                        PeriodCard(period, directory, now, viewModel.canAllocate, onAllocate = { viewModel.onAllocate(period) }, onOpenFaculty = onOpenFaculty)
                    }
                }
            }
        }
    }

    val allocating = uiState.allocating
    if (allocating != null && directory != null) {
        AllocateSheet(allocating, directory, onPick = viewModel::onPickSubstitute, onDismiss = viewModel::onDismissAllocate)
    }
}

@Composable
private fun AbsentTodayCard(directory: FacultyDirectory, onOpenPunchIn: () -> Unit, onOpenFaculty: (String) -> Unit) {
    FacultyCard(Modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.faculty_absent_today), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenPunchIn).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_fingerprint), null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.faculty_staff_punch_in), color = BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            directory.absentWithPeriods.forEach { f ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpenFaculty(f.id) }, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(AlertBackground), contentAlignment = Alignment.Center) {
                        Text(f.initials, color = BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                        Text(absenceReason(f, directory), color = TextSecondary, fontSize = AppTextSize.Body)
                    }
                    Text(
                        directory.freePeriodsCreatedBy(f.id).size.let { pluralStringResource(R.plurals.faculty_periods_count, it, it) },
                        color = TextPrimary,
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            val late = FacultyRepository.lateArrival
            val lateFaculty = directory.faculty(late.facultyId)
            val keptWith = directory.faculty(late.keptWithId)
            val punch = lateFaculty?.presence?.punchMinute
            if (lateFaculty != null && keptWith != null && punch != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = PresentBackground) {
                    Row(Modifier.padding(14.dp)) {
                        Icon(painterResource(R.drawable.ic_check), null, tint = PresentGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.faculty_late_note, lateFaculty.name, timeOfDay(punch), late.returnedPeriod, late.keptPeriod, keptWith.name),
                            color = PresentGreen,
                            fontSize = AppTextSize.Body,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
        }
    }
}

/** "IT · not punched in by 09:15 AM" / "CSE · on sick leave (approved)". */
@Composable
internal fun absenceReason(f: Faculty, directory: FacultyDirectory): String {
    val dept = directory.department(f.departmentId)?.name.orEmpty()
    return when (f.presence.status) {
        PresenceStatus.ON_LEAVE -> stringResource(R.string.faculty_reason_leave, dept, f.presence.leaveReason ?: stringResource(R.string.faculty_leave_default))
        else -> stringResource(R.string.faculty_reason_not_in, dept, timeOfDay(PUNCH_CUTOFF_MINUTE))
    }
}

@Composable
private fun PeriodCard(
    period: FreePeriod,
    directory: FacultyDirectory,
    now: Int,
    canAllocate: Boolean,
    onAllocate: () -> Unit,
    onOpenFaculty: (String) -> Unit,
) {
    val timing = period.timing(now)
    val allocatedTo = directory.faculty(period.allocatedToId)
    val absent = directory.faculty(period.absentFacultyId)
    val division = directory.division(period.divisionId)
    val unallocated = allocatedTo == null
    val (badgeBg, badgeFg) = when {
        unallocated -> Tiles.amberBg to OnboardingAmberCard
        else -> PresentBackground to PresentGreen
    }
    FacultyCard(Modifier.padding(horizontal = 16.dp), border = if (unallocated && timing != PeriodTiming.ENDED) Tiles.amberBorder else FieldBorder) {
        Column(Modifier.padding(18.dp)) {
            Row {
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(badgeBg), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.faculty_period_short, period.period), color = badgeFg, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    val range = timeRange(period.slot.startMinute, period.slot.endMinute)
                    Text(
                        when (timing) {
                            PeriodTiming.IN_PROGRESS -> stringResource(R.string.faculty_in_progress, range)
                            else -> range
                        },
                        color = TextSecondary,
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(period.subject, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                    if (division != null) {
                        Text(
                            stringResource(R.string.faculty_class_room, division.programme, division.semester, division.section, period.room),
                            color = TextSecondary,
                            fontSize = AppTextSize.Body,
                        )
                    }
                    if (absent != null) {
                        Row(Modifier.padding(top = 2.dp).clickable { onOpenFaculty(absent.id) }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_person_off), null, tint = BadgeRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.faculty_absent_name, absent.name), color = BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (allocatedTo != null) {
                LabelPill(
                    stringResource(R.string.faculty_allocated_to, allocatedTo.name),
                    if (timing == PeriodTiming.ENDED) BadgeNeutralBackground else PresentBackground,
                    if (timing == PeriodTiming.ENDED) TextSecondary else PresentGreen,
                    iconRes = R.drawable.ic_clock,
                )
            } else {
                LabelPill(stringResource(R.string.faculty_unallocated), Tiles.amberBg, OnboardingAmberCard, iconRes = R.drawable.ic_info_outline)
            }
            when {
                timing == PeriodTiming.ENDED -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(if (allocatedTo != null) R.string.faculty_period_ended else R.string.faculty_period_ended_uncovered),
                        color = TextSecondary,
                        fontSize = AppTextSize.Body,
                    )
                }
                canAllocate -> {
                    Spacer(Modifier.height(12.dp))
                    if (allocatedTo == null) {
                        Button(
                            onClick = onAllocate,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                        ) { Text(stringResource(R.string.faculty_allocate), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold) }
                    } else {
                        OutlinedButton(
                            onClick = onAllocate,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, FieldBorder),
                        ) { Text(stringResource(R.string.faculty_change_substitute), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllocateSheet(period: FreePeriod, directory: FacultyDirectory, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val candidates = FacultyRepository.freeTeachersFor(period)
    var choice by remember(period.id) { mutableStateOf(period.allocatedToId) }
    val division = directory.division(period.divisionId)
    FacultySheet(onDismiss) {
        SheetHeader(
            stringResource(R.string.faculty_allocate_title, period.period, period.subject),
            division?.let { stringResource(R.string.faculty_allocate_sub, it.programme, it.semester, it.section, timeRange(period.slot.startMinute, period.slot.endMinute)) },
            onDismiss,
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.faculty_allocate_hint), color = TextSecondary, fontSize = AppTextSize.Body)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            candidates.forEach { f ->
                RadioCard(
                    f.name,
                    stringResource(R.string.faculty_candidate_sub, directory.department(f.departmentId)?.name.orEmpty(), f.designation),
                    selected = choice == f.id,
                    onClick = { choice = f.id },
                )
            }
            if (period.allocatedToId != null) {
                HorizontalDivider(color = FieldBorder)
                RadioCard(stringResource(R.string.faculty_leave_unallocated), stringResource(R.string.faculty_leave_unallocated_sub), selected = choice == null, onClick = { choice = null })
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onPick(choice) },
            enabled = choice != period.allocatedToId,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White, disabledContainerColor = SelectedRowBackground, disabledContentColor = TextSecondary),
        ) {
            Text(
                if (choice == null && period.allocatedToId != null) stringResource(R.string.faculty_remove_substitute)
                else stringResource(R.string.faculty_allocate_notify),
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
