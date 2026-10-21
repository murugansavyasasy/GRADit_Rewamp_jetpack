package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.dayMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultySheet
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.InitialsAvatar
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.DestructiveRed
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Applied today, 08:12 AM" / "Applied 05 Oct, 07:40 PM". */
@Composable
fun appliedLabel(millis: Long): String = if (isDaysAgo(millis, 0)) {
    stringResource(R.string.att_applied_today, clockLabel(millis))
} else {
    stringResource(R.string.att_applied_on, SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis)) + ", " + clockLabel(millis))
}

@Composable
fun LeaveStatusPill(status: LeaveStatus) {
    when (status) {
        LeaveStatus.PENDING -> LabelPill(stringResource(R.string.att_leave_pending), AmberChipBackground, OnboardingAmberCard)
        LeaveStatus.APPROVED -> LabelPill(stringResource(R.string.att_leave_approved_chip), OnboardingGreenDisc, AttGreen)
        LeaveStatus.REJECTED -> LabelPill(stringResource(R.string.att_leave_rejected), AlertBackground, BadgeRed)
    }
}

/** From / To / Days tiles. */
@Composable
fun LeaveDates(request: LeaveRequest) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DateTile(stringResource(R.string.att_from), dayMonth(request.from), Modifier.weight(1f))
        DateTile(stringResource(R.string.att_to), dayMonth(request.to), Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(16.dp), color = SelectedRowBackground) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.att_days), color = BrandBlue, fontSize = AppTextSize.Label)
                Text(request.days.toString(), color = BrandBlue, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun DateTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = ScreenBackground) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, color = TextSecondary, fontSize = AppTextSize.Label)
            Text(value, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** A request in the advisor's list: who, type, dates, reason, and Reject / Approve while pending. */
@Composable
fun LeaveCard(
    request: LeaveRequest,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    busy: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(request.initials, size = 48.dp, background = OnboardingGreenDisc, content = AttGreen)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.studentName, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.att_class_reg, request.classLabel, request.registerNo), color = TextSecondary, fontSize = AppTextSize.Label)
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelPill(stringResource(request.type.label), SelectedRowBackground, BrandBlue)
                Spacer(Modifier.width(10.dp))
                Text(appliedLabel(request.appliedAt), color = TextSecondary, fontSize = AppTextSize.Label)
            }
            Spacer(Modifier.height(10.dp))
            LeaveDates(request)
            Spacer(Modifier.height(10.dp))
            Text(request.reason, color = TextPrimary, fontSize = AppTextSize.Body, lineHeight = 21.sp, maxLines = 3)
            Spacer(Modifier.height(12.dp))
            when (request.status) {
                LeaveStatus.PENDING -> DecisionButtons(busy, onReject, onApprove)
                LeaveStatus.APPROVED -> LeaveStatusPill(LeaveStatus.APPROVED)
                LeaveStatus.REJECTED -> Column {
                    LeaveStatusPill(LeaveStatus.REJECTED)
                    request.decisionNote?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.att_reject_reason_shown, it), color = BadgeRed, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Outlined red "Reject" and blue "Approve". */
@Composable
fun DecisionButtons(busy: Boolean, onReject: () -> Unit, onApprove: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onReject,
            enabled = !busy,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DestructiveRed),
        ) {
            Text(stringResource(R.string.att_reject), color = DestructiveRed, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onApprove,
            enabled = !busy,
            modifier = Modifier.weight(1.1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.ic_check), null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.att_approve), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** "Reject leave request?": a required reason (the family sees it) with quick picks. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RejectLeaveSheet(request: LeaveRequest, busy: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable(request.id) { mutableStateOf("") }
    val quick = listOf(R.string.att_quick_exam_week, R.string.att_quick_certificate, R.string.att_quick_incomplete).map { stringResource(it) }
    FacultySheet(onDismiss) {
        Text(stringResource(R.string.att_reject_title), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
        Text(
            stringResource(R.string.att_reject_subtitle, request.studentName, stringResource(request.type.label), leaveSpan(request)),
            color = TextSecondary,
            fontSize = AppTextSize.Body,
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Text(stringResource(R.string.att_reject_reason), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            Text(" *", color = BadgeRed, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it.take(MAX_REASON) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            placeholder = { Text(stringResource(R.string.att_reject_hint), color = TextSecondary) },
            shape = RoundedCornerShape(16.dp),
            colors = appTextFieldColors(),
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.att_reject_visible), color = TextSecondary, fontSize = AppTextSize.Label)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.att_quick_reasons), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            quick.forEach { option ->
                val picked = reason == option
                Surface(
                    onClick = { reason = option },
                    shape = RoundedCornerShape(50),
                    color = if (picked) BrandBlue else White,
                    border = if (picked) null else BorderStroke(1.dp, FieldBorder),
                ) {
                    Text(option, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = if (picked) White else TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { onConfirm(reason) },
            enabled = reason.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = White),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.att_confirm_reject), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            stringResource(R.string.att_cancel),
            Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(vertical = 16.dp),
            color = BrandBlue,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** "07–08 Oct" or "09 Oct". */
@Composable
fun leaveSpan(request: LeaveRequest): String =
    if (request.from == request.to) dayMonth(request.from) else stringResource(R.string.att_day_range, request.from.day.toString().padStart(2, '0'), dayMonth(request.to))

private const val MAX_REASON = 250
