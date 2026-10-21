package com.vsca.vsnapvoicecollege.ui.dashboard.fees

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.daysFromToday
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Deep blue of the summary card, shared with the credits summary. */
internal val FeeHeroBlue = Color(0xFF1E42B4)

/** Darker green of the Pay / Submit buttons. */
internal val PayGreen = Color(0xFF17733F)

/** "15 Oct". */
internal fun dayMonth(millis: Long): String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))

/** "05 Oct 2026". */
internal fun dayMonthYear(millis: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

/** Amount due, the due chip, the % paid ring and Total / Paid / In review. */
@Composable
internal fun FeeSummaryCard(account: FeeAccount, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = FeeHeroBlue) {
        Box {
            // Soft circle behind the ring, as in the design.
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-70).dp).size(240.dp).clip(CircleShape)
                    .background(White.copy(alpha = 0.07f)),
            )
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.fee_amount_due), color = White.copy(alpha = 0.9f), fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(rupees(account.amountDue), color = White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 46.sp, maxLines = 1)
                        Spacer(Modifier.height(8.dp))
                        DueChip(account)
                    }
                    Spacer(Modifier.width(12.dp))
                    PaidRing(account.percentPaid)
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryTile(stringResource(R.string.fee_total), rupees(account.total), Modifier.weight(1f))
                    SummaryTile(stringResource(R.string.fee_paid), rupees(account.paid), Modifier.weight(1f))
                    SummaryTile(stringResource(R.string.fee_in_review), rupees(account.inReview), Modifier.weight(1f))
                }
            }
        }
    }
}

/** "Due in 2 days · 15 Oct", "Due today", "Overdue by 3 days", or "All fees paid". */
@Composable
private fun DueChip(account: FeeAccount) {
    val due = account.nextDueAt
    val (text, background, content) = when {
        due == null || account.amountDue == 0L -> Triple(stringResource(R.string.fee_all_paid), OnboardingGreenDisc, BrandGreen)
        else -> {
            val days = daysFromToday(due)
            when {
                days < 0 -> Triple(pluralStringResource(R.plurals.fee_overdue_by, -days, -days, dayMonth(due)), AlertBackground, BadgeRed)
                days == 0 -> Triple(stringResource(R.string.fee_due_today, dayMonth(due)), AmberChipBackground, OnboardingAmberCard)
                else -> Triple(pluralStringResource(R.plurals.fee_due_in, days, days, dayMonth(due)), AmberChipBackground, OnboardingAmberCard)
            }
        }
    }
    Surface(shape = RoundedCornerShape(50), color = background) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = content, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun PaidRing(percent: Int) {
    Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(100.dp)) {
            val stroke = 12.dp.toPx()
            drawArc(White.copy(alpha = 0.25f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(stroke))
            drawArc(White, startAngle = -90f, sweepAngle = 360f * percent / 100f, useCenter = false, style = Stroke(stroke, cap = StrokeCap.Butt))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.fee_percent, percent), color = White, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
            Text(stringResource(R.string.fee_paid_lower), color = White.copy(alpha = 0.85f), fontSize = AppTextSize.Label, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = White.copy(alpha = 0.12f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = White.copy(alpha = 0.85f), fontSize = AppTextSize.Label, maxLines = 1)
            Text(value, color = White, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

/** Rounded icon tile for a fee head. */
@Composable
internal fun FeeIconTile(category: FeeCategory) {
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(category.background), contentAlignment = Alignment.Center) {
        Icon(painterResource(category.icon), contentDescription = null, tint = category.content, modifier = Modifier.size(24.dp))
    }
}

/** Paid / In review / Due / Overdue pill on the fee structure. */
@Composable
internal fun FeeStatePill(state: FeeState) {
    when (state) {
        FeeState.PAID -> LabelPill(stringResource(R.string.fee_state_paid), OnboardingGreenDisc, BrandGreen)
        FeeState.IN_REVIEW -> LabelPill(stringResource(R.string.fee_state_in_review), AmberChipBackground, OnboardingAmberCard)
        FeeState.DUE -> LabelPill(stringResource(R.string.fee_state_due), AmberChipBackground, OnboardingAmberCard)
        FeeState.OVERDUE -> LabelPill(stringResource(R.string.fee_state_overdue), AlertBackground, BadgeRed)
    }
}

/** Verified / In review / Sent back pill on a payment. */
@Composable
internal fun PaymentStatusPill(status: PaymentStatus) {
    when (status) {
        PaymentStatus.VERIFIED -> LabelPill(stringResource(R.string.fee_payment_verified), OnboardingGreenDisc, BrandGreen)
        PaymentStatus.IN_REVIEW -> LabelPill(stringResource(R.string.fee_state_in_review), AmberChipBackground, OnboardingAmberCard)
        PaymentStatus.REJECTED -> LabelPill(stringResource(R.string.fee_payment_rejected), AlertBackground, BadgeRed)
    }
}
