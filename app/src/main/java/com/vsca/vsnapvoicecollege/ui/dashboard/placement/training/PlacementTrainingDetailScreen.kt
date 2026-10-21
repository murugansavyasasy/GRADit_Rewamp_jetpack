package com.vsca.vsnapvoicecollege.ui.dashboard.placement.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.InitialsBadge
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementChild
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementFooter
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PrimaryOutlinedAction
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.SecondaryAction
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.SoonBannerBackground
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.daysFromToday
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.fullDate
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** One training: trainer, actions, the schedule with the next sessions, and whose batch it is for. */
@Composable
fun PlacementTrainingDetailScreen(trainingId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val trainings by PlacementTrainingRepository.trainings.collectAsStateWithLifecycle()
    val training = trainings.firstOrNull { it.id == trainingId }
    val context = LocalContext.current
    val role = remember { SessionPreferences.getRole(context) }
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.training_detail_title),
            subtitle = stringResource(R.string.placement_subtitle),
            onBack = onBack,
        )
        if (training == null) {
            Text(stringResource(R.string.training_not_found), Modifier.padding(32.dp), color = TextSecondary)
        } else {
            PlacementTrainingDetailContent(training, PlacementRepository.familyFor(role))
        }
    }
}

@Composable
private fun PlacementTrainingDetailContent(training: PlacementTraining, family: List<PlacementChild>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        SummaryCard(training)
        ScheduleCard(training)
        BatchesCard(training, family)
        PlacementFooter(Modifier.padding(top = 4.dp), note = stringResource(R.string.training_footer_note))
        Spacer(Modifier.height(16.dp))
    }
}

/** Pills, title, trainer and the actions (none once the training has ended). */
@Composable
private fun SummaryCard(training: PlacementTraining) {
    val context = LocalContext.current
    val next = training.nextSession()
    val schedule = cardRepeatLabel(training) ?: fullDate(training.firstStart)
    val time = sessionTimeLabel(training, next ?: training.firstStart)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(18.dp)) {
            TrainingPills(training)
            Spacer(Modifier.height(12.dp))
            Text(training.title, color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
            if (training.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(training.description, color = TextSecondary, fontSize = AppTextSize.Body, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(14.dp))
            TrainerPanel(training.trainer)
            if (next != null) {
                Spacer(Modifier.height(14.dp))
                if (training.mode.isInPerson) {
                    PrimaryOutlinedAction(R.drawable.ic_map_outline, stringResource(R.string.placement_open_maps)) {
                        openTrainingInMaps(context, training)
                    }
                } else if (training.joinUrl != null) {
                    val platform = training.platform ?: training.venue
                    PrimaryOutlinedAction(
                        R.drawable.ic_videocam_outline,
                        stringResource(if (training.isJoinOpen()) R.string.training_join_now else R.string.training_join_later, platform),
                    ) { openTrainingJoinLink(context, training) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryAction(R.drawable.ic_event_add, stringResource(R.string.placement_add_calendar), Modifier.weight(1f)) {
                        addTrainingToCalendar(context, training)
                    }
                    SecondaryAction(R.drawable.ic_share, stringResource(R.string.placement_share), Modifier.weight(1f)) {
                        shareTraining(context, training, schedule, time)
                    }
                }
            }
        }
    }
}

/** Starts / Time / Repeats / Until / Venue or Platform, then the next few sessions. */
@Composable
private fun ScheduleCard(training: PlacementTraining) {
    val upcoming = training.upcomingSessions().take(MAX_NEXT_SESSIONS)
    DetailCard(stringResource(R.string.training_schedule)) {
        ScheduleRow(stringResource(R.string.training_starts), fullDate(training.firstStart))
        ScheduleRow(stringResource(R.string.training_time), sessionTimeLabel(training))
        if (training.repeat != TrainingRepeat.Once) {
            ScheduleRow(stringResource(R.string.training_repeats), repeatLabel(training.repeat), valueColor = OnboardingAmberCard)
            ScheduleRow(stringResource(R.string.training_until), weekdayDayMonthYear(training.lastStart))
        }
        if (training.mode.isInPerson) {
            ScheduleRow(stringResource(R.string.training_venue), training.venue)
        } else {
            ScheduleRow(stringResource(R.string.training_platform), training.platform ?: training.venue)
        }
        if (upcoming.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.training_next_sessions), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { upcoming.forEach { NextSessionRow(it) } }
        }
    }
}

@Composable
private fun ScheduleRow(label: String, value: String, valueColor: Color = TextPrimary) {
    HorizontalDivider(color = FieldBorder)
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(0.38f), color = TextSecondary, fontSize = AppTextSize.Body)
        Text(value, Modifier.weight(0.62f), color = valueColor, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
    }
}

/** "Mon 12 Oct · 03:05 PM"; today's session sits on amber. */
@Composable
private fun NextSessionRow(sessionStart: Long) {
    val today = daysFromToday(sessionStart) == 0
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (today) SoonBannerBackground else ScreenBackground) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                tint = if (today) OnboardingAmberCard else TextPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.training_session_row, dayMonthLabel(sessionStart), clockLabel(sessionStart)),
                color = if (today) OnboardingAmberCard else TextPrimary,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** "Applicable batches" — green when one of the children's (or the student's) batches is listed. */
@Composable
private fun BatchesCard(training: PlacementTraining, family: List<PlacementChild>) {
    val anyIncluded = family.any { training.isFor(it.batch) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (anyIncluded) IncludedCardBackground else White,
        border = BorderStroke(1.dp, if (anyIncluded) BrandGreen.copy(alpha = 0.7f) else FieldBorder),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.training_applicable_batches), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            BatchChips(training.batches, family.map { it.batch }.toSet())
            if (family.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    family.forEach { child -> ChildBatchRow(child, training.isFor(child.batch), onWhite = anyIncluded) }
                }
            }
        }
    }
}

@Composable
private fun ChildBatchRow(child: PlacementChild, included: Boolean, onWhite: Boolean) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (onWhite) White else Color.Transparent) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsBadge(child.initials, size = 36.dp, background = BadgeNeutralBackground, content = TextPrimary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.training_child_row, child.firstName, child.course, child.batch),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(if (included) R.drawable.ic_check else R.drawable.ic_remove_circle_outline),
                        contentDescription = null,
                        tint = if (included) BrandGreen else TextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(if (included) R.string.training_batch_included else R.string.training_batch_not_included, child.batch),
                        color = if (included) BrandGreen else TextSecondary,
                        fontSize = AppTextSize.Caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** White card with a bold title ("Schedule"). */
@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Upcoming sessions listed under the schedule. */
private const val MAX_NEXT_SESSIONS = 3

/** Very light green behind "Applicable batches" when a child's batch is listed. */
private val IncludedCardBackground = OnboardingGreenDisc.copy(alpha = 0.8f)

@Preview(showBackground = true, widthDp = 390, heightDp = 1500)
@Composable
private fun PlacementTrainingDetailPreview() {
    GRADit_RewampTheme {
        Column(Modifier.background(ScreenBackground)) {
            PlacementTrainingDetailContent(
                PlacementTrainingRepository.trainings.value.first { it.id == "aptitude-bootcamp" },
                PlacementRepository.familyFor(Role.PARENT),
            )
        }
    }
}
