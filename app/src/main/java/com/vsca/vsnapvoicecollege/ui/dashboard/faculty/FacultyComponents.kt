package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberRing
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.DateFormatSymbols
import java.util.Calendar

/** Dark green for "Present" and "Class teacher". */
internal val PresentGreen = Color(0xFF17733F)

/** Light green panels (present tile, class-teacher card). */
internal val PresentBackground = OnboardingGreenDisc

/** Avatar colours, picked by initials so a teacher keeps theirs everywhere. */
private val avatarPalette = listOf(
    BadgeNeutralBackground to TextPrimary,
    SelectedRowBackground to BrandBlue,
    OnboardingGreenDisc to PresentGreen,
)

@Composable
internal fun InitialsAvatar(initials: String, size: Dp = 56.dp, background: Color? = null, content: Color? = null) {
    val (bg, fg) = avatarPalette[(initials.hashCode() and 0x7fffffff) % avatarPalette.size]
    Box(
        Modifier.size(size).clip(CircleShape).background(background ?: bg).border(3.dp, White, CircleShape).border(1.dp, FieldBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = content ?: fg, fontSize = if (size >= 64.dp) AppTextSize.H2 else AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun HodBadge(modifier: Modifier = Modifier) =
    LabelPill(stringResource(R.string.faculty_badge_hod), SelectedRowBackground, BrandBlue, modifier, iconRes = R.drawable.ic_crown)

@Composable
internal fun ClassTeacherBadge(modifier: Modifier = Modifier) =
    LabelPill(stringResource(R.string.faculty_badge_class_teacher), PresentBackground, PresentGreen, modifier, iconRes = R.drawable.ic_star)

/** "Present · 08:56 AM", "Absent", "On leave", "Not in yet". */
@Composable
internal fun PresencePill(presence: Presence, modifier: Modifier = Modifier) {
    when (presence.status) {
        PresenceStatus.PRESENT -> LabelPill(
            presence.punchMinute?.let { stringResource(R.string.faculty_present_at, timeOfDay(it)) } ?: stringResource(R.string.faculty_present),
            PresentBackground, PresentGreen, modifier, iconRes = R.drawable.ic_check,
        )
        PresenceStatus.ABSENT -> LabelPill(stringResource(R.string.faculty_absent), AlertBackground, BadgeRed, modifier, iconRes = R.drawable.ic_person_off)
        PresenceStatus.ON_LEAVE -> LabelPill(stringResource(R.string.faculty_on_leave), AmberChipBackground, OnboardingAmberCard, modifier, iconRes = R.drawable.ic_calendar)
        PresenceStatus.NOT_IN -> LabelPill(stringResource(R.string.faculty_not_in), BadgeNeutralBackground, TextSecondary, modifier, iconRes = R.drawable.ic_clock)
    }
}

/** Big number over a label; [accent] tiles are tinted and may open something. */
@Composable
internal fun StatTile(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    background: Color = White,
    valueColor: Color = TextPrimary,
    labelColor: Color = TextPrimary,
    border: Color = FieldBorder,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Box(Modifier.heightIn(min = 84.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Column {
                Text(value.toString(), color = valueColor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(label, color = labelColor, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
            }
            if (onClick != null) {
                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = labelColor, modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
            }
        }
    }
    val shape = RoundedCornerShape(18.dp)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = background, border = BorderStroke(1.dp, border), content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = background, border = BorderStroke(1.dp, border), content = content)
    }
}

/** The four directory / summary tiles' colour sets. */
internal object Tiles {
    val presentBg = PresentBackground
    val awayBg = AlertBackground
    val amberBg = AmberChipBackground
    val amberBorder = OnboardingAmberRing
}

/** Bold title + optional subtitle + round close button, at the top of every sheet. */
@Composable
internal fun SheetHeader(title: String, subtitle: String?, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
            subtitle?.let { Text(it, color = TextSecondary, fontSize = AppTextSize.Body) }
        }
        Surface(onClick = onClose, shape = CircleShape, color = White, border = BorderStroke(1.dp, FieldBorder)) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.faculty_close), tint = TextPrimary)
            }
        }
    }
}

/** Radio card: title + subtitle, outlined blue with "Selected" when chosen. */
@Composable
internal fun RadioCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) SelectedRowBackground else White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BrandBlue else FieldBorder),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioDot(selected)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (enabled) TextPrimary else TextSecondary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                subtitle?.let { Text(it, color = TextSecondary, fontSize = AppTextSize.Body) }
            }
            when {
                trailing != null -> trailing()
                selected -> Text(stringResource(R.string.faculty_selected), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        Modifier.size(26.dp).clip(CircleShape).border(if (selected) 7.dp else 2.dp, if (selected) BrandBlue else Color(0xFFB4BACB), CircleShape),
    )
}

@Composable
internal fun KindTag(kind: SubjectKind, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (kind) {
        SubjectKind.THEORY -> Triple(R.string.faculty_kind_theory, SelectedRowBackground, BrandBlue)
        SubjectKind.LAB -> Triple(R.string.faculty_kind_lab, PresentBackground, PresentGreen)
        SubjectKind.ACTIVITY -> Triple(R.string.faculty_kind_activity, BadgeNeutralBackground, TextPrimary)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = bg) {
        Text(stringResource(label), Modifier.padding(horizontal = 9.dp, vertical = 2.dp), color = fg, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
    }
}

/** Round outlined chat button. */
@Composable
internal fun MessageButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = BrandBlue) {
    Surface(onClick = onClick, modifier = modifier.size(52.dp), shape = CircleShape, color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_chat_bubble_outline), contentDescription, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

/** Child card for the parent switcher. */
@Composable
internal fun ChildCard(child: Child, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = if (selected) SelectedRowBackground else White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BrandBlue else FieldBorder),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (selected) BrandBlue else BadgeNeutralBackground), contentAlignment = Alignment.Center) {
                Text(child.initials, color = if (selected) White else TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(child.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    stringResource(R.string.faculty_child_class, child.programme, child.currentSemester, child.section),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A white rounded card with a hairline border. */
@Composable
internal fun FacultyCard(modifier: Modifier = Modifier, border: Color = FieldBorder, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = White, border = BorderStroke(1.dp, border)) { content() }
}

/** Label / value row in a profile card. */
@Composable
internal fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text(label, Modifier.weight(0.42f), color = TextSecondary, fontSize = AppTextSize.BodyLarge)
        Text(value, Modifier.weight(0.58f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun IconText(@DrawableRes icon: Int, text: String, color: Color = TextSecondary, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(icon), null, tint = color, modifier = Modifier.size(22.dp))
        Text(text, color = color, fontSize = AppTextSize.BodyLarge)
    }
}

// ---- Formatting ----

/** "08:56 AM" in the device's 12/24-hour setting. */
@Composable
internal fun timeOfDay(minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minute / 60)
        set(Calendar.MINUTE, minute % 60)
    }
    return android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(cal.time)
}

/** "09:00 – 09:50 AM" style range. */
@Composable
internal fun timeRange(start: Int, end: Int): String = stringResource(R.string.faculty_time_range, timeOfDay(start), timeOfDay(end))

/** "Mon" for a [Calendar] day constant, in the app language. */
internal fun shortDay(day: Int): String = DateFormatSymbols.getInstance().shortWeekdays[day]

/** "Jul – Nov 2025" or "Dec 2024 – Apr 2025". */
internal fun monthRangeLabel(range: MonthRange): String {
    val months = DateFormatSymbols.getInstance().shortMonths
    val start = months[range.start % 12]
    val end = months[range.end % 12]
    return if (range.startYear == range.endYear) "$start – $end ${range.endYear}" else "$start ${range.startYear} – $end ${range.endYear}"
}

/** "P3 and P5" / "P1, P4 and P6". */
@Composable
internal fun periodList(periods: List<Int>): String {
    val labels = periods.map { stringResource(R.string.faculty_period_short, it) }
    return when (labels.size) {
        0 -> ""
        1 -> labels[0]
        else -> stringResource(R.string.faculty_and, labels.dropLast(1).joinToString(", "), labels.last())
    }
}

/** "Mon P1 · Tue P3" / "Mon P5, P6 · Thu P5, P6". */
@Composable
internal fun slotsLabel(slots: List<Slot>): String {
    val labels = PeriodSlot.entries.associate { it.number to stringResource(R.string.faculty_period_short, it.number) }
    return groupSlotsByDay(slots).joinToString(" · ") { (day, periods) ->
        shortDay(day) + " " + periods.joinToString(", ") { labels[it].orEmpty() }
    }
}
