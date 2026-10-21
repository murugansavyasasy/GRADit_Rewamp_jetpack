package com.vsca.vsnapvoicecollege.ui.dashboard.placement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.FilterDropdown
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.MessageCard
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.RetryButton
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.util.Locale

/** Placement events for parents: what's coming up, what has ended, and which children can attend. */
@Composable
fun PlacementEventsScreen(
    onBack: () -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlacementEventsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlacementEventsContent(
        uiState = uiState,
        onBack = onBack,
        onTab = viewModel::onTab,
        onQuery = viewModel::onQueryChange,
        onMode = viewModel::onMode,
        onCourse = viewModel::onCourse,
        onRetry = viewModel::refresh,
        onOpenEvent = onOpenEvent,
        modifier = modifier,
    )
}

@Composable
private fun PlacementEventsContent(
    uiState: PlacementEventsUiState,
    onBack: () -> Unit,
    onTab: (PlacementTab) -> Unit,
    onQuery: (String) -> Unit,
    onMode: (PlacementMode?) -> Unit,
    onCourse: (String?) -> Unit,
    onRetry: () -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.placement_title),
                subtitle = stringResource(R.string.placement_subtitle),
                onBack = onBack,
            )
        }
        if (!uiState.isOnline && uiState.load != PlacementLoad.ERROR) {
            item(key = "offline") {
                OfflineBanner(
                    stringResource(R.string.placement_offline_banner, syncedLabel(uiState.lastSyncedAt)),
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        uiState.soon.firstOrNull()?.let { next ->
            item(key = "soon") { SoonBanner(next, uiState.soon.size, onClick = { onOpenEvent(next.id) }) }
        }
        item(key = "tabs") {
            Spacer(Modifier.height(8.dp))
            PlacementTabs(
                selected = uiState.tab,
                counts = mapOf(PlacementTab.UPCOMING to uiState.upcomingCount, PlacementTab.HISTORICAL to uiState.historicalCount),
                onSelect = onTab,
            )
        }
        item(key = "search") {
            SearchBar(
                query = uiState.query,
                onQueryChange = onQuery,
                hint = stringResource(R.string.placement_search_hint),
                containerColor = White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp).border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
            )
        }
        item(key = "filters") { Filters(uiState, onMode, onCourse) }
        when {
            uiState.load == PlacementLoad.LOADING -> item(key = "loading") { SkeletonCards(2, Modifier.padding(16.dp)) }
            uiState.load == PlacementLoad.ERROR -> item(key = "error") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_wifi,
                        iconTint = BadgeRed,
                        iconBackground = AlertBackground,
                        title = stringResource(R.string.placement_error_title),
                        subtitle = stringResource(R.string.placement_error_body),
                    ) { RetryButton(onRetry) }
                }
            }
            uiState.isEmpty -> item(key = "empty") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_work,
                        iconTint = BrandBlue,
                        iconBackground = SelectedRowBackground,
                        title = stringResource(
                            when {
                                uiState.isFiltered -> R.string.placement_no_match_title
                                uiState.tab == PlacementTab.UPCOMING -> R.string.placement_none_upcoming_title
                                else -> R.string.placement_none_historical_title
                            },
                        ),
                        subtitle = stringResource(if (uiState.isFiltered) R.string.placement_no_match_body else R.string.placement_none_body),
                    )
                }
            }
            else -> uiState.sections.forEach { (section, events) ->
                item(key = "section-" + section.name) {
                    Text(
                        stringResource(section.label).uppercase(Locale.getDefault()),
                        Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                        color = TextSecondary,
                        fontSize = AppTextSize.Label,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                    )
                }
                items(events, key = { it.id }) { event ->
                    PlacementEventCard(
                        event = event,
                        family = uiState.family,
                        soon = event in uiState.soon,
                        onClick = { onOpenEvent(event.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    )
                }
            }
        }
        item(key = "footer") { PlacementFooter(Modifier.padding(top = 16.dp)) }
    }
}

/** "1 event in the next 2 days — Freshworks Pre-placement Talk, tomorrow at 10:00 AM". */
@Composable
private fun SoonBanner(next: PlacementEvent, count: Int, onClick: () -> Unit) {
    val time = clockLabel(next.startAt)
    val whenText = if (daysFromToday(next.startAt) == 0) {
        stringResource(R.string.placement_soon_today, next.title, time)
    } else {
        stringResource(R.string.placement_soon_tomorrow, next.title, time)
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = SoonBannerBackground,
        border = BorderStroke(1.dp, SoonBorder),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = OnboardingAmberCard, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pluralStringResource(R.plurals.placement_soon_count, count, count, PlacementEventsUiState.REMINDER_DAYS),
                    color = OnboardingAmberCard,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(whenText, color = OnboardingAmberCard, fontSize = AppTextSize.Label, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
            }
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = OnboardingAmberCard, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Filters(uiState: PlacementEventsUiState, onMode: (PlacementMode?) -> Unit, onCourse: (String?) -> Unit) {
    val all = stringResource(R.string.placement_filter_all)
    val modeLabels = PlacementMode.entries.associateWith { stringResource(it.label) }
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterDropdown(
            label = stringResource(R.string.placement_filter_mode),
            value = uiState.mode?.let(modeLabels::getValue) ?: all,
            options = listOf(all to { onMode(null) }) + PlacementMode.entries.map { modeLabels.getValue(it) to { onMode(it) } },
            selected = uiState.mode?.let(modeLabels::getValue) ?: all,
            highlighted = uiState.mode != null,
        )
        FilterDropdown(
            label = stringResource(R.string.placement_filter_course),
            value = uiState.course ?: all,
            options = listOf(all to { onCourse(null) }) + uiState.courses.map { it to { onCourse(it) } },
            selected = uiState.course ?: all,
            highlighted = uiState.course != null,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlacementEventCard(
    event: PlacementEvent,
    family: List<PlacementChild>,
    soon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ended = event.hasEnded()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, if (soon) SoonBorder else FieldBorder),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                DateTile(
                    event.startAt,
                    when {
                        ended -> DateTileStyle.ENDED
                        soon -> DateTileStyle.SOON
                        else -> DateTileStyle.UPCOMING
                    },
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    EventPills(event)
                    Spacer(Modifier.height(8.dp))
                    Text(event.title, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(event.subtitle, color = TextSecondary, fontSize = AppTextSize.Body, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(12.dp))
            EventInfoLine(R.drawable.ic_clock, timeRangeLabel(event))
            Spacer(Modifier.height(6.dp))
            EventInfoLine(if (event.mode.isInPerson) R.drawable.ic_place_outline else R.drawable.ic_videocam_outline, event.venue)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = FieldBorder)
            Spacer(Modifier.height(12.dp))
            FieldCaption(
                if (event.companies.size == 1) {
                    stringResource(R.string.placement_company)
                } else {
                    stringResource(R.string.placement_companies_count, event.companies.size)
                },
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                event.companies.take(MAX_COMPANY_CHIPS).forEach { CompanyChip(it) }
                val more = event.companies.size - MAX_COMPANY_CHIPS
                if (more > 0) {
                    Surface(shape = RoundedCornerShape(50), color = BadgeNeutralBackground, border = BorderStroke(1.dp, FieldBorder)) {
                        Text(
                            stringResource(R.string.placement_more_companies, more),
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = TextSecondary,
                            fontSize = AppTextSize.Body,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FieldCaption(stringResource(R.string.placement_eligible_courses))
            Spacer(Modifier.height(8.dp))
            CourseChips(event.eligibleCourses)
            if (family.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FamilyStrip(event, family)
            }
        }
    }
}

/** "✓ Open to Aarav's and Diya's courses", or a grey line when none of them can attend. */
@Composable
private fun FamilyStrip(event: PlacementEvent, family: List<PlacementChild>) {
    val eligible = family.filter { event.isEligible(it.course) }
    val text = when {
        eligible.isEmpty() -> when (family.size) {
            1 -> stringResource(R.string.placement_closed_to_one, family[0].firstName)
            2 -> stringResource(R.string.placement_closed_to_two, family[0].firstName, family[1].firstName)
            else -> stringResource(R.string.placement_closed_to_all)
        }
        eligible.size == 1 -> stringResource(R.string.placement_open_to_one, eligible[0].firstName)
        eligible.size == 2 -> stringResource(R.string.placement_open_to_two, eligible[0].firstName, eligible[1].firstName)
        else -> pluralStringResource(R.plurals.placement_open_to_many, eligible.size, eligible.size)
    }
    val open = eligible.isNotEmpty()
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = if (open) OnboardingGreenDisc else BadgeNeutralBackground) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(if (open) R.drawable.ic_check else R.drawable.ic_remove_circle_outline),
                contentDescription = null,
                tint = if (open) BrandGreen else TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text, color = if (open) BrandGreen else TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
        }
    }
}

/** Job fairs list many companies; the card shows a few and the detail screen the rest. */
private const val MAX_COMPANY_CHIPS = 3

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlacementEventsPreview() {
    GRADit_RewampTheme {
        val events = PlacementRepository.events.value
        val now = System.currentTimeMillis()
        val upcoming = events.filterNot { it.hasEnded(now) }.sortedBy { it.startAt }
        PlacementEventsContent(
            uiState = PlacementEventsUiState(
                load = PlacementLoad.READY,
                upcomingCount = upcoming.size,
                historicalCount = events.size - upcoming.size,
                soon = upcoming.take(1),
                sections = listOf(PlacementSection.THIS_WEEK to upcoming.take(1), PlacementSection.LATER to upcoming.drop(1)),
                family = PlacementRepository.familyFor(Role.PARENT),
            ),
            onBack = {}, onTab = {}, onQuery = {}, onMode = {}, onCourse = {}, onRetry = {}, onOpenEvent = {},
        )
    }
}
