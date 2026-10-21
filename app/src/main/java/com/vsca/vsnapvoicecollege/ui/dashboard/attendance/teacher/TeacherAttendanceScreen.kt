package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.teacher

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.dayMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveCard
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveStatus
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.RejectLeaveSheet
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.shortWeekdayDate
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.LoadState
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
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberRing
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The teacher's Attendance tab: classes to take for a day of the week, and leave requests to decide. */
@Composable
fun TeacherAttendanceScreen(
    onBack: () -> Unit,
    onTakeAttendance: (sessionId: String, day: Day) -> Unit,
    onOpenLeave: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherAttendanceViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    state.message?.let { message ->
        val text = when (message) {
            is TeacherMessage.Decided -> stringResource(if (message.approved) R.string.att_leave_approved_toast else R.string.att_leave_rejected_toast, message.name)
            TeacherMessage.Failed -> stringResource(R.string.att_send_failed)
        }
        LaunchedEffect(message) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.att_title),
                subtitle = stringResource(R.string.att_teacher_subtitle, TeacherRepository.TEACHER_NAME, TeacherRepository.TEACHER_DEPARTMENT),
                onBack = onBack,
            )
        }
        if (!state.isOnline && state.load == LoadState.READY) {
            item(key = "offline") { OfflineBanner(stringResource(R.string.att_teacher_offline), Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        }
        item(key = "tabs") {
            BadgeTabs(
                labels = listOf(stringResource(R.string.att_tab_classes), stringResource(R.string.att_tab_leave_requests)),
                badges = listOf(state.pendingToday, state.leaveCounts[LeaveStatus.PENDING] ?: 0),
                selected = state.tab.ordinal,
                onSelect = { viewModel.onTab(TeacherTab.entries[it]) },
            )
        }
        when {
            state.load == LoadState.LOADING -> item(key = "loading") { SkeletonCards(3, Modifier.padding(16.dp)) }
            state.load == LoadState.ERROR -> item(key = "error") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_wifi,
                        iconTint = BadgeRed,
                        iconBackground = AlertBackground,
                        title = stringResource(R.string.att_teacher_error_title),
                        subtitle = stringResource(R.string.att_error_body),
                    ) { RetryButton(viewModel::refresh) }
                }
            }
            state.tab == TeacherTab.CLASSES -> classesTab(state, viewModel, onTakeAttendance)
            else -> leavesTab(state, viewModel, onOpenLeave)
        }
    }
    state.rejecting?.let { request ->
        RejectLeaveSheet(request, state.deciding, onConfirm = viewModel::reject, onDismiss = viewModel::closeReject)
    }
}

/** Pill-shaped tabs with a count bubble ("Classes 2 | Leave requests 2"). */
@Composable
internal fun BadgeTabs(labels: List<String>, badges: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp), color = BadgeNeutralBackground) {
        Row(Modifier.padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) White else BadgeNeutralBackground,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                ) {
                    // The label gives way (ellipsis) so the count badge always stays visible in long languages.
                    Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label,
                            Modifier.weight(1f, fill = false),
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = AppTextSize.BodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        badges.getOrNull(index)?.takeIf { it > 0 }?.let { count ->
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.clip(RoundedCornerShape(50)).background(if (isSelected) BrandBlue else SelectedRowBackground).padding(horizontal = 9.dp, vertical = 1.dp)) {
                                Text(count.toString(), color = if (isSelected) White else BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Classes ----

private fun LazyListScope.classesTab(state: TeacherUiState, vm: TeacherAttendanceViewModel, onTake: (String, Day) -> Unit) {
    item(key = "week") { WeekStrip(state, vm) }
    item(key = "day-head") {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.selected == state.today) stringResource(R.string.att_today_date, shortWeekdayDate(state.selected).replace(",", "")) else shortWeekdayDate(state.selected),
                Modifier.weight(1f),
                color = TextPrimary,
                fontSize = AppTextSize.H6,
                fontWeight = FontWeight.ExtraBold,
            )
            if (state.pendingOnSelected > 0) LabelPill(stringResource(R.string.att_n_pending, state.pendingOnSelected), AmberChipBackground, OnboardingAmberCard)
        }
    }
    if (state.cards.isEmpty()) {
        item(key = "no-classes") {
            MessageCard {
                StateMessage(
                    iconRes = R.drawable.ic_calendar,
                    iconTint = BrandBlue,
                    iconBackground = SelectedRowBackground,
                    title = stringResource(R.string.att_no_classes_title),
                    subtitle = stringResource(R.string.att_no_classes_body),
                )
            }
        }
    }
    items(state.cards, key = { it.session.id }) { card -> SessionCardView(card, state.selected, onTake) }
}

@Composable
private fun WeekStrip(state: TeacherUiState, vm: TeacherAttendanceViewModel) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left), stringResource(R.string.att_previous_week), tint = TextPrimary,
                    modifier = Modifier.clip(CircleShape).clickable { vm.onWeek(-1) }.padding(8.dp).size(22.dp),
                )
                Text(
                    stringResource(R.string.att_span, dayMonth(state.weekStart), dayMonth(state.weekStart.plusDays(6)) + " " + state.weekStart.plusDays(6).year),
                    Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = AppTextSize.H6,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Icon(
                    painterResource(R.drawable.ic_chevron_right), stringResource(R.string.att_next_week), tint = TextPrimary,
                    modifier = Modifier.clip(CircleShape).clickable { vm.onWeek(1) }.padding(8.dp).size(22.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                state.weekDays.forEach { day ->
                    val selected = day == state.selected
                    Surface(onClick = { vm.onDay(day) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = if (selected) BrandBlue else White) {
                        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(SimpleDateFormat("EEE", Locale.getDefault()).format(Date(day.millis)), color = if (selected) White else TextSecondary, fontSize = AppTextSize.Label, maxLines = 1)
                            Text(day.day.toString(), color = if (selected) White else TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                            val dot = state.dots[day] ?: DayDot.NONE
                            Box(
                                Modifier.size(6.dp).clip(CircleShape).background(
                                    when {
                                        dot == DayDot.NONE -> Color.Transparent
                                        selected -> White
                                        dot == DayDot.DONE -> AttGreen
                                        else -> TextMuted
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SessionCardView(card: SessionCard, day: Day, onTake: (String, Day) -> Unit) {
    val s = card.session
    val c = card.reportClass
    val open = card.state == SessionState.PENDING || card.state == SessionState.MISSED
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, if (open) OnboardingAmberRing else FieldBorder),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(SelectedRowBackground), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_book), null, tint = BrandBlue, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.subject, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                    Text(c?.let { "${it.programme} · Sem ${it.division.semester} ${it.division.section}" }.orEmpty(), color = TextSecondary, fontSize = AppTextSize.Body)
                    Text(stringResource(R.string.att_room_students, s.room, c?.students?.size ?: 0), color = TextSecondary, fontSize = AppTextSize.Label)
                }
                LabelPill(stringResource(R.string.att_period_time, s.period, timeOfDay(s.startMinute)), SelectedRowBackground, BrandBlue)
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = FieldBorder)
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (card.state) {
                    SessionState.TAKEN -> LabelPill(
                        stringResource(R.string.att_taken, card.record?.present ?: 0, c?.students?.size ?: 0),
                        OnboardingGreenDisc,
                        AttGreen,
                    )
                    SessionState.PENDING -> LabelPill(stringResource(R.string.att_pending), AmberChipBackground, OnboardingAmberCard)
                    SessionState.MISSED -> LabelPill(stringResource(R.string.att_not_taken), AlertBackground, BadgeRed)
                    SessionState.UPCOMING -> LabelPill(stringResource(R.string.att_upcoming), BadgeNeutralBackground, TextSecondary)
                }
                Spacer(Modifier.weight(1f))
                when {
                    card.state == SessionState.TAKEN && card.canEdit -> OutlinedButton(
                        onClick = { onTake(s.id, day) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, FieldBorder),
                    ) { Text(stringResource(R.string.att_edit), color = BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold) }
                    open -> Button(
                        onClick = { onTake(s.id, day) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_calendar), null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.att_take_attendance), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---- Leave requests ----

private fun LazyListScope.leavesTab(state: TeacherUiState, vm: TeacherAttendanceViewModel, onOpenLeave: (String) -> Unit) {
    item(key = "leave-filters") {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                LeaveStatus.PENDING to R.string.att_leave_pending,
                LeaveStatus.APPROVED to R.string.att_leave_approved_chip,
                LeaveStatus.REJECTED to R.string.att_leave_rejected,
            ).forEach { (status, label) ->
                val selected = state.leaveFilter == status
                Surface(
                    onClick = { vm.onLeaveFilter(status) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) BrandBlue else White,
                    border = if (selected) null else BorderStroke(1.dp, FieldBorder),
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(label), color = if (selected) White else TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) White.copy(alpha = 0.25f) else SelectedRowBackground).padding(horizontal = 8.dp)) {
                            Text((state.leaveCounts[status] ?: 0).toString(), color = if (selected) White else BrandBlue, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    if (state.leaves.isEmpty()) {
        item(key = "no-leaves") {
            MessageCard {
                StateMessage(
                    iconRes = R.drawable.ic_check,
                    iconTint = AttGreen,
                    iconBackground = OnboardingGreenDisc,
                    title = stringResource(R.string.att_no_leaves_title),
                    subtitle = stringResource(R.string.att_no_leaves_body),
                )
            }
        }
    }
    items(state.leaves, key = { it.id }) { request ->
        LeaveCard(
            request = request,
            onOpen = { onOpenLeave(request.id) },
            onApprove = { vm.approve(request) },
            onReject = { vm.openReject(request) },
            busy = state.deciding,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
        )
    }
}
