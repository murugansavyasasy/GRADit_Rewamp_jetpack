package com.vsca.vsnapvoicecollege.ui.dashboard.placement

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Amber outline on events coming up in the next couple of days. */
internal val SoonBorder = Color(0xFFE6C98A)
internal val SoonBannerBackground = Color(0xFFFDF3DC)

/** Type, mode and timing pills in one wrapping row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventPills(event: PlacementEvent, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LabelPill(stringResource(event.type.label), PlacementTypeBackground, BrandBlue)
        LabelPill(stringResource(event.mode.label), event.mode.background, event.mode.content, iconRes = event.mode.icon)
        TimingPill(event)
    }
}

/** "Ended", "Happening now", "Today" or "Tomorrow"; nothing for events further out. */
@Composable
private fun TimingPill(event: PlacementEvent) {
    val now = System.currentTimeMillis()
    when {
        event.hasEnded(now) -> LabelPill(stringResource(R.string.placement_ended), BadgeNeutralBackground, TextSecondary)
        event.startAt <= now -> LabelPill(stringResource(R.string.placement_live_now), OnboardingGreenDisc, BrandGreen)
        daysFromToday(event.startAt, now) == 0 -> LabelPill(stringResource(R.string.placement_today), AmberChipBackground, OnboardingAmberCard)
        daysFromToday(event.startAt, now) == 1 -> LabelPill(stringResource(R.string.placement_tomorrow), AmberChipBackground, OnboardingAmberCard)
    }
}

/** "Today", "Tomorrow" or "In 11 days" — the suffix after the date on the detail screen. */
@Composable
internal fun relativeDayLabel(millis: Long): String? {
    val days = daysFromToday(millis)
    return when {
        days < 0 -> null
        days == 0 -> stringResource(R.string.placement_today)
        days == 1 -> stringResource(R.string.placement_tomorrow)
        else -> pluralStringResource(R.plurals.placement_in_days, days, days)
    }
}

/** "10:00 AM – 11:00 AM", or just the start when there's no end time. */
@Composable
internal fun timeRangeLabel(event: PlacementEvent): String {
    val start = clockLabel(event.startAt)
    return if (event.endAt != null) stringResource(R.string.placement_time_range, start, clockLabel(event.endAt)) else start
}

/** "Thu, 08 Oct 2026". */
internal fun fullDate(millis: Long): String = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(millis))

/** "14 Oct". */
internal fun shortDate(millis: Long): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))

/** "Wed 07 Oct, 09:05 AM" — when the offline list was last refreshed. */
@Composable
internal fun syncedLabel(millis: Long): String =
    "${SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date(millis))}, ${clockLabel(millis)}"

/** Month / day / weekday tile on the left of a list card. */
@Composable
internal fun DateTile(millis: Long, style: DateTileStyle) {
    val date = Date(millis)
    val (background, content) = when (style) {
        DateTileStyle.SOON -> BrandBlue to White
        DateTileStyle.UPCOMING -> PlacementTypeBackground to BrandBlue
        DateTileStyle.ENDED -> BadgeNeutralBackground to TextSecondary
    }
    Surface(shape = RoundedCornerShape(14.dp), color = background) {
        Column(Modifier.width(64.dp).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val title = if (style == DateTileStyle.ENDED) TextPrimary else content
            Text(SimpleDateFormat("MMM", Locale.getDefault()).format(date), color = title, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
            Text(SimpleDateFormat("dd", Locale.getDefault()).format(date), color = title, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
            Text(SimpleDateFormat("EEE", Locale.getDefault()).format(date), color = title, fontSize = AppTextSize.Caption, fontWeight = FontWeight.Medium)
        }
    }
}

internal enum class DateTileStyle { SOON, UPCOMING, ENDED }

/** Icon + text line under an event title (time, venue). */
@Composable
internal fun EventInfoLine(iconRes: Int, text: String, modifier: Modifier = Modifier, bold: String? = null) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        if (bold != null) {
            Text(bold, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
            if (text.isNotEmpty()) Text(" · $text", color = TextSecondary, fontSize = AppTextSize.Body)
        } else {
            Text(text, color = TextPrimary, fontSize = AppTextSize.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Round initials badge for a company ("FW") or a child ("AR"). */
@Composable
internal fun InitialsBadge(
    initials: String,
    size: Dp = 28.dp,
    background: Color = White,
    content: Color = BrandBlue,
) {
    Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text(initials, color = content, fontSize = if (size > 32.dp) AppTextSize.Caption else AppTextSize.CaptionSmall, fontWeight = FontWeight.ExtraBold)
    }
}

/** "FW Freshworks" pill on the list card. */
@Composable
internal fun CompanyChip(company: PlacementCompany) {
    Surface(shape = RoundedCornerShape(50), color = BadgeNeutralBackground, border = BorderStroke(1.dp, FieldBorder)) {
        Row(Modifier.padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsBadge(company.initials)
            Spacer(Modifier.width(8.dp))
            Text(company.name, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Company row on the detail screen: square initials tile + name. */
@Composable
internal fun CompanyRow(company: PlacementCompany) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(14.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Text(company.initials, color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(company.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** Green outlined course chip ("B.E. CSE"). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CourseChips(courses: List<String>, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        courses.forEach { course ->
            Surface(shape = RoundedCornerShape(8.dp), color = OnboardingGreenDisc, border = BorderStroke(1.dp, BrandGreen)) {
                Text(course, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = BrandGreen, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Small grey caption above a block on a card ("Company", "Eligible courses"). */
@Composable
internal fun FieldCaption(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
}

/** Upcoming / Historical with count bubbles above the label and an underline. */
@Composable
internal fun PlacementTabs(selected: PlacementTab, counts: Map<PlacementTab, Int>, onSelect: (PlacementTab) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            PlacementTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier.weight(1f).clickable { onSelect(tab) }.padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(shape = RoundedCornerShape(50), color = if (isSelected) BrandBlue else BadgeNeutralBackground) {
                        Text(
                            (counts[tab] ?: 0).toString(),
                            Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                            color = if (isSelected) White else TextPrimary,
                            fontSize = AppTextSize.Caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(tab.label),
                        color = if (isSelected) BrandBlue else TextPrimary,
                        fontSize = AppTextSize.BodyLarge,
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

/** "Events are posted by the placement cell…" note and the Powered-by row. */
@Composable
internal fun PlacementFooter(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_info_outline), contentDescription = null, tint = TextSecondary, modifier = Modifier.padding(top = 3.dp).size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.placement_footer_note), color = TextSecondary, fontSize = AppTextSize.Label, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        PoweredByRow(textColor = TextSecondary)
    }
}

// ---- Actions ----

private fun launch(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.placement_no_app, Toast.LENGTH_SHORT).show()
    }
}

internal fun openInMaps(context: Context, event: PlacementEvent) =
    launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(event.venue))))

internal fun openJoinLink(context: Context, event: PlacementEvent) {
    val url = event.joinUrl ?: return
    if (!event.isJoinOpen()) {
        Toast.makeText(context, R.string.placement_join_not_open, Toast.LENGTH_SHORT).show()
        return
    }
    launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

internal fun addToCalendar(context: Context, event: PlacementEvent) {
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, event.title)
        .putExtra(CalendarContract.Events.DESCRIPTION, event.subtitle)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, event.joinUrl ?: event.venue)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startAt)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endAt ?: (event.startAt + DEFAULT_LENGTH_MS))
    launch(context, intent)
}

/** In-app link to an event. TODO: switch to the college's https deep-link domain. */
private fun eventLink(event: PlacementEvent) = "gradit://placement/${event.id}"

internal fun shareEvent(context: Context, event: PlacementEvent, timeLabel: String) {
    val text = context.getString(
        R.string.placement_share_text,
        event.title,
        event.subtitle,
        "${fullDate(event.startAt)} · $timeLabel",
        event.venue,
        eventLink(event),
    )
    launch(
        context,
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, event.title).putExtra(Intent.EXTRA_TEXT, text),
            null,
        ),
    )
}

/** Calendar entries for events without an end time last an hour. */
private const val DEFAULT_LENGTH_MS = 60 * 60 * 1000L
