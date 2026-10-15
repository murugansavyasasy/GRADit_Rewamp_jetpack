package com.vsca.vsnapvoicecollege.ui.dashboard.files

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.NavySurface
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dark navy used for video tiles and the offline / snackbar banners. */
internal val FilesDark = NavySurface

/** Square tile with the format's icon: PDF / DOCX label tiles, a photo tile, or a dark video tile. */
@Composable
internal fun FileTile(
    format: FileFormat,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val tint = format.tint
    val background = if (format.kind == FileKind.VIDEO) FilesDark else tint.background
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        when (format.kind) {
            FileKind.VIDEO -> Box(
                modifier = Modifier
                    .size(size * 0.5f)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    tint = FilesDark,
                    modifier = Modifier.size(size * 0.28f),
                )
            }
            FileKind.IMAGE -> Icon(
                painter = painterResource(R.drawable.ic_image_outline),
                contentDescription = null,
                tint = tint.content,
                modifier = Modifier.size(size * 0.5f),
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_file),
                    contentDescription = null,
                    tint = tint.content,
                    modifier = Modifier.size(size * 0.38f),
                )
                Text(
                    text = format.label,
                    color = tint.content,
                    fontSize = (size.value * 0.2f).sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** "PDF", "Image · JPG", "Video · MP4", "DOCX" chip. */
@Composable
internal fun TypeChip(format: FileFormat, modifier: Modifier = Modifier) {
    val tint = format.tint
    val label = when (format.kind) {
        FileKind.IMAGE -> stringResource(R.string.files_type_image, format.label)
        FileKind.VIDEO -> stringResource(R.string.files_type_video, format.label)
        else -> format.label
    }
    val icon = when (format.kind) {
        FileKind.IMAGE -> R.drawable.ic_image_outline
        FileKind.VIDEO -> R.drawable.ic_videocam_outline
        else -> R.drawable.ic_file
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = tint.background) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = tint.content, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(text = label, color = tint.content, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
        }
    }
}

/** All / Images / PDFs / Documents / Videos tabs with count bubbles and an underline. */
@Composable
internal fun FileTypeTabs(
    selected: FileTypeTab,
    counts: Map<FileTypeTab, Int>,
    onSelect: (FileTypeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            FileTypeTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(shape = RoundedCornerShape(50), color = if (isSelected) BrandBlue else BadgeNeutralBackground) {
                        Text(
                            text = (counts[tab] ?: 0).toString(),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 1.dp),
                            color = if (isSelected) White else TextPrimary,
                            fontSize = AppTextSize.Caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(tab.label),
                        color = if (isSelected) BrandBlue else TextSecondary,
                        fontSize = AppTextSize.Body,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (isSelected) BrandBlue else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = FieldBorder)
    }
}

/** Eye icon, "74 / 117 viewed" and a green progress bar. */
@Composable
internal fun ViewedProgress(viewed: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_visibility_outline),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.files_viewed, viewed, total),
                color = TextPrimary,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) (viewed.toFloat() / total).coerceIn(0f, 1f) else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = BrandGreen,
            trackColor = FieldBorder,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** Horizontally scrolling row of filter chips. */
@Composable
internal fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** Fills the space behind preview content; used by detail screens. */
@Composable
internal fun PreviewFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}

// ---- Labels ----

/** "Sem 5 · Division A · Students, Parents". */
@Composable
internal fun audienceLine(file: SharedFile): String =
    "${file.audience.label} · ${rolesLabel(file.roles)}"

@Composable
internal fun rolesLabel(roles: Set<AudienceRole>): String =
    AudienceRole.entries.filter { it in roles }.map { stringResource(it.label) }.joinToString(", ")

/** "Today, 09:40 AM" for today, otherwise "06 Oct 2026". */
@Composable
internal fun shortDateLabel(millis: Long): String =
    if (isDaysAgo(millis, 0)) {
        stringResource(R.string.files_today_at, clockLabel(millis))
    } else {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    }

/** "Fri 09 Oct, 08:00 AM". */
@Composable
internal fun scheduleLabel(millis: Long): String =
    "${SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date(millis))}, ${clockLabel(millis)}"

/** "Sat 31 Oct 2026". */
internal fun longDateLabel(millis: Long): String =
    SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault()).format(Date(millis))

/** "31 Oct". */
internal fun dayMonthLabel(millis: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))

/** "04:32" / "1:02:10". */
internal fun durationLabel(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, (total % 3600) / 60, total % 60)
    } else {
        "%02d:%02d".format(total / 60, total % 60)
    }
}
