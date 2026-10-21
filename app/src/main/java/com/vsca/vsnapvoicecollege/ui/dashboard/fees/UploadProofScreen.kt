package com.vsca.vsnapvoicecollege.ui.dashboard.fees

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AppTextField
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.IMAGE_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.PDF_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.dashedBorder
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.pickPastDate
import com.vsca.vsnapvoicecollege.ui.components.readAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Proof of a payment made outside the app (UPI, bank transfer, DD or cash) for the accounts office to verify. */
@Composable
fun UploadProofScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UploadProofViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val submitted = stringResource(R.string.fee_proof_submitted)
    val failed = stringResource(R.string.fee_proof_submit_failed)
    LaunchedEffect(uiState.submit) {
        when (uiState.submit) {
            SubmitState.DONE -> {
                Toast.makeText(context, submitted, Toast.LENGTH_LONG).show()
                onSubmitted()
            }
            SubmitState.FAILED -> {
                Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                viewModel.onFailureShown()
            }
            else -> Unit
        }
    }
    UploadProofContent(
        uiState = uiState,
        onBack = onBack,
        onFee = viewModel::onFee,
        onAmount = viewModel::onAmount,
        onPickDate = { pickPastDate(context, uiState.paidOn, viewModel::onPaidOn) },
        onMethod = viewModel::onMethod,
        onReference = viewModel::onReference,
        onReceipt = viewModel::onReceipt,
        onSubmit = viewModel::submit,
        modifier = modifier,
    )
}

@Composable
private fun UploadProofContent(
    uiState: UploadProofUiState,
    onBack: () -> Unit,
    onFee: (String) -> Unit,
    onAmount: (String) -> Unit,
    onPickDate: () -> Unit,
    onMethod: (PaymentMethod) -> Unit,
    onReference: (String) -> Unit,
    onReceipt: (FileAttachment?) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(White).imePadding()) {
        TitledTopBar(title = stringResource(R.string.fee_upload_title), onBack = onBack)
        if (uiState.fees.isEmpty()) {
            Text(stringResource(R.string.fee_upload_nothing), Modifier.padding(24.dp), color = TextSecondary, fontSize = AppTextSize.Body)
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            FeeField(uiState, onFee)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(uiState, onAmount, Modifier.weight(1f))
                DateField(dayMonthYear(uiState.paidOn), onPickDate, Modifier.weight(1f))
            }
            MethodField(uiState.method, onMethod)
            AppTextField(
                value = uiState.reference,
                onValueChange = onReference,
                label = stringResource(if (uiState.method == PaymentMethod.DD_CASH) R.string.fee_reference_dd else R.string.fee_reference_utr),
                placeholder = stringResource(if (uiState.method == PaymentMethod.DD_CASH) R.string.fee_reference_dd_hint else R.string.fee_reference_utr_hint),
                errorMessage = if (uiState.errorShown(ProofError.REFERENCE_MISSING)) stringResource(R.string.fee_error_reference) else null,
            )
            ReceiptField(uiState.receipt, uiState.errorShown(ProofError.RECEIPT_MISSING), onReceipt)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onSubmit,
            enabled = uiState.submit != SubmitState.SUBMITTING,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PayGreen, contentColor = White),
        ) {
            if (uiState.submit == SubmitState.SUBMITTING) {
                CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.fee_submit_verification), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.Medium)
}

/** "Hostel & mess — ₹22,000". */
@Composable
private fun FeeField(uiState: UploadProofUiState, onFee: (String) -> Unit) {
    val labels = uiState.fees.map { fee ->
        stringResource(R.string.fee_option, stringResource(fee.category.label), rupees(uiState.balances[fee.id] ?: fee.amount))
    }
    Column {
        FormLabel(stringResource(R.string.fee_field_fee))
        Spacer(Modifier.height(8.dp))
        DropdownField(
            value = labels[uiState.fees.indexOfFirst { it.id == uiState.feeId }.coerceAtLeast(0)],
            options = labels,
            onOptionSelected = { label -> onFee(uiState.fees[labels.indexOf(label)].id) },
        )
    }
}

@Composable
private fun AmountField(uiState: UploadProofUiState, onAmount: (String) -> Unit, modifier: Modifier = Modifier) {
    val error = when {
        uiState.errorShown(ProofError.AMOUNT_MISSING) -> stringResource(R.string.fee_error_amount)
        uiState.errorShown(ProofError.AMOUNT_TOO_HIGH) -> stringResource(R.string.fee_error_amount_high, rupees(uiState.balance))
        else -> null
    }
    Column(modifier) {
        FormLabel(stringResource(R.string.fee_field_amount))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = onAmount,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = RupeeTransformation,
            supportingText = error?.let { { Text(it, color = BadgeRed, fontSize = AppTextSize.Caption) } },
            colors = appTextFieldColors(),
        )
    }
}

/** Shows "22000" as "₹ 22,000" while the field keeps plain digits. */
private object RupeeTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val shown = "₹ " + rupees(digits.toLong()).removePrefix("₹")
        return TransformedText(
            AnnotatedString(shown),
            object : OffsetMapping {
                // The cursor lives at the end: amounts are typed, not edited mid-number.
                override fun originalToTransformed(offset: Int) = if (offset >= digits.length) shown.length else shownIndexOf(offset)
                override fun transformedToOriginal(offset: Int) = shown.take(offset).count(Char::isDigit).coerceAtMost(digits.length)
                private fun shownIndexOf(digitCount: Int): Int {
                    var seen = 0
                    shown.forEachIndexed { i, c ->
                        if (seen == digitCount) return i
                        if (c.isDigit()) seen++
                    }
                    return shown.length
                }
            },
        )
    }
}

@Composable
private fun DateField(value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        FormLabel(stringResource(R.string.fee_field_paid_on))
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            color = White,
            border = BorderStroke(1.dp, FieldBorder),
        ) {
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                Text(value, color = TextPrimary, fontSize = AppTextSize.BodyLarge, maxLines = 1)
            }
        }
    }
}

/** UPI / Bank / DD / Cash. */
@Composable
private fun MethodField(selected: PaymentMethod, onMethod: (PaymentMethod) -> Unit) {
    Column {
        FormLabel(stringResource(R.string.fee_field_method))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(PaymentMethod.UPI, PaymentMethod.BANK, PaymentMethod.DD_CASH).forEach { method ->
                val isSelected = method == selected
                Surface(
                    onClick = { onMethod(method) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SelectedRowBackground else White,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandBlue else FieldBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(method.label),
                            color = if (isSelected) BrandBlue else TextPrimary,
                            fontSize = AppTextSize.Body,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Dashed "Tap to upload" box, then the picked file with a remove button. */
@Composable
private fun ReceiptField(receipt: FileAttachment?, showError: Boolean, onReceipt: (FileAttachment?) -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readAttachment(context, it) }?.let(onReceipt)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormLabel(stringResource(R.string.fee_field_receipt))
        Column(
            Modifier.fillMaxWidth()
                .dashedBorder(color = if (showError) BadgeRed else BrandBlue.copy(alpha = 0.5f), cornerRadius = 18.dp, strokeWidth = 1.dp)
                .background(ScreenBackground, RoundedCornerShape(18.dp))
                .clickable { picker.launch(IMAGE_MIME_TYPES + PDF_MIME_TYPES) }
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painterResource(R.drawable.ic_upload), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.fee_tap_to_upload), color = BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.fee_upload_types), color = TextSecondary, fontSize = AppTextSize.Label)
        }
        if (showError) {
            Text(stringResource(R.string.fee_error_receipt), color = BadgeRed, fontSize = AppTextSize.Caption)
        }
        receipt?.let { file ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(OnboardingGreenDisc, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(if (file.type == AttachmentType.PDF) R.drawable.ic_file else R.drawable.ic_image_outline),
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.fee_uploaded_size, fileSizeLabel(file.sizeBytes)),
                            color = BrandGreen,
                            fontSize = AppTextSize.Label,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { onReceipt(null) }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.fee_remove_receipt), tint = TextSecondary)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun UploadProofPreview() {
    GRADit_RewampTheme {
        val account = FeeRepository.accounts.value[0]
        val fees = account.fees.filter { account.balanceOf(it) > 0 }
        UploadProofContent(
            uiState = UploadProofUiState(
                studentId = account.studentId,
                fees = fees,
                balances = fees.associate { it.id to account.balanceOf(it) },
                feeId = fees.first().id,
                amount = "22000",
                reference = "UTR 4271 9930 1185",
                receipt = FileAttachment("upi_receipt_0510.jpg", 1_258_291, AttachmentType.IMAGE),
            ),
            onBack = {}, onFee = {}, onAmount = {}, onPickDate = {}, onMethod = {}, onReference = {}, onReceipt = {}, onSubmit = {},
        )
    }
}
