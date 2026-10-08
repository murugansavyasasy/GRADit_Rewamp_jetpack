package com.vsca.vsnapvoicecollege.ui.dashboard.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.IconBadge
import com.vsca.vsnapvoicecollege.ui.components.LogoBadge
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.IconButtonBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SearchFieldBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

private data class QuickAction(
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
)

private val quickActions = listOf(
    QuickAction(R.drawable.ic_calendar, R.string.qa_attendance),
    QuickAction(R.drawable.ic_book, R.string.qa_assignment),
    QuickAction(R.drawable.ic_note_add, R.string.qa_exam),
    QuickAction(R.drawable.ic_credit_card, R.string.qa_fee),
    QuickAction(R.drawable.ic_campaign, R.string.qa_notice),
    QuickAction(R.drawable.ic_calendar, R.string.qa_events),
    QuickAction(R.drawable.ic_videocam, R.string.qa_video),
    QuickAction(R.drawable.ic_forum, R.string.qa_discussion),
    QuickAction(R.drawable.ic_chat, R.string.qa_text_msg),
    QuickAction(R.drawable.ic_mic, R.string.qa_voice_msg),
    QuickAction(R.drawable.ic_image, R.string.qa_image_pdf),
    QuickAction(R.drawable.ic_people, R.string.qa_faculty),
    QuickAction(R.drawable.ic_menu_book, R.string.qa_course),
    QuickAction(R.drawable.ic_star, R.string.qa_category_credit),
    QuickAction(R.drawable.ic_bar_chart, R.string.qa_sem_credit),
    QuickAction(R.drawable.ic_resume, R.string.qa_resume),
    QuickAction(R.drawable.ic_work, R.string.qa_placement_events),
    QuickAction(R.drawable.ic_target, R.string.qa_placement_training),
)

/** How many actions show before the grid is expanded (the rest sit behind "More"). */
private const val COLLAPSED_ACTION_COUNT = 7

/** Items per grid row. */
private const val GRID_COLUMNS = 4

/**
 * Student home screen. Hardcoded content — API integration comes later.
 */
@Composable
fun StudentHomeScreen(
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        HomeHeader(
            onSearchClick = {
                searchActive = !searchActive
                if (!searchActive) query = ""
            },
            onNotificationsClick = onOpenNotifications,
        )
        Spacer(Modifier.height(16.dp))
        AlertBanner()
        Spacer(Modifier.height(16.dp))
        if (searchActive) {
            BorderedCard(modifier = Modifier.fillMaxWidth()) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    hint = stringResource(R.string.home_search_hint),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        QuickActionsCard(filterQuery = if (searchActive) query else null)
        Spacer(Modifier.height(16.dp))
        AttendanceCard()
        Spacer(Modifier.height(16.dp))
        TodaysClassesCard()
        Spacer(Modifier.height(20.dp))
        UpcomingEventsSection()
        Spacer(Modifier.height(20.dp))
        AssignmentsDueSection()
        Spacer(Modifier.height(20.dp))
        NoticeBoardSection()
        Spacer(Modifier.height(20.dp))
        ChatSection()
        Spacer(Modifier.height(24.dp))
    }
}

/** White, thin-bordered container used by the home feed cards. */
@Composable
private fun BorderedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        content()
    }
}

@Composable
private fun UpcomingEventsSection() {
    Column {
        SectionHeader(
            title = stringResource(R.string.home_upcoming_title),
            actionText = stringResource(R.string.home_see_all),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EventCard(
                date = stringResource(R.string.home_event1_date),
                title = stringResource(R.string.home_event1_title),
                location = stringResource(R.string.home_event1_location),
                modifier = Modifier.weight(1f),
            )
            EventCard(
                date = stringResource(R.string.home_event2_date),
                title = stringResource(R.string.home_event2_title),
                location = stringResource(R.string.home_event2_location),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EventCard(
    date: String,
    title: String,
    location: String,
    modifier: Modifier = Modifier,
) {
    BorderedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Tag(text = date, background = SelectedRowBackground, textColor = BrandBlue)
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(text = location, color = TextSecondary, fontSize = AppTextSize.Caption)
        }
    }
}

@Composable
private fun AssignmentsDueSection() {
    Column {
        SectionHeader(
            title = stringResource(R.string.home_assignments_title),
            actionText = stringResource(R.string.home_see_all),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssignmentCard(
                course = stringResource(R.string.home_assign1_course),
                title = stringResource(R.string.home_assign1_title),
                due = stringResource(R.string.home_assign1_due),
                dueUrgent = true,
                modifier = Modifier.weight(1f),
            )
            AssignmentCard(
                course = stringResource(R.string.home_assign2_course),
                title = stringResource(R.string.home_assign2_title),
                due = stringResource(R.string.home_assign2_due),
                dueUrgent = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AssignmentCard(
    course: String,
    title: String,
    due: String,
    dueUrgent: Boolean,
    modifier: Modifier = Modifier,
) {
    BorderedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = course,
                color = BrandBlue,
                fontSize = AppTextSize.Caption,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            if (dueUrgent) {
                Tag(text = due, background = AmberChipBackground, textColor = AmberText)
            } else {
                Text(
                    text = due,
                    color = BrandBlue,
                    fontSize = AppTextSize.Caption,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun NoticeBoardSection() {
    Column {
        SectionHeader(
            title = stringResource(R.string.home_notice_title),
            actionText = stringResource(R.string.home_see_all),
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NoticeRow(
                title = stringResource(R.string.home_notice1_title),
                meta = stringResource(R.string.home_notice1_meta),
            )
            NoticeRow(
                title = stringResource(R.string.home_notice2_title),
                meta = stringResource(R.string.home_notice2_meta),
            )
        }
    }
}

@Composable
private fun NoticeRow(title: String, meta: String) {
    BorderedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                iconRes = R.drawable.ic_campaign,
                contentDescription = null,
                size = 40.dp,
                cornerRadius = 12.dp,
                iconSize = 20.dp,
                containerColor = SelectedRowBackground,
                iconTint = BrandBlue,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = AppTextSize.Body,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(text = meta, color = TextSecondary, fontSize = AppTextSize.Caption)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.home_notice_open_desc),
                tint = TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ChatSection() {
    Column {
        SectionHeader(
            title = stringResource(R.string.home_chat_title),
            actionText = stringResource(R.string.home_see_all),
        )
        Spacer(Modifier.height(12.dp))
        ChatRow(
            initials = stringResource(R.string.home_chat1_initials),
            name = stringResource(R.string.home_chat1_name),
            time = stringResource(R.string.home_chat1_time),
            message = stringResource(R.string.home_chat1_message),
            unread = stringResource(R.string.home_chat1_unread),
        )
        Spacer(Modifier.height(12.dp))
        AdRow(
            label = stringResource(R.string.home_ad_label),
            message = stringResource(R.string.home_ad_message),
        )
    }
}

@Composable
private fun ChatRow(
    initials: String,
    name: String,
    time: String,
    message: String,
    unread: String,
) {
    BorderedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SelectedRowBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    color = BrandBlue,
                    fontSize = AppTextSize.Caption,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = time, color = TextMuted, fontSize = AppTextSize.CaptionSmall)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message,
                        modifier = Modifier.weight(1f),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    UnreadBadge(count = unread)
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: String) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(BrandBlue),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count,
            color = White,
            fontSize = AppTextSize.CaptionSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AdRow(label: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SelectedRowBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = AppTextSize.CaptionSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = BrandBlue,
                fontSize = AppTextSize.Caption,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeHeader(onSearchClick: () -> Unit, onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LogoBadge(
            size = 48.dp,
            cornerRadius = 14.dp,
            iconSize = 26.dp,
            containerColor = BrandBlue,
            iconTint = White,
            shadowElevation = 0.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_greeting, stringResource(R.string.home_user_name)),
                color = TextPrimary,
                fontSize = AppTextSize.H6,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RolePill(text = stringResource(R.string.home_role_student))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_college),
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                )
            }
        }
        CircleIconButton(
            iconRes = R.drawable.ic_search,
            contentDescription = stringResource(R.string.search_content_desc),
            onClick = onSearchClick,
        )
        Spacer(Modifier.width(10.dp))
        CircleIconButton(
            iconRes = R.drawable.ic_notifications,
            contentDescription = stringResource(R.string.home_notifications_desc),
            showBadge = true,
            onClick = onNotificationsClick,
        )
    }
}

@Composable
private fun RolePill(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = TextPrimary) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            color = White,
            fontSize = AppTextSize.CaptionSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CircleIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    showBadge: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = White,
            border = BorderStroke(1.dp, IconButtonBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BadgeRed),
            )
        }
    }
}

@Composable
private fun AlertBanner() {
    Surface(shape = RoundedCornerShape(14.dp), color = AlertBackground) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_error),
                contentDescription = null,
                tint = BadgeRed,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.home_alert),
                modifier = Modifier.weight(1f),
                color = BadgeRed,
                fontSize = AppTextSize.Caption,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.home_alert_dismiss),
                tint = BadgeRed,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun HomeCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        content()
    }
}

/**
 * The quick-actions menu grid.
 *
 * When [filterQuery] is null the grid is in its normal state: the first
 * [COLLAPSED_ACTION_COUNT] actions plus a More/Less toggle. When non-null the
 * grid is in search mode and shows only the actions whose label matches the
 * query (all of them when the query is blank), with no toggle.
 */
@Composable
private fun QuickActionsCard(filterQuery: String?) {
    if (filterQuery == null) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val visibleActions = if (expanded) quickActions else quickActions.take(COLLAPSED_ACTION_COUNT)

        val cells: List<@Composable (Modifier) -> Unit> = buildList {
            visibleActions.forEach { action ->
                add { cellModifier -> QuickActionItem(action, modifier = cellModifier) }
            }
            add { cellModifier ->
                QuickActionToggle(
                    expanded = expanded,
                    onClick = { expanded = !expanded },
                    modifier = cellModifier,
                )
            }
        }
        QuickActionsGrid(cells)
        return
    }

    // Search mode — filter the menu cards by their (resolved) label.
    val matches = quickActions.filter { action ->
        filterQuery.isBlank() || stringResource(action.label).contains(filterQuery, ignoreCase = true)
    }
    if (matches.isEmpty()) {
        HomeCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_search_no_results),
                    color = TextSecondary,
                    fontSize = AppTextSize.Body,
                )
            }
        }
    } else {
        QuickActionsGrid(matches.map { action -> { cellModifier -> QuickActionItem(action, modifier = cellModifier) } })
    }
}

@Composable
private fun QuickActionsGrid(cells: List<@Composable (Modifier) -> Unit>) {
    HomeCard {
        Column(modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp)) {
            cells.chunked(GRID_COLUMNS).forEachIndexed { index, rowCells ->
                if (index > 0) Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowCells.forEach { cell -> cell(Modifier.weight(1f)) }
                    // Keep a short final row left-aligned by padding the remaining columns.
                    repeat(GRID_COLUMNS - rowCells.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun QuickActionToggle(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconRes = if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
    val label = stringResource(if (expanded) R.string.qa_less else R.string.qa_more)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(
            iconRes = iconRes,
            contentDescription = label,
            size = 52.dp,
            cornerRadius = 16.dp,
            iconSize = 24.dp,
            containerColor = SelectedRowBackground,
            iconTint = BrandBlue,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = AppTextSize.CaptionSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun QuickActionItem(action: QuickAction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(
            iconRes = action.icon,
            contentDescription = stringResource(action.label),
            size = 52.dp,
            cornerRadius = 16.dp,
            iconSize = 24.dp,
            containerColor = SelectedRowBackground,
            iconTint = BrandBlue,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(action.label),
            color = TextPrimary,
            fontSize = AppTextSize.CaptionSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun AttendanceCard() {
    HomeCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttendanceRing(percent = 0.92f, label = stringResource(R.string.home_att_percent))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_att_header),
                    color = TextMuted,
                    fontSize = AppTextSize.CaptionSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_att_status),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_att_next),
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                )
            }
        }
    }
}

@Composable
private fun AttendanceRing(percent: Float, label: String) {
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = SelectedRowBackground,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = BrandBlue,
                startAngle = -90f,
                sweepAngle = 360f * percent,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = label,
            color = BrandBlue,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun TodaysClassesCard() {
    HomeCard {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = stringResource(R.string.home_classes_title),
                actionText = stringResource(R.string.home_timetable),
            )
            Spacer(Modifier.height(14.dp))
            ClassItem(
                period = stringResource(R.string.home_class1_period),
                time = stringResource(R.string.home_class1_time),
                title = stringResource(R.string.home_class1_title),
                subtitle = stringResource(R.string.home_class1_sub),
                primaryTag = stringResource(R.string.home_class1_tag_now),
                secondaryTag = stringResource(R.string.home_class1_tag_swapped),
                highlighted = true,
            )
            Spacer(Modifier.height(12.dp))
            ClassItem(
                period = stringResource(R.string.home_class2_period),
                time = stringResource(R.string.home_class2_time),
                title = stringResource(R.string.home_class2_title),
                subtitle = stringResource(R.string.home_class2_sub),
                primaryTag = stringResource(R.string.home_class2_tag_next),
                secondaryTag = stringResource(R.string.home_class2_tag_changed),
                highlighted = false,
            )
        }
    }
}

@Composable
private fun ClassItem(
    period: String,
    time: String,
    title: String,
    subtitle: String,
    primaryTag: String,
    secondaryTag: String,
    highlighted: Boolean,
) {
    val borderColor = if (highlighted) BrandBlue else AmberText.copy(alpha = 0.5f)
    val background = if (highlighted) SelectedRowBackground else White
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (highlighted) BrandBlue else BadgeNeutralBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = period,
                    color = if (highlighted) White else BrandBlue,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(time, color = TextSecondary, fontSize = AppTextSize.CaptionSmall)
                Spacer(Modifier.height(2.dp))
                Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Caption)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (highlighted) {
                    Tag(primaryTag, background = BrandBlue, textColor = White)
                } else {
                    Tag(primaryTag, background = SearchFieldBackground, textColor = TextSecondary)
                }
                Spacer(Modifier.height(6.dp))
                if (highlighted) {
                    Tag(secondaryTag, background = SelectedRowBackground, textColor = BrandBlue)
                } else {
                    Tag(secondaryTag, background = AmberChipBackground, textColor = AmberText)
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, background: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = textColor,
            fontSize = AppTextSize.CaptionSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionHeader(title: String, actionText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = AppTextSize.H6,
            fontWeight = FontWeight.ExtraBold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { /* TODO */ },
        ) {
            Text(
                text = actionText,
                color = BrandBlue,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
