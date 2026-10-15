package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.FilterDropdown
import com.vsca.vsnapvoicecollege.ui.components.IconLine
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Management / faculty notice board: notices with status tabs, filters, read progress
 * and a "+" button to create a new one.
 */
@Composable
fun NoticeBoardScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoticeBoardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NoticeBoardContent(
        uiState = uiState,
        onBack = onBack,
        onQuery = viewModel::onQueryChange,
        onCategory = viewModel::onCategory,
        onAudience = viewModel::onAudience,
        onStatus = viewModel::onStatus,
        onSort = viewModel::onSort,
        onRetry = viewModel::refresh,
        onCreate = onCreate,
        onOpenNotice = onOpenNotice,
        modifier = modifier,
    )
}

@Composable
private fun NoticeBoardContent(
    uiState: NoticeBoardUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onCategory: (NoticeCategory?) -> Unit,
    onAudience: (NoticeRole?) -> Unit,
    onStatus: (NoticeStatus?) -> Unit,
    onSort: (NoticeSort) -> Unit,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onOpenNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = uiState.options
    val all = stringResource(R.string.notice_filter_all)
    val empty = uiState.load == NoticeLoad.READY && (uiState.counts[null] ?: 0) == 0 &&
        options.query.isBlank() && options.category == null && options.audience == null
    Box(modifier = modifier.fillMaxSize().background(ScreenBackground)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item(key = "header") {
                TitledTopBar(
                    title = stringResource(R.string.notice_board_title),
                    subtitle = uiState.author?.let { stringResource(R.string.notice_author, it.name, it.title) },
                    onBack = onBack,
                )
            }
            item(key = "search") {
                SearchBar(
                    query = options.query,
                    onQueryChange = onQuery,
                    hint = stringResource(R.string.notice_search_title_hint),
                    containerColor = White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
                )
            }
            item(key = "filters") { StaffFilters(options, all, onCategory, onAudience, onStatus, onSort) }
            item(key = "tabs") { StatusTabs(options.status, uiState.counts, onStatus) }
            when {
                uiState.load == NoticeLoad.LOADING -> item(key = "loading") { SkeletonCards(3, Modifier.padding(16.dp)) }
                uiState.load == NoticeLoad.ERROR -> item(key = "error") {
                    MessageCard {
                        StateMessage(
                            iconRes = R.drawable.ic_wifi,
                            iconTint = BadgeRed,
                            iconBackground = AlertBackground,
                            title = stringResource(R.string.notice_error_staff_title),
                            subtitle = stringResource(R.string.notice_error_body),
                        ) { RetryButton(onRetry) }
                    }
                }
                empty -> item(key = "empty") {
                    MessageCard {
                        StateMessage(
                            iconRes = R.drawable.ic_campaign,
                            iconTint = BrandBlue,
                            iconBackground = SelectedRowBackground,
                            title = stringResource(R.string.notice_empty_title),
                            subtitle = stringResource(R.string.notice_empty_staff_body),
                        ) {
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = onCreate,
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                            ) {
                                Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.notice_create), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                uiState.notices.isEmpty() -> item(key = "none") {
                    Text(
                        stringResource(R.string.notice_no_matches),
                        Modifier.fillMaxWidth().padding(32.dp),
                        color = TextSecondary,
                        fontSize = AppTextSize.Body,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> items(uiState.notices, key = { it.id }) { notice ->
                    StaffNoticeCard(notice, onClick = { onOpenNotice(notice.id) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp))
                }
            }
        }
        if (!empty) {
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(68.dp),
                shape = CircleShape,
                containerColor = BrandBlue,
                contentColor = White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            ) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.notice_create), modifier = Modifier.size(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffFilters(
    options: NoticeBoardOptions,
    all: String,
    onCategory: (NoticeCategory?) -> Unit,
    onAudience: (NoticeRole?) -> Unit,
    onStatus: (NoticeStatus?) -> Unit,
    onSort: (NoticeSort) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val categoryValue = options.category?.let { stringResource(it.label) } ?: all
        FilterDropdown(
            label = stringResource(R.string.notice_filter_category),
            value = categoryValue,
            selected = categoryValue,
            options = listOf<Pair<String, () -> Unit>>(all to { onCategory(null) }) +
                NoticeCategory.entries.map { c -> stringResource(c.label) to { onCategory(c) } },
        )
        val audienceValue = options.audience?.let { stringResource(it.label) } ?: all
        FilterDropdown(
            label = stringResource(R.string.notice_filter_audience),
            value = audienceValue,
            selected = audienceValue,
            options = listOf<Pair<String, () -> Unit>>(all to { onAudience(null) }) +
                NoticeRole.entries.map { r -> stringResource(r.label) to { onAudience(r) } },
        )
        val statusValue = options.status?.let { stringResource(it.label) } ?: all
        FilterDropdown(
            label = stringResource(R.string.notice_filter_status),
            value = statusValue,
            selected = statusValue,
            options = listOf<Pair<String, () -> Unit>>(all to { onStatus(null) }) +
                NoticeStatus.entries.map { s -> stringResource(s.label) to { onStatus(s) } },
        )
        val sortValue = stringResource(options.sort.label)
        FilterDropdown(
            label = null,
            value = sortValue,
            selected = sortValue,
            iconRes = R.drawable.ic_sort,
            options = NoticeSort.entries.map { s -> stringResource(s.label) to { onSort(s) } },
        )
    }
}

/** All / Published / Scheduled / Drafts / Expired with count bubbles and an underline. */
@Composable
private fun StatusTabs(selected: NoticeStatus?, counts: Map<NoticeStatus?, Int>, onSelect: (NoticeStatus?) -> Unit) {
    val tabs: List<Pair<NoticeStatus?, Int>> = listOf<Pair<NoticeStatus?, Int>>(null to R.string.notice_tab_all) +
        NoticeStatus.entries.map { it to it.tab }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            tabs.forEach { (status, label) ->
                val isSelected = status == selected
                Column(
                    modifier = Modifier.clickable { onSelect(status) }.padding(top = 6.dp).width(82.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(shape = RoundedCornerShape(50), color = if (isSelected) BrandBlue else BadgeNeutralBackground) {
                        Text(
                            (counts[status] ?: 0).toString(),
                            Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                            color = if (isSelected) White else TextPrimary,
                            fontSize = AppTextSize.Caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(label),
                        color = if (isSelected) BrandBlue else TextSecondary,
                        fontSize = AppTextSize.Body,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (isSelected) BrandBlue else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = FieldBorder)
    }
}

@Composable
private fun StaffNoticeCard(notice: Notice, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = notice.status()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    notice.title,
                    Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = AppTextSize.H6,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 25.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(10.dp))
                StatusPill(status)
            }
            Spacer(Modifier.height(10.dp))
            NoticePills(notice, showPinned = notice.pinned && status != NoticeStatus.EXPIRED)
            Spacer(Modifier.height(12.dp))
            IconLine(R.drawable.ic_people_outline, audienceSummary(notice))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconLine(R.drawable.ic_calendar, timeSummary(notice), modifier = Modifier.weight(1f, fill = false))
                if (notice.attachments.isNotEmpty()) {
                    Spacer(Modifier.width(14.dp))
                    IconLine(
                        R.drawable.ic_attach_file,
                        pluralStringResource(R.plurals.notice_attachments, notice.attachments.size, notice.attachments.size),
                    )
                }
            }
            if (status == NoticeStatus.PUBLISHED || status == NoticeStatus.EXPIRED) {
                Spacer(Modifier.height(12.dp))
                ReadProgress(notice.readCount, notice.recipientCount)
            }
        }
    }
}

/** "B.E. CSE Sem 5 A, B · Students, Parents", or a prompt when no audience is set yet. */
@Composable
internal fun audienceSummary(notice: Notice): String =
    if (notice.classes.isEmpty() || notice.roles.isEmpty()) {
        stringResource(R.string.notice_no_audience)
    } else {
        stringResource(R.string.notice_audience_line, classesLabel(notice.classes), rolesLabel(notice.roles))
    }

@Composable
internal fun rolesLabel(roles: Set<NoticeRole>): String =
    NoticeRole.entries.filter { it in roles }.map { stringResource(it.label) }.joinToString(", ")

@Composable
private fun timeSummary(notice: Notice): String = when (notice.status()) {
    NoticeStatus.SCHEDULED -> stringResource(R.string.notice_goes_out, shortTimeLabel(notice.scheduledAt ?: notice.effectiveTime))
    NoticeStatus.DRAFT -> stringResource(R.string.notice_edited, shortTimeLabel(notice.createdAt))
    NoticeStatus.EXPIRED -> stringResource(R.string.notice_expired_on, plainDate(notice.expiresAt ?: notice.effectiveTime))
    NoticeStatus.PUBLISHED -> stringResource(R.string.notice_published_at, shortTimeLabel(notice.effectiveTime))
}

@Composable
internal fun MessageCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 32.dp)) { content() }
    }
}

@Composable
internal fun RetryButton(onRetry: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = onRetry,
        modifier = Modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
    ) {
        Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.notice_try_again), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NoticeBoardPreview() {
    GRADit_RewampTheme {
        val notices = NoticeRepository.notices.value
        NoticeBoardContent(
            uiState = NoticeBoardUiState(
                load = NoticeLoad.READY,
                author = NoticeAuthor("Dr. Suresh Kumar", "SK", "Principal"),
                counts = mapOf<NoticeStatus?, Int>(null to notices.size) + NoticeStatus.entries.associateWith { s -> notices.count { it.status() == s } },
                notices = NoticeSort.NEWEST.apply(notices),
            ),
            onBack = {}, onQuery = {}, onCategory = {}, onAudience = {}, onStatus = {}, onSort = {}, onRetry = {}, onCreate = {}, onOpenNotice = {},
        )
    }
}
