package com.vsca.vsnapvoicecollege.ui.dashboard.fees

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.ConfirmDialog
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.Segment
import com.vsca.vsnapvoicecollege.ui.components.SegmentedControl
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.MessageCard
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.RetryButton
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.syncedLabel
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Fee details for parents and students: what's due, the fee structure and payments made. */
@Composable
fun FeeDetailsScreen(
    onBack: () -> Unit,
    onUploadProof: (studentId: String, feeId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeeDetailsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmPay by rememberSaveable { mutableStateOf(false) }

    uiState.message?.let { message ->
        val text = when (message) {
            is FeeMessage.Paid -> stringResource(R.string.fee_pay_done, rupees(message.amount))
            FeeMessage.PayFailed -> stringResource(R.string.fee_pay_failed)
        }
        LaunchedEffect(message) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    FeeDetailsContent(
        uiState = uiState,
        onBack = onBack,
        onStudent = viewModel::onStudent,
        onTab = viewModel::onTab,
        onToggleFee = viewModel::onToggleFee,
        onRetry = viewModel::refresh,
        onDownload = { account -> FeeStatement.download(context, account) },
        onUploadProof = { account ->
            // Start the form on the first ticked fee, or the first one that can be paid.
            val feeId = account.payableFees().firstOrNull { it.id in uiState.selected }?.id
            onUploadProof(account.studentId, feeId)
        },
        onPay = { confirmPay = true },
        modifier = modifier,
    )

    val account = uiState.account
    if (confirmPay && account != null) {
        val fees = account.fees.filter { it.id in uiState.selected }
        ConfirmDialog(
            title = stringResource(R.string.fee_pay_confirm_title, rupees(uiState.payAmount)),
            body = stringResource(
                R.string.fee_pay_confirm_body,
                joinLabels(fees.map { stringResource(it.category.label) }),
                account.firstName,
            ),
            confirm = stringResource(R.string.fee_pay_confirm),
            destructive = false,
            onDismiss = { confirmPay = false },
            onConfirm = {
                confirmPay = false
                viewModel.pay()
            },
        )
    }
}

@Composable
private fun FeeDetailsContent(
    uiState: FeeDetailsUiState,
    onBack: () -> Unit,
    onStudent: (String) -> Unit,
    onTab: (FeeTab) -> Unit,
    onToggleFee: (String) -> Unit,
    onRetry: () -> Unit,
    onDownload: (FeeAccount) -> Unit,
    onUploadProof: (FeeAccount) -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = uiState.account
    val header = account ?: uiState.accounts.firstOrNull()
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
            item(key = "header") {
                TitledTopBar(
                    title = stringResource(R.string.fee_title),
                    subtitle = header?.let { stringResource(R.string.fee_subtitle, it.name, it.programme, it.semester, it.academicYear) },
                    onBack = onBack,
                    trailing = {
                        SquareIconButton(
                            iconRes = R.drawable.ic_download,
                            contentDescription = stringResource(R.string.fee_download_statement),
                            onClick = { account?.let(onDownload) },
                        )
                    },
                )
            }
            if (!uiState.isOnline && uiState.load != FeeLoad.ERROR) {
                item(key = "offline") {
                    OfflineBanner(
                        stringResource(R.string.fee_offline_banner, syncedLabel(uiState.lastSyncedAt)),
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
            if (uiState.showChildSwitcher) {
                item(key = "children") { ChildSwitcher(uiState.accounts, header?.studentId, onStudent) }
            }
            when {
                uiState.load == FeeLoad.LOADING -> item(key = "loading") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(26.dp)).background(FieldBorder))
                        SkeletonCards(2)
                    }
                }
                uiState.load == FeeLoad.ERROR || account == null -> item(key = "error") {
                    MessageCard {
                        StateMessage(
                            iconRes = R.drawable.ic_wifi,
                            iconTint = BadgeRed,
                            iconBackground = AlertBackground,
                            title = stringResource(R.string.fee_error_title),
                            subtitle = stringResource(R.string.fee_error_body),
                        ) { RetryButton(onRetry) }
                    }
                }
                else -> {
                    item(key = "summary") { FeeSummaryCard(account, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                    item(key = "tabs") {
                        SegmentedControl(
                            options = FeeTab.entries.map { Segment(stringResource(it.label)) },
                            selectedIndex = uiState.tab.ordinal,
                            onSelect = { onTab(FeeTab.entries[it]) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            height = 52.dp,
                        )
                    }
                    when (uiState.tab) {
                        FeeTab.DUE -> dueTab(account, uiState.selected, onToggleFee)
                        FeeTab.STRUCTURE -> item(key = "structure") { FeeStructureCard(account) }
                        FeeTab.PAYMENTS -> paymentsTab(account)
                    }
                    item(key = "footer") {
                        PoweredByRow(Modifier.fillMaxWidth().padding(top = 20.dp), textColor = TextSecondary)
                    }
                }
            }
        }
        if (account != null && uiState.tab == FeeTab.DUE && account.amountDue > 0) {
            PayBar(
                payAmount = uiState.payAmount,
                canUpload = account.payableFees().isNotEmpty(),
                isPaying = uiState.isPaying,
                onUpload = { onUploadProof(account) },
                onPay = onPay,
            )
        }
    }
}

@Composable
private fun ChildSwitcher(accounts: List<FeeAccount>, selectedId: String?, onSelect: (String) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        accounts.forEach { account ->
            val isSelected = account.studentId == selectedId
            Surface(
                onClick = { onSelect(account.studentId) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                color = if (isSelected) SelectedRowBackground else White,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandBlue else FieldBorder),
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (isSelected) BrandBlue else BadgeNeutralBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(account.initials, color = if (isSelected) White else TextPrimary, fontSize = AppTextSize.Caption, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(account.firstName, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            if (account.amountDue > 0) stringResource(R.string.fee_child_due, rupees(account.amountDue)) else stringResource(R.string.fee_all_paid),
                            color = if (account.amountDue > 0) OnboardingAmberCard else BrandGreen,
                            fontSize = AppTextSize.Caption,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ---- Due ----

private fun LazyListScope.dueTab(
    account: FeeAccount,
    selected: Set<String>,
    onToggleFee: (String) -> Unit,
) {
    val unpaid = account.unpaidFees
    if (unpaid.isEmpty()) {
        item(key = "all-paid") {
            MessageCard {
                StateMessage(
                    iconRes = R.drawable.ic_check,
                    iconTint = BrandGreen,
                    iconBackground = OnboardingGreenDisc,
                    title = stringResource(R.string.fee_nothing_due_title),
                    subtitle = stringResource(R.string.fee_nothing_due_body, account.academicYear),
                )
            }
        }
    }
    items(unpaid, key = { "due-" + it.id }) { fee ->
        DueFeeCard(account, fee, fee.id in selected, onToggle = { onToggleFee(fee.id) })
    }
    if (unpaid.isNotEmpty() && account.paidFees.isNotEmpty()) {
        item(key = "paid-note") { PaidNote(account) }
    }
}

@Composable
private fun DueFeeCard(account: FeeAccount, fee: FeeItem, selected: Boolean, onToggle: () -> Unit) {
    val state = account.stateOf(fee)
    val proof = account.pendingProof(fee.id)
    val rejected = account.rejectedProof(fee.id)
    val selectable = proof == null
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(if (selectable && selected) 2.dp else 1.dp, if (selectable && selected) BrandBlue else FieldBorder),
    ) {
        Column(Modifier.padding(16.dp)) {
            FeeRow(fee, account.balanceOf(fee), dueLine(fee, state))
            Spacer(Modifier.height(12.dp))
            when {
                proof != null -> NoteBox(
                    R.drawable.ic_clock,
                    stringResource(R.string.fee_proof_in_review, proof.reference),
                    AmberChipBackground,
                    OnboardingAmberCard,
                )
                else -> {
                    if (rejected != null) {
                        NoteBox(
                            R.drawable.ic_info_outline,
                            if (rejected.note != null) {
                                stringResource(R.string.fee_proof_rejected_reason, rejected.note)
                            } else {
                                stringResource(R.string.fee_proof_rejected)
                            },
                            AlertBackground,
                            BadgeRed,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    IncludeRow(selected, onToggle)
                }
            }
        }
    }
}

/** "Term 1 · due 15 Oct", or "… · overdue since 15 Oct". */
@Composable
private fun dueLine(fee: FeeItem, state: FeeState): String =
    if (state == FeeState.OVERDUE) {
        stringResource(R.string.fee_overdue_since, fee.period, dayMonth(fee.dueAt))
    } else {
        stringResource(R.string.fee_due_on, fee.period, dayMonth(fee.dueAt))
    }

/** Icon tile, fee name with a detail line, and the amount on the right. */
@Composable
private fun FeeRow(fee: FeeItem, amount: Long, detail: String, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FeeIconTile(fee.category)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(fee.category.label), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
            Text(detail, color = TextSecondary, fontSize = AppTextSize.Label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(rupees(amount), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
            trailing()
        }
    }
}

@Composable
private fun NoteBox(iconRes: Int, text: String, background: Color, content: Color) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = background) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(iconRes), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = content, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun IncludeRow(selected: Boolean, onToggle: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = ScreenBackground) {
        Row(Modifier.clickable(onClick = onToggle).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = BrandBlue, uncheckedColor = TextSecondary, checkmarkColor = White),
            )
            Text(stringResource(R.string.fee_include_payment), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** "✓ Tuition ₹85,000 and Transport ₹13,000 are paid for this year." */
@Composable
private fun PaidNote(account: FeeAccount) {
    val paid = account.paidFees
    val list = joinLabels(paid.map { stringResource(R.string.fee_name_amount, stringResource(it.category.label), rupees(it.amount)) })
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = OnboardingGreenDisc,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = BrandGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(if (paid.size == 1) R.string.fee_paid_note_one else R.string.fee_paid_note_many, list),
                color = BrandGreen,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
            )
        }
    }
}

/** "A", "A and B", "A, B and C" in the app language. */
@Composable
internal fun joinLabels(labels: List<String>): String = when (labels.size) {
    0 -> ""
    1 -> labels[0]
    else -> stringResource(R.string.fee_list_and, labels.dropLast(1).joinToString(", "), labels.last())
}

/** Upload proof (outlined) and Pay ₹… → (green), pinned above the bottom bar. */
@Composable
private fun PayBar(payAmount: Long, canUpload: Boolean, isPaying: Boolean, onUpload: () -> Unit, onPay: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = White, shadowElevation = 8.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onUpload,
                enabled = canUpload,
                modifier = Modifier.weight(0.42f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FieldBorder),
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Icon(painterResource(R.drawable.ic_upload), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.fee_upload_proof), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onPay,
                enabled = payAmount > 0 && !isPaying,
                modifier = Modifier.weight(0.58f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PayGreen, contentColor = White),
            ) {
                if (isPaying) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (payAmount > 0) stringResource(R.string.fee_pay_amount, rupees(payAmount)) else stringResource(R.string.fee_pay),
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ---- Fee structure ----

@Composable
private fun FeeStructureCard(account: FeeAccount) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.fee_structure_title, account.academicYear), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            account.fees.sortedBy { it.dueAt }.forEachIndexed { index, fee ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FieldBorder)
                FeeRow(fee, fee.amount, stringResource(R.string.fee_due_on, fee.period, dayMonthYear(fee.dueAt))) {
                    Spacer(Modifier.height(4.dp))
                    FeeStatePill(account.stateOf(fee))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = FieldBorder)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.fee_total_for_year), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                Text(rupees(account.total), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ---- Payments ----

private fun LazyListScope.paymentsTab(account: FeeAccount) {
    val payments = account.payments.sortedByDescending { it.paidOn }
    if (payments.isEmpty()) {
        item(key = "no-payments") {
            MessageCard {
                StateMessage(
                    iconRes = R.drawable.ic_credit_card,
                    iconTint = BrandBlue,
                    iconBackground = SelectedRowBackground,
                    title = stringResource(R.string.fee_no_payments_title),
                    subtitle = stringResource(R.string.fee_no_payments_body),
                )
            }
        }
        return
    }
    item(key = "payments") {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(22.dp),
            color = White,
            border = BorderStroke(1.dp, FieldBorder),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                payments.forEachIndexed { index, payment ->
                    if (index > 0) HorizontalDivider(color = FieldBorder)
                    PaymentRow(account, payment)
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(account: FeeAccount, payment: FeePayment) {
    val fee = account.fees.firstOrNull { it.id == payment.feeId }
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    fee?.let { stringResource(it.category.label) }.orEmpty(),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.fee_payment_line, dayMonthYear(payment.paidOn), stringResource(payment.method.label), payment.reference),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                    lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(rupees(payment.amount), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                PaymentStatusPill(payment.status)
            }
        }
        if (payment.status == PaymentStatus.REJECTED && payment.note != null) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.fee_payment_note, payment.note), color = BadgeRed, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun FeeDetailsPreview() {
    GRADit_RewampTheme {
        val accounts = FeeRepository.accounts.value
        FeeDetailsContent(
            uiState = FeeDetailsUiState(
                load = FeeLoad.READY,
                accounts = accounts,
                account = accounts[0],
                selected = setOf("a-exam"),
            ),
            onBack = {}, onStudent = {}, onTab = {}, onToggleFee = {}, onRetry = {}, onDownload = {}, onUploadProof = {}, onPay = {},
        )
    }
}
