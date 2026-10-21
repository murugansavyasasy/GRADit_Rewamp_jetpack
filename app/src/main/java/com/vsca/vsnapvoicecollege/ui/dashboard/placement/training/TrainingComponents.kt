package com.vsca.vsnapvoicecollege.ui.dashboard.placement.training

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.InitialsBadge
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.PlacementTypeBackground
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.daysFromToday
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.launchIntent
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** "Placement training", mode and status pills in one wrapping row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrainingPills(training: PlacementTraining, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LabelPill(stringResource(R.string.training_type), AmberChipBackground, OnboardingAmberCard)
        LabelPill(stringResource(training.mode.label), training.mode.background, training.mode.content, iconRes = training.mode.icon)
        TrainingStatusPill(training)
    }
}

/**
 * "Ended", "Session in progress", "Session today · 02:00 PM", "Session tomorrow · …",
 * "Starts in 5 days" before the first session, or "Next session Fri 09 Oct".
 */
@Composable
private fun TrainingStatusPill(training: PlacementTraining) {
    val now = System.currentTimeMillis()
    val next = training.nextSession(now)
    when {
        next == null -> LabelPill(stringResource(R.string.placement_ended), BadgeNeutralBackground, TextSecondary)
        next <= now -> LabelPill(stringResource(R.string.training_status_live), OnboardingGreenDisc, BrandGreen)
        daysFromToday(next, now) == 0 -> LabelPill(stringResource(R.string.training_status_today, clockLabel(next)), BrandBlue, White)
        daysFromToday(next, now) == 1 ->
            LabelPill(stringResource(R.string.training_status_tomorrow, clockLabel(next)), PlacementTypeBackground, BrandBlue)
        !training.hasStarted(now) -> {
            val days = daysFromToday(next, now)
            LabelPill(pluralStringResource(R.plurals.training_starts_in_days, days, days), PlacementTypeBackground, BrandBlue)
        }
        else -> LabelPill(stringResource(R.string.training_status_next, dayMonthLabel(next)), PlacementTypeBackground, BrandBlue)
    }
}

/** "10:00 AM – 01:00 PM" for one session. */
@Composable
internal fun sessionTimeLabel(training: PlacementTraining, sessionStart: Long = training.firstStart): String =
    stringResource(R.string.placement_time_range, clockLabel(sessionStart), clockLabel(training.endOf(sessionStart)))

/** "Every week on Monday and Wednesday", "Every day, Monday to Friday" or "Does not repeat". */
@Composable
internal fun repeatLabel(repeat: TrainingRepeat): String = when (repeat) {
    TrainingRepeat.Once -> stringResource(R.string.training_repeat_once)
    TrainingRepeat.Weekdays -> stringResource(R.string.training_repeat_weekdays)
    is TrainingRepeat.Weekly -> stringResource(R.string.training_repeat_weekly, weekdayList(repeat))
}

/** "Repeats every week on Monday and Wednesday until 16 Oct 2026"; null for a single session. */
@Composable
internal fun cardRepeatLabel(training: PlacementTraining): String? {
    val until = dayMonthYear(training.lastStart)
    return when (val repeat = training.repeat) {
        TrainingRepeat.Once -> null
        TrainingRepeat.Weekdays -> stringResource(R.string.training_card_repeat_weekdays, until)
        is TrainingRepeat.Weekly -> stringResource(R.string.training_card_repeat_weekly, weekdayList(repeat), until)
    }
}

/** "Monday", "Monday and Wednesday", "Monday, Wednesday and Friday" in the app language. */
@Composable
private fun weekdayList(repeat: TrainingRepeat.Weekly): String {
    val names = repeat.sortedDays.map { weekdayName(it, "EEEE") }
    return when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> stringResource(R.string.training_list_two, names.dropLast(1).joinToString(", "), names.last())
    }
}

/** "Mon & Wed", "Mon – Fri" — the compact days on the student's list. */
@Composable
internal fun shortDaysLabel(repeat: TrainingRepeat): String? = when (repeat) {
    TrainingRepeat.Once -> null
    TrainingRepeat.Weekdays -> stringResource(R.string.training_weekdays_short)
    is TrainingRepeat.Weekly -> repeat.sortedDays.joinToString(" & ") { weekdayName(it, "EEE") }
}

private fun weekdayName(dayOfWeek: Int, pattern: String): String {
    val date = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek) }.time
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

/** "Wed 07 Oct". */
internal fun dayMonthLabel(millis: Long): String = SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date(millis))

/** "16 Oct 2026". */
internal fun dayMonthYear(millis: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

/** "Fri 16 Oct 2026". */
internal fun weekdayDayMonthYear(millis: Long): String = SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault()).format(Date(millis))

/** Initials, "Trainer", name and designation on a light grey panel. */
@Composable
internal fun TrainerPanel(trainer: TrainingTrainer, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = ScreenBackground) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsBadge(trainer.initials, size = 44.dp, background = PlacementTypeBackground, content = BrandBlue)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(stringResource(R.string.training_trainer), color = TextSecondary, fontSize = AppTextSize.Caption, fontWeight = FontWeight.SemiBold)
                Text(trainer.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                Text(trainer.designation, color = TextSecondary, fontSize = AppTextSize.Label)
            }
        }
    }
}

/** Batch-year chips; the family's own batches are green, the rest grey. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BatchChips(batches: List<Int>, highlighted: Set<Int>, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        batches.sorted().forEach { batch ->
            val ours = batch in highlighted
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (ours) OnboardingGreenDisc else BadgeNeutralBackground,
                border = BorderStroke(1.dp, if (ours) BrandGreen else FieldBorder),
            ) {
                Text(
                    batch.toString(),
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = if (ours) BrandGreen else TextPrimary,
                    fontSize = AppTextSize.Label,
                    fontWeight = if (ours) FontWeight.Bold else FontWeight.SemiBold,
                )
            }
        }
    }
}

// ---- Actions ----

internal fun openTrainingInMaps(context: Context, training: PlacementTraining) =
    launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(training.venue))))

internal fun openTrainingJoinLink(context: Context, training: PlacementTraining) {
    val url = training.joinUrl ?: return
    if (!training.isJoinOpen()) {
        Toast.makeText(context, R.string.training_join_not_open, Toast.LENGTH_SHORT).show()
        return
    }
    launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

/** Adds the next session, repeating like the training does until its last session. */
internal fun addTrainingToCalendar(context: Context, training: PlacementTraining) {
    val start = training.nextSession() ?: training.firstStart
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, training.title)
        .putExtra(CalendarContract.Events.DESCRIPTION, training.description)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, training.joinUrl ?: training.venue)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, training.endOf(start))
    recurrenceRule(training)?.let { intent.putExtra(CalendarContract.Events.RRULE, it) }
    launchIntent(context, intent)
}

/** RFC 5545 rule, e.g. "FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20261016T235959Z". */
private fun recurrenceRule(training: PlacementTraining): String? {
    val days = when (val repeat = training.repeat) {
        TrainingRepeat.Once -> return null
        TrainingRepeat.Weekdays -> listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        is TrainingRepeat.Weekly -> repeat.sortedDays
    }
    val until = SimpleDateFormat("yyyyMMdd'T'235959'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(training.endOf(training.lastStart)))
    return "FREQ=WEEKLY;BYDAY=${days.joinToString(",") { RRULE_DAYS.getValue(it) }};UNTIL=$until"
}

private val RRULE_DAYS = mapOf(
    Calendar.SUNDAY to "SU", Calendar.MONDAY to "MO", Calendar.TUESDAY to "TU", Calendar.WEDNESDAY to "WE",
    Calendar.THURSDAY to "TH", Calendar.FRIDAY to "FR", Calendar.SATURDAY to "SA",
)

/** In-app link to a training. TODO: switch to the college's https deep-link domain. */
private fun trainingLink(training: PlacementTraining) = "gradit://placement/training/${training.id}"

internal fun shareTraining(context: Context, training: PlacementTraining, scheduleLabel: String, timeLabel: String) {
    val text = context.getString(
        R.string.training_share_text,
        training.title,
        training.description,
        scheduleLabel,
        timeLabel,
        training.venue,
        trainingLink(training),
    )
    launchIntent(
        context,
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, training.title).putExtra(Intent.EXTRA_TEXT, text),
            null,
        ),
    )
}
