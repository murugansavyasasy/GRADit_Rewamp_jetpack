package com.vsca.vsnapvoicecollege.ui.auth.login

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.LocaleManager
import com.vsca.vsnapvoicecollege.ui.components.AppPasswordField
import com.vsca.vsnapvoicecollege.ui.components.AppTextField
import com.vsca.vsnapvoicecollege.ui.components.LanguageBottomSheet
import com.vsca.vsnapvoicecollege.ui.components.LanguageSelector
import com.vsca.vsnapvoicecollege.ui.components.SupportedLanguages
import com.vsca.vsnapvoicecollege.ui.components.LogoBadge
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.AppDimens
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Login screen — identifier + password sign-in, with a fingerprint shortcut.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: (mobile: String) -> Unit,
    onLoginSuccess: (mobile: String) -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLanguage = remember { LocaleManager.currentLanguage() }
    SystemBarIcons(darkIcons = true)

    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess(uiState.identifier.trim())
            viewModel.onLoginNavigated()
        }
    }

    LaunchedEffect(uiState.navigateToForgotPassword) {
        if (uiState.navigateToForgotPassword) {
            onNavigateToForgotPassword(uiState.identifier.trim())
            viewModel.onForgotPasswordNavigated()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoBadge(
                size = 56.dp,
                cornerRadius = 16.dp,
                iconSize = 28.dp,
                containerColor = BrandBlue,
                iconTint = White,
                shadowElevation = 0.dp,
            )
            LanguageSelector(
                language = currentLanguage.englishName,
                onClick = viewModel::onLanguageClick,
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.login_welcome_title),
            color = TextPrimary,
            fontSize = AppTextSize.H1,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.login_welcome_subtitle),
            color = TextSecondary,
            fontSize = AppTextSize.Body,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(28.dp))

        AppTextField(
            value = uiState.identifier,
            onValueChange = viewModel::onIdentifierChange,
            label = stringResource(R.string.login_identifier_label),
            placeholder = stringResource(R.string.login_identifier_hint),
            keyboardType = KeyboardType.Phone,
            errorMessage = uiState.identifierErrorRes?.let { stringResource(it) },
        )

        Spacer(Modifier.height(16.dp))

        AppPasswordField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.login_password_label),
            isVisible = uiState.isPasswordVisible,
            onToggleVisibility = viewModel::onTogglePasswordVisibility,
            errorMessage = uiState.passwordErrorRes?.let { stringResource(it) },
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.keepSignedIn,
                    onCheckedChange = viewModel::onKeepSignedInChange,
                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
                )
                Text(
                    text = stringResource(R.string.login_keep_signed_in),
                    color = TextPrimary,
                    fontSize = AppTextSize.Label,
                )
            }
            // Validates the mobile number on click before navigating.
            Text(
                text = stringResource(R.string.login_forgot_password),
                color = BrandBlue,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = viewModel::onForgotPasswordClick),
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton(
                text = stringResource(R.string.login_sign_in),
                onClick = viewModel::onLoginClick,
                modifier = Modifier.weight(1f),
            )
            if (uiState.isFingerprintEnabled) {
                FingerprintButton(onClick = viewModel::onFingerprintClick)
            }
        }

        if (uiState.isFingerprintEnabled) {
            Spacer(Modifier.height(12.dp))
            FingerprintStatus()
        }

        Spacer(Modifier.weight(1f))

        LegalText(
            onNavigateToTerms = onNavigateToTerms,
            onNavigateToPrivacy = onNavigateToPrivacy,
        )

        Spacer(Modifier.height(20.dp))

        PoweredByRow(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textColor = TextSecondary,
            logoBackground = White,
        )

        Spacer(Modifier.height(24.dp))
    }

    if (uiState.showBiometricPrompt) {
        BiometricBottomSheet(
            userName = uiState.biometricUserName,
            maskedId = uiState.biometricMaskedId,
            onCancel = viewModel::onDismissBiometricPrompt,
            onUsePassword = viewModel::onDismissBiometricPrompt,
            onDismiss = viewModel::onDismissBiometricPrompt,
        )
    }

    if (uiState.showLanguageSheet) {
        LanguageBottomSheet(
            languages = SupportedLanguages,
            selectedCode = currentLanguage.code,
            onApply = { language ->
                viewModel.onDismissLanguageSheet()
                // Recreates the activity in the new locale.
                LocaleManager.apply(language.code)
            },
            onDismiss = viewModel::onDismissLanguageSheet,
        )
    }
}

@Composable
private fun FingerprintButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(AppDimens.ButtonHeight),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = White,
        border = BorderStroke(1.5.dp, BrandGreen),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_fingerprint),
                contentDescription = stringResource(R.string.login_fingerprint_button_desc),
                tint = BrandGreen,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun FingerprintStatus(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(BrandGreen),
        )
        Text(
            text = stringResource(R.string.login_fingerprint_on),
            color = BrandGreen,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LegalText(
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val terms = stringResource(R.string.login_terms)
    val privacy = stringResource(R.string.login_privacy)
    val full = stringResource(R.string.login_legal, terms, privacy)

    val annotated = buildAnnotatedString {
        append(full)
        listOf(
            terms to onNavigateToTerms,
            privacy to onNavigateToPrivacy,
        ).forEach { (token, onClick) ->
            val start = full.indexOf(token)
            if (start >= 0) {
                val end = start + token.length
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary),
                    start,
                    end,
                )
                addLink(
                    LinkAnnotation.Clickable(
                        tag = token,
                        linkInteractionListener = { onClick() },
                    ),
                    start,
                    end,
                )
            }
        }
    }

    Text(
        text = annotated,
        modifier = modifier.fillMaxWidth(),
        color = TextSecondary,
        fontSize = AppTextSize.Caption,
        lineHeight = 19.sp,
        textAlign = TextAlign.Center,
    )
}
