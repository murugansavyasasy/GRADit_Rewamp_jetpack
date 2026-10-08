package com.vsca.vsnapvoicecollege.ui.dashboard.profile

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AppCard
import com.vsca.vsnapvoicecollege.ui.components.SectionLabel
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppDimens
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Profile ("Me") screen. Hardcoded content — API integration comes later.
 */
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onAllRoles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.profile_title),
            color = TextPrimary,
            fontSize = AppTextSize.H2,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(16.dp))

        ProfileCard(onAllRoles = onAllRoles)

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.profile_linked_students))
        Spacer(Modifier.height(10.dp))

        StudentCard(
            initials = stringResource(R.string.profile_student1_initials),
            name = stringResource(R.string.profile_student1_name),
            detail = stringResource(R.string.profile_student1_detail),
            badgeColor = BrandBlue,
            badgeTextColor = White,
            selected = true,
        )
        Spacer(Modifier.height(12.dp))
        StudentCard(
            initials = stringResource(R.string.profile_student2_initials),
            name = stringResource(R.string.profile_student2_name),
            detail = stringResource(R.string.profile_student2_detail),
            badgeColor = OnboardingGreenDisc,
            badgeTextColor = BrandGreen,
            selected = false,
        )

        Spacer(Modifier.height(16.dp))
        MenuCard(onOpenSettings = onOpenSettings)

        Spacer(Modifier.height(16.dp))
        SignOutButton(onClick = { showSignOutDialog = true })

        Spacer(Modifier.height(24.dp))
    }

    if (showSignOutDialog) {
        SignOutConfirmDialog(
            onConfirm = {
                showSignOutDialog = false
                onSignOut()
            },
            onDismiss = { showSignOutDialog = false },
        )
    }
}

@Composable
private fun SignOutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = White) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AlertBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logout),
                        contentDescription = null,
                        tint = BadgeRed,
                        modifier = Modifier.size(30.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.profile_signout_confirm_title),
                    color = TextPrimary,
                    fontSize = AppTextSize.H5,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.profile_signout_confirm_message),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DialogButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = onDismiss,
                        containerColor = White,
                        contentColor = TextPrimary,
                        border = BorderStroke(1.dp, FieldBorder),
                        modifier = Modifier.weight(1f),
                    )
                    DialogButton(
                        text = stringResource(R.string.profile_sign_out),
                        onClick = onConfirm,
                        containerColor = BadgeRed,
                        contentColor = White,
                        border = null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke?,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(AppDimens.ButtonHeight),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = containerColor,
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = contentColor,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProfileCard(onAllRoles: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SelectedRowBackground,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Avatar(
                    initials = stringResource(R.string.profile_avatar_initials),
                    size = 56.dp,
                    background = BrandBlue,
                    textColor = White,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_name),
                        color = TextPrimary,
                        fontSize = AppTextSize.H6,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.profile_phone),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                    Spacer(Modifier.height(6.dp))
                    Pill(
                        text = stringResource(R.string.profile_role_parent),
                        background = White,
                        textColor = BrandBlue,
                    )
                }
                EditButton()
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.profile_your_roles))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoleChip(stringResource(R.string.profile_role_parent), selected = true)
                RoleChip(stringResource(R.string.profile_role_principal), selected = false)
                RoleChip(stringResource(R.string.profile_role_all), selected = false, onClick = onAllRoles)
            }
        }
    }
}

@Composable
private fun EditButton() {
    Surface(
        onClick = { /* TODO: edit profile */ },
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.profile_edit_desc),
                tint = BrandBlue,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RoleChip(text: String, selected: Boolean, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) BrandBlue else White,
        border = if (selected) null else BorderStroke(1.dp, BrandBlue),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = text,
                color = if (selected) White else BrandBlue,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StudentCard(
    initials: String,
    name: String,
    detail: String,
    badgeColor: Color,
    badgeTextColor: Color,
    selected: Boolean,
) {
    Surface(
        onClick = { /* TODO: switch student */ },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = White,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) BrandBlue else FieldBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                initials = initials,
                size = 44.dp,
                background = badgeColor,
                textColor = badgeTextColor,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(detail, color = TextSecondary, fontSize = AppTextSize.Caption)
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun MenuCard(onOpenSettings: () -> Unit) {
    AppCard {
        Column {
            MenuRow(R.drawable.ic_tune, stringResource(R.string.profile_settings), onClick = onOpenSettings)
        }
    }
}

@Composable
private fun MenuRow(@DrawableRes iconRes: Int, text: String, onClick: () -> Unit) {
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
            text = text,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimens.ButtonHeight),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AlertBackground,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_logout),
                contentDescription = null,
                tint = BadgeRed,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.profile_sign_out),
                color = BadgeRed,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Avatar(
    initials: String,
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    textColor: Color,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = textColor,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Pill(text: String, background: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            color = textColor,
            fontSize = AppTextSize.CaptionSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

