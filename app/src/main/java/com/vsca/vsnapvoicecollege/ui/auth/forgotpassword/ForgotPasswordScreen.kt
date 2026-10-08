package com.vsca.vsnapvoicecollege.ui.auth.forgotpassword

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.IconBadge
import com.vsca.vsnapvoicecollege.ui.components.LogoBadge
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.SelectableCard
import com.vsca.vsnapvoicecollege.ui.components.SelectionRadio
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Forgot-password screen — pick where to receive the reset code, then send it.
 */
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onOtpSent: (target: String) -> Unit,
    modifier: Modifier = Modifier,
    enteredMobile: String = "",
    viewModel: ForgotPasswordViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SystemBarIcons(darkIcons = true)

    LaunchedEffect(enteredMobile) {
        viewModel.setEnteredMobile(enteredMobile)
    }

    LaunchedEffect(uiState.isCodeSent) {
        if (uiState.isCodeSent) {
            val target = when (uiState.selectedMethod) {
                ResetMethod.SMS -> uiState.maskedMobile
                ResetMethod.EMAIL -> uiState.maskedEmail
            }
            onOtpSent(target)
        }
    }

    ForgotPasswordContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onMethodSelected = viewModel::onMethodSelected,
        onSendCode = viewModel::onSendCodeClick,
        modifier = modifier,
    )
}

@Composable
private fun ForgotPasswordContent(
    uiState: ForgotPasswordUiState,
    onNavigateBack: () -> Unit,
    onMethodSelected: (ResetMethod) -> Unit,
    onSendCode: () -> Unit,
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
            iconRes = R.drawable.ic_lock,
            contentDescription = stringResource(R.string.forgot_lock_desc),
            size = 64.dp,
            cornerRadius = 18.dp,
            iconSize = 30.dp,
            containerColor = SelectedRowBackground,
            iconTint = BrandBlue,
            shadowElevation = 0.dp,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.forgot_title),
            color = TextPrimary,
            fontSize = AppTextSize.H2,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.forgot_subtitle),
            color = TextSecondary,
            fontSize = AppTextSize.Body,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.forgot_send_via),
            color = TextSecondary,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(12.dp))

        MethodRow(
            iconRes = R.drawable.ic_smartphone,
            iconDescription = stringResource(R.string.forgot_method_sms_desc),
            title = stringResource(R.string.forgot_method_sms_title),
            subtitle = uiState.maskedMobile,
            selected = uiState.selectedMethod == ResetMethod.SMS,
            onClick = { onMethodSelected(ResetMethod.SMS) },
        )

        Spacer(Modifier.height(12.dp))

        MethodRow(
            iconRes = R.drawable.ic_email,
            iconDescription = stringResource(R.string.forgot_method_email_desc),
            title = stringResource(R.string.forgot_method_email_title),
            subtitle = uiState.maskedEmail,
            selected = uiState.selectedMethod == ResetMethod.EMAIL,
            onClick = { onMethodSelected(ResetMethod.EMAIL) },
        )

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = stringResource(R.string.forgot_send_code),
            onClick = onSendCode,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.forgot_back_to_sign_in),
            color = BrandBlue,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onNavigateBack),
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
private fun MethodRow(
    @DrawableRes iconRes: Int,
    iconDescription: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableCard(selected = selected, onClick = onClick, modifier = modifier) {
        IconBadge(
            iconRes = iconRes,
            contentDescription = iconDescription,
            containerColor = if (selected) BrandBlue else BadgeNeutralBackground,
            iconTint = if (selected) White else TextPrimary,
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
            )
        }

        Spacer(Modifier.width(12.dp))

        SelectionRadio(selected = selected)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ForgotPasswordPreview() {
    GRADit_RewampTheme {
        ForgotPasswordContent(
            uiState = ForgotPasswordUiState(
                selectedMethod = ResetMethod.SMS,
                maskedMobile = "+91 ●●●●●● 3210",
                maskedEmail = "pa●●●●●@gmail.com",
            ),
            onNavigateBack = {},
            onMethodSelected = {},
            onSendCode = {},
        )
    }
}
