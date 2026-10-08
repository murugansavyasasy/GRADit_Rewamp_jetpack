package com.vsca.vsnapvoicecollege.ui.dashboard.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.LocaleManager
import com.vsca.vsnapvoicecollege.ui.components.AppCard
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.LanguageBottomSheet
import com.vsca.vsnapvoicecollege.ui.components.SectionLabel
import com.vsca.vsnapvoicecollege.ui.components.SupportedLanguages
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.IndicatorInactive
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Settings screen, reached from the profile. Notification toggles are stateful;
 * the rest are hardcoded rows for now.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenFaqs: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    val currentLanguage = remember { LocaleManager.currentLanguage() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.settings_title),
                color = TextPrimary,
                fontSize = AppTextSize.H4,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.settings_section_notifications))
        Spacer(Modifier.height(10.dp))
        AppCard {
            Column {
                ToggleRow(
                    title = stringResource(R.string.settings_attendance_title),
                    subtitle = stringResource(R.string.settings_attendance_sub),
                    checked = uiState.attendanceAlerts,
                    onCheckedChange = { viewModel.onToggle(SettingToggle.ATTENDANCE, it) },
                )
                RowDivider()
                ToggleRow(
                    title = stringResource(R.string.settings_fee_title),
                    subtitle = stringResource(R.string.settings_fee_sub),
                    checked = uiState.feeReminders,
                    onCheckedChange = { viewModel.onToggle(SettingToggle.FEE, it) },
                )
                RowDivider()
                ToggleRow(
                    title = stringResource(R.string.settings_results_title),
                    subtitle = stringResource(R.string.settings_results_sub),
                    checked = uiState.resultsExams,
                    onCheckedChange = { viewModel.onToggle(SettingToggle.RESULTS, it) },
                )
                RowDivider()
                ToggleRow(
                    title = stringResource(R.string.settings_bus_title),
                    subtitle = stringResource(R.string.settings_bus_sub),
                    checked = uiState.busArrival,
                    onCheckedChange = { viewModel.onToggle(SettingToggle.BUS, it) },
                )
                RowDivider()
                ToggleRow(
                    title = stringResource(R.string.settings_biometric_title),
                    subtitle = stringResource(R.string.settings_biometric_sub),
                    checked = uiState.biometricUnlock,
                    onCheckedChange = { viewModel.onToggle(SettingToggle.BIOMETRIC, it) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.settings_section_preferences))
        Spacer(Modifier.height(10.dp))
        AppCard {
            Column {
                NavRow(
                    iconRes = R.drawable.ic_language,
                    title = stringResource(R.string.settings_language),
                    value = currentLanguage.englishName,
                    onClick = { showLanguageSheet = true },
                )
                RowDivider()
                NavRow(
                    iconRes = R.drawable.ic_lock,
                    title = stringResource(R.string.profile_change_password),
                    onClick = onOpenChangePassword,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.settings_section_help))
        Spacer(Modifier.height(10.dp))
        AppCard {
            Column {
                NavRow(
                    iconRes = R.drawable.ic_chat,
                    title = stringResource(R.string.settings_help_support),
                    onClick = onOpenHelpSupport,
                )
                RowDivider()
                NavRow(
                    iconRes = R.drawable.ic_help,
                    title = stringResource(R.string.settings_faqs),
                    onClick = onOpenFaqs,
                )
                RowDivider()
                NavRow(
                    iconRes = R.drawable.ic_shield,
                    title = stringResource(R.string.settings_privacy),
                    onClick = onOpenPrivacy,
                )
                RowDivider()
                NavRow(
                    iconRes = R.drawable.ic_description,
                    title = stringResource(R.string.settings_terms),
                    onClick = onOpenTerms,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showLanguageSheet) {
        LanguageBottomSheet(
            languages = SupportedLanguages,
            selectedCode = currentLanguage.code,
            onApply = { language ->
                showLanguageSheet = false
                // Recreates the activity in the new locale.
                LocaleManager.apply(language.code)
            },
            onDismiss = { showLanguageSheet = false },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Caption)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = BrandGreen,
                checkedBorderColor = BrandGreen,
                uncheckedThumbColor = White,
                uncheckedTrackColor = IndicatorInactive,
                uncheckedBorderColor = IndicatorInactive,
            ),
        )
    }
}

@Composable
private fun NavRow(
    @DrawableRes iconRes: Int,
    title: String,
    value: String? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Medium,
        )
        if (value != null) {
            Text(text = value, color = TextSecondary, fontSize = AppTextSize.Label)
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = FieldBorder,
    )
}
