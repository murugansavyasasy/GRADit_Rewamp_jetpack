package com.vsca.vsnapvoicecollege.ui.dashboard.placement.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.MessageCard
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.RetryButton
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementLoad
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.shortDate
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.syncedLabel
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** The student's running placement trainings with their progress; a row opens the training. */
@Composable
fun StudentPlacementTrainingScreen(
    onBack: () -> Unit,
    onOpenTraining: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentTrainingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentTrainingContent(uiState, onBack, viewModel::onQueryChange, viewModel::refresh, onOpenTraining, modifier)
}

@Composable
private fun StudentTrainingContent(
    uiState: StudentTrainingUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenTraining: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") { TitledTopBar(title = stringResource(R.string.training_title), onBack = onBack) }
        if (!uiState.isOnline && uiState.load != PlacementLoad.ERROR) {
            item(key = "offline") {
                OfflineBanner(
                    stringResource(R.string.placement_offline_banner, syncedLabel(uiState.lastSyncedAt)),
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        item(key = "search") {
            SearchBar(
                query = uiState.query,
                onQueryChange = onQuery,
                hint = stringResource(R.string.placement_student_search_hint),
                containerColor = White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
            )
        }
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
            uiState.trainings.isEmpty() -> item(key = "empty") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_target,
                        iconTint = BrandBlue,
                        iconBackground = SelectedRowBackground,
                        title = stringResource(if (uiState.query.isBlank()) R.string.training_none_upcoming_title else R.string.training_no_match_title),
                        subtitle = stringResource(if (uiState.query.isBlank()) R.string.training_none_body else R.string.training_student_no_match_body),
                    )
                }
            }
            else -> item(key = "trainings") {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = White,
                    border = BorderStroke(1.dp, FieldBorder),
                ) {
                    Column {
                        uiState.trainings.forEachIndexed { index, training ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FieldBorder)
                            StudentTrainingRow(training, onClick = { onOpenTraining(training.id) })
                        }
                    }
                }
            }
        }
    }
}

/** "Aptitude Bootcamp / Mon & Wed · 4:15 PM · Room C-101 / 8 of 12 done". */
@Composable
private fun StudentTrainingRow(training: PlacementTraining, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val next = training.nextSession(now) ?: training.firstStart
    val place = if (training.mode.isInPerson) training.venue else stringResource(R.string.training_online, training.platform ?: training.venue)
    val details = when {
        training.registration == TrainingRegistration.SLOT_BOOKING -> stringResource(R.string.training_slots_from, shortDate(training.firstStart))
        training.repeat == TrainingRepeat.Once -> stringResource(R.string.training_session_row, dayMonthLabel(next), clockLabel(next))
        else -> listOfNotNull(shortDaysLabel(training.repeat), clockLabel(next), place).joinToString(" · ")
    }
    val status = when {
        training.registration == TrainingRegistration.REGISTERED -> stringResource(R.string.training_registered)
        training.registration == TrainingRegistration.SLOT_BOOKING -> stringResource(R.string.training_book_slot)
        training.hasStarted(now) -> stringResource(R.string.training_progress, training.sessionsDone(now), training.sessions.size)
        else -> shortDate(training.firstStart)
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).background(SelectedRowBackground, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_target), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(training.title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
            Spacer(Modifier.height(2.dp))
            Text(details, color = TextSecondary, fontSize = AppTextSize.Label, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Text(status, color = TextPrimary, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StudentPlacementTrainingPreview() {
    GRADit_RewampTheme {
        val now = System.currentTimeMillis()
        StudentTrainingContent(
            uiState = StudentTrainingUiState(
                load = PlacementLoad.READY,
                trainings = PlacementTrainingRepository.trainings.value.filter { !it.hasEnded(now) && it.isFor(2024) }.sortedBy { it.nextSession(now) },
            ),
            onBack = {}, onQuery = {}, onRetry = {}, onOpenTraining = {},
        )
    }
}
