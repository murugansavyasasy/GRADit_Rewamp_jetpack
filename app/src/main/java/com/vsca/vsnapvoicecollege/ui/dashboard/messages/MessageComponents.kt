package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.DashedRing
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Back button + title, with optional trailing actions (search, overflow menu). */
@Composable
internal fun MessagesTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = AppTextSize.H4,
            fontWeight = FontWeight.ExtraBold,
        )
        actions()
    }
}

@Composable
internal fun TopBarIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Circular initials avatar tinted by the sender's department. */
@Composable
internal fun SenderAvatar(
    initials: String,
    accent: MessageAccent,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, color = accent.content, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

/** "Exam Cell" / "Principal" pill next to a sender's name. */
@Composable
internal fun DepartmentBadge(department: SenderDepartment, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = department.accent.background) {
        Text(
            text = stringResource(department.label),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
            color = department.accent.content,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Sender name (ellipsized) followed by their department badge. */
@Composable
internal fun SenderLine(sender: MessageSender, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = sender.name,
            modifier = Modifier.weight(1f, fill = false),
            color = TextPrimary,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        DepartmentBadge(sender.department)
    }
}

/** Filter chip with a count bubble, e.g. "Text 259". */
@Composable
internal fun CountChip(
    text: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) BrandBlue else White,
        border = if (selected) null else BorderStroke(1.dp, FieldBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = if (selected) White else TextPrimary,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) White.copy(alpha = 0.22f) else SelectedRowBackground,
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                    color = if (selected) White else BrandBlue,
                    fontSize = AppTextSize.Caption,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Light-blue "Messages from your college. You cannot reply to these." banner. */
@Composable
internal fun InfoBanner(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = SelectedRowBackground) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(R.drawable.ic_info_outline),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text = text, color = BrandBlue, fontSize = AppTextSize.Label, lineHeight = 20.sp)
        }
    }
}

/** "TODAY" / "YESTERDAY" list section header. */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier = modifier,
        color = TextSecondary,
        fontSize = AppTextSize.Caption,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
    )
}

/** Blue dot + "New" pill marking an unread message. */
@Composable
internal fun NewMarker(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(BrandBlue),
        )
        Spacer(Modifier.width(6.dp))
        Surface(shape = RoundedCornerShape(6.dp), color = BrandBlue) {
            Text(
                text = stringResource(R.string.msg_new),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                color = White,
                fontSize = AppTextSize.CaptionSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

enum class VoicePlayerStyle {
    /** "0:12" — inbox cards (shows elapsed time while playing). */
    COMPACT,

    /** "00:00 / 00:12" — history cards and the message screen. */
    FULL,
}

/** Play / pause button with a progress track and time, backed by [controller]. */
@Composable
internal fun VoicePlayerBar(
    key: String,
    clip: VoiceClip,
    controller: VoicePlaybackController,
    modifier: Modifier = Modifier,
    style: VoicePlayerStyle = VoicePlayerStyle.COMPACT,
) {
    val playing = controller.isPlaying(key)
    val position = if (playing) controller.positionMs else 0L
    val progress = if (clip.durationMs > 0) (position.toFloat() / clip.durationMs).coerceIn(0f, 1f) else 0f
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(50), color = SelectedRowBackground) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = { controller.toggle(key, clip) },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = BrandBlue,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = stringResource(if (playing) R.string.msg_voice_pause else R.string.msg_voice_play),
                        tint = White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                color = BrandBlue,
                trackColor = DashedRing.copy(alpha = 0.6f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = when (style) {
                    VoicePlayerStyle.COMPACT -> formatClipTime(if (playing) position else clip.durationMs)
                    VoicePlayerStyle.FULL -> "${formatClipTimePadded(position)} / ${formatClipTimePadded(clip.durationMs)}"
                },
                color = BrandBlue,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Section a message belongs in, by the day it was sent. */
internal fun sectionFor(millis: Long): InboxSection = when {
    isDaysAgo(millis, 0) -> InboxSection.TODAY
    isDaysAgo(millis, 1) -> InboxSection.YESTERDAY
    else -> InboxSection.EARLIER
}

/** Time for today / yesterday ("3:13 PM"), otherwise the date ("3 Oct"). */
@Composable
internal fun listTimeLabel(millis: Long): String =
    if (sectionFor(millis) == InboxSection.EARLIER) {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    } else {
        clockLabel(millis)
    }

/** "6 Oct 2026, 3:13 PM". */
@Composable
internal fun fullDateLabel(millis: Long): String =
    "${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))}, ${clockLabel(millis)}"

/** "05/10/2026". */
internal fun numericDateLabel(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

// ---- Downloads ----
