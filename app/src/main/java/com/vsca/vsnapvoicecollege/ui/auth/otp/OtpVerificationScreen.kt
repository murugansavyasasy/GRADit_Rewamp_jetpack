package com.vsca.vsnapvoicecollege.ui.auth.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.LogoBadge
import com.vsca.vsnapvoicecollege.ui.components.OtpInput
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SearchFieldBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * OTP verification screen — enter the 6-digit code, with a resend countdown.
 */
@Composable
fun OtpVerificationScreen(
    onNavigateBack: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    maskedTarget: String = "",
    viewModel: OtpVerificationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SystemBarIcons(darkIcons = true)

    LaunchedEffect(maskedTarget) {
        viewModel.setMaskedTarget(maskedTarget)
    }

    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) onVerified()
    }

    OtpVerificationContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onOtpChange = viewModel::onOtpChange,
        onResend = viewModel::onResendClick,
        onVerify = viewModel::onVerifyClick,
        modifier = modifier,
    )
}

@Composable
private fun OtpVerificationContent(
    uiState: OtpVerificationUiState,
    onNavigateBack: () -> Unit,
    onOtpChange: (String) -> Unit,
    onResend: () -> Unit,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        BackButton(onClick = onNavigateBack)

        Spacer(Modifier.height(24.dp))

        LogoBadge(
            iconRes = R.drawable.ic_shield_check,
            contentDescription = stringResource(R.string.otp_shield_desc),
            size = 64.dp,
            cornerRadius = 18.dp,
            iconSize = 30.dp,
            containerColor = OnboardingGreenDisc,
            iconTint = BrandGreen,
            shadowElevation = 0.dp,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.otp_title),
            color = TextPrimary,
            fontSize = AppTextSize.H2,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(Modifier.height(8.dp))

        SubtitleWithTarget(maskedTarget = uiState.maskedTarget)

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.otp_change),
            color = BrandBlue,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onNavigateBack),
        )

        Spacer(Modifier.height(24.dp))

        OtpInput(
            value = uiState.otp,
            onValueChange = onOtpChange,
            length = OtpVerificationUiState.OTP_LENGTH,
        )

        Spacer(Modifier.height(20.dp))

        ResendRow(
            secondsRemaining = uiState.resendSecondsRemaining,
            canResend = uiState.canResend,
            onResend = onResend,
        )

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = stringResource(R.string.otp_verify),
            onClick = onVerify,
            enabled = uiState.isComplete,
        )

        Spacer(Modifier.height(16.dp))

        PoweredByRow(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textColor = TextSecondary,
            logoBackground = White,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SubtitleWithTarget(maskedTarget: String) {
    val full = stringResource(R.string.otp_subtitle, maskedTarget)
    val annotated = buildAnnotatedString {
        append(full)
        val start = full.indexOf(maskedTarget)
        if (start >= 0) {
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary),
                start,
                start + maskedTarget.length,
            )
        }
    }
    Text(
        text = annotated,
        color = TextSecondary,
        fontSize = AppTextSize.Body,
        lineHeight = 21.sp,
    )
}

@Composable
private fun ResendRow(
    secondsRemaining: Int,
    canResend: Boolean,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SearchFieldBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = stringResource(R.string.otp_timer_desc),
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.otp_resend_in),
                color = TextSecondary,
                fontSize = AppTextSize.Label,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatTimer(secondsRemaining),
                color = TextPrimary,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.otp_resend),
                color = if (canResend) BrandBlue else TextMuted,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
                modifier = if (canResend) {
                    Modifier.clickable(onClick = onResend)
                } else {
                    Modifier
                },
            )
        }
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OtpVerificationPreview() {
    GRADit_RewampTheme {
        OtpVerificationContent(
            uiState = OtpVerificationUiState(
                otp = "482",
                maskedTarget = "+91 ●●●●●●3210",
                resendSecondsRemaining = 42,
            ),
            onNavigateBack = {},
            onOtpChange = {},
            onResend = {},
            onVerify = {},
        )
    }
}
