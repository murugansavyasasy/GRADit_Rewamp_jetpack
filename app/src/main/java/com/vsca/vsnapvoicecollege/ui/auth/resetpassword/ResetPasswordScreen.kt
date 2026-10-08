package com.vsca.vsnapvoicecollege.ui.auth.resetpassword

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AppPasswordField
import com.vsca.vsnapvoicecollege.ui.components.AppTextField
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.LogoBadge
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.RadioBorder
import com.vsca.vsnapvoicecollege.ui.theme.SearchFieldBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Create / reset password screen — set a new password with live strength and
 * requirement feedback.
 */
@Composable
fun ResetPasswordScreen(
    onNavigateBack: () -> Unit,
    onPasswordReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SystemBarIcons(darkIcons = true)

    LaunchedEffect(uiState.isPasswordReset) {
        if (uiState.isPasswordReset) onPasswordReset()
    }

    ResetPasswordContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onToggleVisibility = viewModel::onTogglePasswordVisibility,
        onSave = viewModel::onSaveClick,
        modifier = modifier,
    )
}

@Composable
private fun ResetPasswordContent(
    uiState: ResetPasswordUiState,
    onNavigateBack: () -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        BackButton(onClick = onNavigateBack)

        Spacer(Modifier.height(24.dp))

        LogoBadge(
            iconRes = R.drawable.ic_lock,
            contentDescription = stringResource(R.string.create_pw_lock_desc),
            size = 64.dp,
            cornerRadius = 18.dp,
            iconSize = 30.dp,
            containerColor = SelectedRowBackground,
            iconTint = BrandBlue,
            shadowElevation = 0.dp,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.create_pw_title),
            color = TextPrimary,
            fontSize = AppTextSize.H2,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.create_pw_subtitle),
            color = TextSecondary,
            fontSize = AppTextSize.Body,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(24.dp))

        AppPasswordField(
            value = uiState.newPassword,
            onValueChange = onNewPasswordChange,
            label = stringResource(R.string.create_pw_new_label),
            isVisible = uiState.isPasswordVisible,
            onToggleVisibility = onToggleVisibility,
        )

        if (uiState.strength != PasswordStrength.NONE) {
            Spacer(Modifier.height(10.dp))
            StrengthMeter(strength = uiState.strength)
        }

        Spacer(Modifier.height(20.dp))

        AppTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.create_pw_confirm_label),
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(Modifier.height(20.dp))

        RequirementsCard(uiState = uiState)

        Spacer(Modifier.height(32.dp))

        PrimaryButton(
            text = stringResource(R.string.create_pw_save),
            onClick = onSave,
            enabled = uiState.canSave,
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
private fun StrengthMeter(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val color = when (strength) {
        PasswordStrength.WEAK -> BadgeRed
        PasswordStrength.FAIR -> AmberText
        PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> BrandGreen
        PasswordStrength.NONE -> FieldBorder
    }
    val labelRes = when (strength) {
        PasswordStrength.WEAK -> R.string.pw_strength_weak
        PasswordStrength.FAIR -> R.string.pw_strength_fair
        PasswordStrength.STRONG -> R.string.pw_strength_strong
        PasswordStrength.VERY_STRONG -> R.string.pw_strength_very_strong
        PasswordStrength.NONE -> null
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(ResetPasswordUiState.STRENGTH_SEGMENTS) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index < strength.segments) color else FieldBorder),
                )
            }
        }
        if (labelRes != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(labelRes),
                color = color,
                fontSize = AppTextSize.Caption,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RequirementsCard(
    uiState: ResetPasswordUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SearchFieldBackground,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RequirementRow(stringResource(R.string.pw_req_length), uiState.hasMinLength)
            RequirementRow(stringResource(R.string.pw_req_case), uiState.hasUpperAndLower)
            RequirementRow(stringResource(R.string.pw_req_number), uiState.hasNumberOrSymbol)
            RequirementRow(stringResource(R.string.pw_req_match), uiState.passwordsMatch)
        }
    }
}

@Composable
private fun RequirementRow(
    text: String,
    met: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (met) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(BrandGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.pw_req_met_desc),
                    tint = White,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, RadioBorder, CircleShape),
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = text,
            color = if (met) TextPrimary else TextSecondary,
            fontSize = AppTextSize.Label,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResetPasswordPreview() {
    GRADit_RewampTheme {
        ResetPasswordContent(
            uiState = ResetPasswordUiState(
                newPassword = "Password12",
                confirmPassword = "Password12",
            ),
            onNavigateBack = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onToggleVisibility = {},
            onSave = {},
        )
    }
}
