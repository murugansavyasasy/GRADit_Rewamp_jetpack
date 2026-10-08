package com.vsca.vsnapvoicecollege.ui.auth.roleselection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.IconBadge
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SelectableCard
import com.vsca.vsnapvoicecollege.ui.components.SelectionRadio
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Role selection shown after sign-in when an account has more than one role.
 * The user picks how they're using the app and the college context, then
 * continues into the main app.
 */
@Composable
fun RoleSelectionScreen(
    onContinue: (Role) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoleSelectionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SystemBarIcons(darkIcons = true)

    RoleSelectionContent(
        uiState = uiState,
        onRoleSelected = viewModel::onRoleSelected,
        onCollegeSelected = viewModel::onCollegeSelected,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun RoleSelectionContent(
    uiState: RoleSelectionUiState,
    onRoleSelected: (Role) -> Unit,
    onCollegeSelected: (String) -> Unit,
    onContinue: (Role) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.role_signed_in_as, uiState.maskedMobile).uppercase(),
                color = BrandBlue,
                fontSize = AppTextSize.Caption,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.role_title),
                color = TextPrimary,
                fontSize = AppTextSize.H2,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.role_subtitle),
                color = TextSecondary,
                fontSize = AppTextSize.Body,
                lineHeight = 21.sp,
            )

            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.availableRoles.forEach { role ->
                    RoleCard(
                        role = role,
                        selected = role == uiState.selectedRole,
                        onClick = { onRoleSelected(role) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.role_college_label),
                color = TextPrimary,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(8.dp))

            DropdownField(
                value = uiState.selectedCollege.orEmpty(),
                options = uiState.colleges,
                onOptionSelected = onCollegeSelected,
            )

            Spacer(Modifier.height(16.dp))
        }

        val selected = uiState.selectedRole
        PrimaryButton(
            text = if (selected != null) {
                stringResource(R.string.role_continue, stringResource(selected.continueLabel))
            } else {
                stringResource(R.string.splash_get_started)
            },
            onClick = { selected?.let(onContinue) },
            enabled = selected != null,
        )

        Spacer(Modifier.height(20.dp))

        PoweredByRow(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textColor = TextSecondary,
            logoBackground = Color.Transparent,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RoleCard(
    role: Role,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (badgeBackground, badgeIconTint) = roleBadgeColors(role)
    SelectableCard(selected = selected, onClick = onClick, modifier = modifier) {
        IconBadge(
            iconRes = role.icon,
            contentDescription = stringResource(role.badgeContentDesc),
            size = 44.dp,
            cornerRadius = 12.dp,
            iconSize = 24.dp,
            containerColor = badgeBackground,
            iconTint = badgeIconTint,
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(role.title),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(role.description),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.width(12.dp))

        SelectionRadio(selected = selected)
    }
}

/** Maps each role to its (badge background, icon tint) accent pair. */
private fun roleBadgeColors(role: Role): Pair<Color, Color> = when (role) {
    Role.PARENT -> BrandBlue to White
    Role.STUDENT -> OnboardingGreenDisc to BrandGreen
    Role.FACULTY -> AmberChipBackground to AmberText
    Role.MANAGEMENT -> OnboardingGreenDisc to BrandGreen
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RoleSelectionPreview() {
    GRADit_RewampTheme {
        RoleSelectionContent(
            uiState = RoleSelectionUiState(
                maskedMobile = "+91 ••••• •3210",
                availableRoles = Role.entries.toList(),
                selectedRole = Role.PARENT,
                colleges = listOf("University College of Engineering, Chennai"),
                selectedCollege = "University College of Engineering, Chennai",
            ),
            onRoleSelected = {},
            onCollegeSelected = {},
            onContinue = {},
        )
    }
}
