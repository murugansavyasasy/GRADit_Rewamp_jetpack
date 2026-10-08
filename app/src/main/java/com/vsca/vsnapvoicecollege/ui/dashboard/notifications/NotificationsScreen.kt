package com.vsca.vsnapvoicecollege.ui.dashboard.notifications

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.CategoryChip
import com.vsca.vsnapvoicecollege.ui.components.IconBadge
import com.vsca.vsnapvoicecollege.ui.components.SectionLabel
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** The filter categories across the top; [ALL] shows everything. */
private enum class NotifCategory(@param:StringRes val label: Int) {
    ALL(R.string.notif_cat_all),
    ACADEMIC(R.string.notif_cat_academic),
    EXAMS(R.string.notif_cat_exams),
    EVENTS(R.string.notif_cat_events),
    PLACEMENT(R.string.notif_cat_placement),
    TRAINING(R.string.notif_cat_training),
    FEES(R.string.notif_cat_fees),
    ATTENDANCE(R.string.notif_cat_attendance),
}

/** Leading-badge accent per notification. */
private enum class NotifAccent(val background: Color, val tint: Color) {
    BLUE(SelectedRowBackground, BrandBlue),
    AMBER(AmberChipBackground, AmberText),
    GREEN(OnboardingGreenDisc, BrandGreen),
}

private data class NotificationItem(
    val category: NotifCategory,
    @param:DrawableRes val icon: Int,
    val accent: NotifAccent,
    @param:StringRes val title: Int,
    @param:StringRes val time: Int,
    @param:StringRes val description: Int,
)

private val notifications = listOf(
    NotificationItem(
        category = NotifCategory.EVENTS,
        icon = R.drawable.ic_image,
        accent = NotifAccent.BLUE,
        title = R.string.notif1_title,
        time = R.string.notif1_time,
        description = R.string.notif1_desc,
    ),
    NotificationItem(
        category = NotifCategory.TRAINING,
        icon = R.drawable.ic_target,
        accent = NotifAccent.AMBER,
        title = R.string.notif2_title,
        time = R.string.notif2_time,
        description = R.string.notif2_desc,
    ),
    NotificationItem(
        category = NotifCategory.PLACEMENT,
        icon = R.drawable.ic_work,
        accent = NotifAccent.GREEN,
        title = R.string.notif3_title,
        time = R.string.notif3_time,
        description = R.string.notif3_desc,
    ),
    NotificationItem(
        category = NotifCategory.EXAMS,
        icon = R.drawable.ic_note_add,
        accent = NotifAccent.BLUE,
        title = R.string.notif4_title,
        time = R.string.notif4_time,
        description = R.string.notif4_desc,
    ),
    NotificationItem(
        category = NotifCategory.EXAMS,
        icon = R.drawable.ic_error,
        accent = NotifAccent.AMBER,
        title = R.string.notif5_title,
        time = R.string.notif5_time,
        description = R.string.notif5_desc,
    ),
)

/**
 * Notifications screen, reached from the home-screen bell. Hardcoded content;
 * chips filter the list by category.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(NotifCategory.ALL) }
    val visible = if (selected == NotifCategory.ALL) {
        notifications
    } else {
        notifications.filter { it.category == selected }
    }

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
                text = stringResource(R.string.notif_title),
                color = TextPrimary,
                fontSize = AppTextSize.H4,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(Modifier.height(16.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NotifCategory.entries.forEach { category ->
                CategoryChip(
                    text = stringResource(category.label),
                    selected = category == selected,
                    onClick = { selected = category },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.notif_section_today))
        Spacer(Modifier.height(12.dp))

        if (visible.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.notif_empty),
                    color = TextSecondary,
                    fontSize = AppTextSize.Body,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visible.forEach { item -> NotificationCard(item) }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NotificationCard(item: NotificationItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ScreenBackground,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            IconBadge(
                iconRes = item.icon,
                contentDescription = null,
                size = 44.dp,
                cornerRadius = 12.dp,
                iconSize = 22.dp,
                containerColor = item.accent.background,
                iconTint = item.accent.tint,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = stringResource(item.title),
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(item.time),
                        color = TextMuted,
                        fontSize = AppTextSize.CaptionSmall,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(item.description),
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationsPreview() {
    GRADit_RewampTheme {
        NotificationsScreen(onBack = {})
    }
}
