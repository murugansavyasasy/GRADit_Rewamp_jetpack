package com.vsca.vsnapvoicecollege.ui.dashboard.placement.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
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
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.DateTile
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.DateTileStyle
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.EventInfoLine
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.FieldCaption
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementChild
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementFooter
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementLoad
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementMode
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementSection
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementTab
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementTabs
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.SoonBannerBackground
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.SoonBorder
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.daysFromToday
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.syncedLabel
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

/** Placement training for parents: sessions coming up, what has ended, and whose batch each is for. */
@Composable
fun PlacementTrainingScreen(
    onBack: () -> Unit,
    onOpenTraining: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlacementTrainingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlacementTrainingContent(
        uiState = uiState,
        onBack = onBack,
        onTab = viewModel::onTab,
        onQuery = viewModel::onQueryChange,
        onMode = viewModel::onMode,
        onBatch = viewModel::onBatch,
        onRetry = viewModel::refresh,
        onOpenTraining = onOpenTraining,
        modifier = modifier,
    )
}

@Composable
private fun PlacementTrainingContent(
    uiState: PlacementTrainingUiState,
    onBack: () -> Unit,
    onTab: (PlacementTab) -> Unit,
    onQuery: (String) -> Unit,
    onMode: (PlacementMode?) -> Unit,
    onBatch: (Int?) -> Unit,
    onRetry: () -> Unit,
    onOpenTraining: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.training_title),
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
            item(key = "soon") { SoonBanner(uiState.soon, onClick = { onOpenTraining(next.id) }) }
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
                hint = stringResource(R.string.training_search_hint),
                containerColor = White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp).border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
            )
        }
        item(key = "filters") { Filters(uiState, onMode, onBatch) }
        when {
            uiState.load == PlacementLoad.LOADING -> item(key = "loading") { SkeletonCards(2, Modifier.padding(16.dp)) }
            uiState.load == PlacementLoad.ERROR -> item(key = "error") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_wifi,
                        iconTint = BadgeRed,
                        iconBackground = AlertBackground,
                        title = stringResource(R.string.training_error_title),
                        subtitle = stringResource(R.string.placement_error_body),
                    ) { RetryButton(onRetry) }
                }
            }
            uiState.isEmpty -> item(key = "empty") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_target,
                        iconTint = BrandBlue,
                        iconBackground = SelectedRowBackground,
                        title = stringResource(
                            when {
                                uiState.isFiltered -> R.string.training_no_match_title
                                uiState.tab == PlacementTab.UPCOMING -> R.string.training_none_upcoming_title
                                else -> R.string.training_none_historical_title
                            },
                        ),
                        subtitle = stringResource(if (uiState.isFiltered) R.string.placement_no_match_body else R.string.training_none_body),
                    )
                }
            }
            else -> uiState.sections.forEach { (section, trainings) ->
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
                items(trainings, key = { it.id }) { training ->
                    TrainingCard(
                        training = training,
                        family = uiState.family,
                        soon = training in uiState.soon,
                        onClick = { onOpenTraining(training.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    )
                }
            }
        }
        item(key = "footer") { PlacementFooter(Modifier.padding(top = 16.dp), note = stringResource(R.string.training_footer_note)) }
    }
}

/** "2 sessions in the next 2 days — Data Structures and Algorithms, Today, Wed 07 Oct at 02:00 PM · …". */
@Composable
private fun SoonBanner(soon: List<PlacementTraining>, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val lines = soon.mapNotNull { training ->
        val next = training.nextSession(now) ?: return@mapNotNull null
        if (daysFromToday(next, now) == 0) {
            stringResource(R.string.training_soon_item_today, training.shortTitle, dayMonthLabel(next), clockLabel(next))
        } else {
            stringResource(R.string.training_soon_item, training.shortTitle, dayMonthLabel(next), clockLabel(next))
        }
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
                    pluralStringResource(R.plurals.training_soon_count, soon.size, soon.size, PlacementTrainingUiState.REMINDER_DAYS),
                    color = OnboardingAmberCard,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(lines.joinToString(" · "), color = OnboardingAmberCard, fontSize = AppTextSize.Label, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
            }
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = OnboardingAmberCard, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Filters(uiState: PlacementTrainingUiState, onMode: (PlacementMode?) -> Unit, onBatch: (Int?) -> Unit) {
    val all = stringResource(R.string.placement_filter_all)
    val modeLabels = uiState.modes.associateWith { stringResource(it.label) }
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val mode = uiState.mode?.let { modeLabels[it] } ?: all
        FilterDropdown(
            label = stringResource(R.string.placement_filter_mode),
            value = mode,
            options = listOf(all to { onMode(null) }) + uiState.modes.map { modeLabels.getValue(it) to { onMode(it) } },
            selected = mode,
            highlighted = uiState.mode != null,
        )
        val batch = uiState.batch?.toString() ?: all
        FilterDropdown(
            label = stringResource(R.string.training_filter_batch),
            value = batch,
            options = listOf(all to { onBatch(null) }) + uiState.batches.map { it.toString() to { onBatch(it) } },
            selected = batch,
            highlighted = uiState.batch != null,
        )
    }
}

@Composable
private fun TrainingCard(
    training: PlacementTraining,
    family: List<PlacementChild>,
    soon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val next = training.nextSession(now)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, if (soon) SoonBorder else FieldBorder),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // The next session's day; the first one's once the training has ended.
                DateTile(
                    next ?: training.firstStart,
                    when {
                        next == null -> DateTileStyle.ENDED
                        soon -> DateTileStyle.SOON
                        else -> DateTileStyle.UPCOMING
                    },
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    TrainingPills(training)
                    Spacer(Modifier.height(8.dp))
                    Text(training.title, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
                    if (training.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(training.description, color = TextSecondary, fontSize = AppTextSize.Body, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            EventInfoLine(R.drawable.ic_clock, sessionTimeLabel(training, next ?: training.firstStart))
            Spacer(Modifier.height(6.dp))
            EventInfoLine(if (training.mode.isInPerson) R.drawable.ic_place_outline else R.drawable.ic_videocam_outline, training.venue)
            cardRepeatLabel(training)?.let { repeat ->
                Spacer(Modifier.height(6.dp))
                RepeatLine(repeat)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = FieldBorder)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    FieldCaption(stringResource(R.string.training_applicable_batches))
                    Spacer(Modifier.height(6.dp))
                    BatchChips(training.batches, family.map { it.batch }.toSet())
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    FieldCaption(stringResource(R.string.training_trainer))
                    Text(training.trainer.name, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End)
                }
            }
            if (family.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FamilyBatchStrip(training, family)
            }
        }
    }
}

/** Amber "Repeats every week on … until …" line with the repeat icon. */
@Composable
private fun RepeatLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(painterResource(R.drawable.ic_repeat), contentDescription = null, tint = OnboardingAmberCard, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = OnboardingAmberCard, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
    }
}

/** "✓ For Aarav's batch (2024)", or a grey line when none of the children's batches is listed. */
@Composable
private fun FamilyBatchStrip(training: PlacementTraining, family: List<PlacementChild>) {
    val included = family.filter { training.isFor(it.batch) }
    val text = when {
        included.isEmpty() -> if (family.size == 1) {
            stringResource(R.string.training_not_for_one, family[0].firstName, family[0].batch)
        } else {
            stringResource(R.string.training_not_for_family)
        }
        included.size == 1 -> stringResource(R.string.training_for_one, included[0].firstName, included[0].batch)
        included.size == 2 -> stringResource(
            R.string.training_for_two,
            included[0].firstName, included[0].batch, included[1].firstName, included[1].batch,
        )
        else -> pluralStringResource(R.plurals.training_for_many, included.size, included.size)
    }
    val open = included.isNotEmpty()
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlacementTrainingPreview() {
    GRADit_RewampTheme {
        val now = System.currentTimeMillis()
        val trainings = PlacementTrainingRepository.trainings.value
        val upcoming = trainings.filterNot { it.hasEnded(now) }.sortedBy { it.nextSession(now) }
        PlacementTrainingContent(
            uiState = PlacementTrainingUiState(
                load = PlacementLoad.READY,
                upcomingCount = upcoming.size,
                historicalCount = trainings.size - upcoming.size,
                soon = upcoming.take(2),
                sections = listOf(PlacementSection.THIS_WEEK to upcoming.take(3), PlacementSection.LATER to upcoming.drop(3)),
                family = PlacementRepository.familyFor(Role.PARENT),
            ),
            onBack = {}, onTab = {}, onQuery = {}, onMode = {}, onBatch = {}, onRetry = {}, onOpenTraining = {},
        )
    }
}
