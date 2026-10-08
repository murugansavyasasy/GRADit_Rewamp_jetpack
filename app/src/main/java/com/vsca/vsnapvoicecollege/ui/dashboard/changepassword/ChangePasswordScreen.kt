package com.vsca.vsnapvoicecollege.ui.dashboard.changepassword

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AppPasswordField
import com.vsca.vsnapvoicecollege.ui.components.AppTextField
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.PasswordStrength
import com.vsca.vsnapvoicecollege.ui.components.PasswordStrengthBar
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.passwordStrengthColor
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Change password screen, reached from Settings.
 */
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onUpdated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isUpdated) {
        if (uiState.isUpdated) onUpdated()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.change_pw_title),
                color = TextPrimary,
                fontSize = AppTextSize.H4,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.change_pw_subtitle),
            color = TextSecondary,
            fontSize = AppTextSize.Body,
        )

        Spacer(Modifier.height(24.dp))
        AppTextField(
            value = uiState.currentPassword,
            onValueChange = viewModel::onCurrentPasswordChange,
            label = stringResource(R.string.change_pw_current_label),
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            labelTrailing = {
                Text(
                    text = stringResource(R.string.change_pw_forgot),
                    color = BrandBlue,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { /* TODO: forgot current password */ },
                )
            },
        )

        Spacer(Modifier.height(16.dp))
        AppPasswordField(
            value = uiState.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            label = stringResource(R.string.change_pw_new_label),
            isVisible = uiState.isNewPasswordVisible,
            onToggleVisibility = viewModel::onToggleNewPasswordVisibility,
        )

        if (uiState.strength != PasswordStrength.NONE) {
            Spacer(Modifier.height(10.dp))
            PasswordStrengthBar(strength = uiState.strength)
            Spacer(Modifier.height(8.dp))
            val labelRes = when (uiState.strength) {
                PasswordStrength.WEAK -> R.string.change_pw_strength_weak
                PasswordStrength.FAIR -> R.string.change_pw_strength_fair
                PasswordStrength.STRONG -> R.string.change_pw_strength_strong
                PasswordStrength.VERY_STRONG -> R.string.change_pw_strength_very_strong
                PasswordStrength.NONE -> null
            }
            if (labelRes != null) {
                Text(
                    text = stringResource(labelRes),
                    color = passwordStrengthColor(uiState.strength),
                    fontSize = AppTextSize.Caption,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        AppTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = stringResource(R.string.change_pw_confirm_label),
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            errorMessage = if (uiState.showMismatch) {
                stringResource(R.string.change_pw_mismatch)
            } else {
                null
            },
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = uiState.signOutOtherDevices,
                onCheckedChange = viewModel::onSignOutOtherDevicesChange,
                colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
            )
            Text(
                text = stringResource(R.string.change_pw_signout_others),
                color = TextPrimary,
                fontSize = AppTextSize.Label,
            )
        }

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = stringResource(R.string.change_pw_update),
            onClick = viewModel::onUpdateClick,
            enabled = uiState.canUpdate,
        )

        Spacer(Modifier.height(24.dp))
    }
}
