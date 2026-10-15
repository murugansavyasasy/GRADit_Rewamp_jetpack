package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.components.AttachmentIcon
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.BULLET
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.cachedAttachmentCopy
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import com.vsca.vsnapvoicecollege.ui.components.openAttachment
import com.vsca.vsnapvoicecollege.ui.components.renderMarkup
import com.vsca.vsnapvoicecollege.ui.components.saveToDownloads
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---- Pills ----

@Composable
internal fun CategoryPill(category: NoticeCategory) {
    LabelPill(stringResource(category.label), category.background, category.content)
}

/** Solid red "URGENT" pill for urgent-priority notices. */
@Composable
internal fun UrgentPill() {
    LabelPill(stringResource(R.string.notice_urgent_badge), BadgeRed, White, iconRes = R.drawable.ic_error)
}

@Composable
internal fun PinnedPill() {
    LabelPill(stringResource(R.string.notice_pinned), SelectedRowBackground, BrandBlue, iconRes = R.drawable.ic_place_outline)
}

@Composable
internal fun StatusPill(status: NoticeStatus, withIcon: Boolean = true) {
    LabelPill(
        text = stringResource(status.label),
        background = status.background,
        content = status.content,
        iconRes = if (withIcon && status == NoticeStatus.PUBLISHED) R.drawable.ic_check else null,
    )
}

/** Blue dot + "New" for unread notices. */
@Composable
internal fun NewDot() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(BrandBlue))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.notice_new), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
    }
}

/** Category, urgent and pinned pills in one wrapping row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NoticePills(notice: Notice, showPinned: Boolean, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryPill(notice.category)
        if (notice.priority == NoticePriority.URGENT) UrgentPill()
        if (showPinned) PinnedPill()
        trailing()
    }
}

// ---- Author ----

@Composable
internal fun AuthorAvatar(author: NoticeAuthor, size: Dp = 52.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(OnboardingGreenDisc), contentAlignment = Alignment.Center) {
        Text(author.initials, color = BrandGreen, fontSize = (size.value * 0.32f).sp, fontWeight = FontWeight.Bold)
    }
}

/** Avatar, "Dr. Suresh Kumar · Principal" and the time underneath. */
@Composable
internal fun AuthorRow(author: NoticeAuthor, time: String, modifier: Modifier = Modifier, avatarSize: Dp = 52.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AuthorAvatar(author, avatarSize)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(R.string.notice_author, author.name, author.title),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(time, color = TextSecondary, fontSize = AppTextSize.Label)
        }
    }
}

// ---- Body ----

/** Renders notice markup: paragraphs, **bold** and indented "• " bullet lines. */
@Composable
internal fun NoticeBody(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val lines = text.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        var index = 0
        while (index < lines.size) {
            if (lines[index].startsWith(BULLET)) {
                val bullets = mutableListOf<String>()
                while (index < lines.size && lines[index].startsWith(BULLET)) bullets += lines[index++].removePrefix(BULLET)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bullets.forEach { item ->
                        Row {
                            Text("•", modifier = Modifier.width(26.dp).padding(start = 8.dp), color = TextPrimary, fontSize = AppTextSize.BodyLarge)
                            Text(renderMarkup(item), color = TextPrimary, fontSize = AppTextSize.BodyLarge, lineHeight = 25.sp)
                        }
                    }
                }
            } else {
                Text(renderMarkup(lines[index++]), color = TextPrimary, fontSize = AppTextSize.BodyLarge, lineHeight = 25.sp)
            }
        }
    }
}

/** First line of the body without markup, for list previews. */
internal fun previewText(body: String): String =
    body.lines().joinToString(" ") { it.removePrefix(BULLET).replace("**", "").trim() }.trim()

// ---- Read progress ----

@Composable
internal fun ReadProgress(read: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        Row {
            Text(
                stringResource(R.string.notice_read_count, read, total),
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.notice_percent, (fraction * 100).toInt()), color = TextSecondary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = BrandGreen,
            trackColor = FieldBorder,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

// ---- Attachments ----

/** Thumbnail (first PDF page or the photo) above the file name, with View and Download. */
@Composable
internal fun AttachmentCard(attachment: FileAttachment, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, attachment.uri) {
        value = withContext(Dispatchers.IO) { renderThumbnail(context, attachment) }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(BadgeNeutralBackground)
                    .clickable { openAttachment(context, attachment) },
                contentAlignment = Alignment.BottomCenter,
            ) {
                val bitmap = thumbnail
                if (bitmap != null && attachment.type == AttachmentType.IMAGE) {
                    Image(bitmap.asImageBitmap(), contentDescription = attachment.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Surface(
                        modifier = Modifier.padding(top = 16.dp, start = 48.dp, end = 48.dp).fillMaxSize(),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                        color = White,
                        shadowElevation = 1.dp,
                    ) {
                        if (bitmap != null) {
                            Image(bitmap.asImageBitmap(), contentDescription = attachment.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                        } else {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.fillMaxWidth(0.6f).height(10.dp).clip(RoundedCornerShape(50)).background(BrandBlue))
                                Box(Modifier.fillMaxWidth(0.4f).height(7.dp).clip(RoundedCornerShape(50)).background(FieldBorder))
                                repeat(3) { Box(Modifier.fillMaxWidth().height(12.dp).background(SelectedRowBackground)) }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = FieldBorder)
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val tileColor = if (attachment.type == AttachmentType.PDF) AlertBackground else SelectedRowBackground
                Box(Modifier.size(52.dp).background(tileColor, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    AttachmentIcon(attachment.type, size = 26.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(attachment.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(
                            R.string.notice_file_meta,
                            stringResource(if (attachment.type == AttachmentType.PDF) R.string.notice_file_pdf else R.string.notice_file_image),
                            fileSizeLabel(attachment.sizeBytes),
                        ),
                        color = TextSecondary,
                        fontSize = AppTextSize.Label,
                    )
                }
                Spacer(Modifier.width(8.dp))
                SquareTonalButton(R.drawable.ic_visibility_outline, stringResource(R.string.notice_view_file, attachment.name)) {
                    openAttachment(context, attachment)
                }
                Spacer(Modifier.width(10.dp))
                SquareTonalButton(R.drawable.ic_download, stringResource(R.string.notice_download_file, attachment.name)) {
                    if (NetworkMonitor.isOnline.value) {
                        saveToDownloads(context, attachment)
                    } else {
                        Toast.makeText(context, R.string.notice_offline_download, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun SquareTonalButton(iconRes: Int, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(52.dp), shape = RoundedCornerShape(14.dp), color = SelectedRowBackground) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(iconRes), description, tint = BrandBlue, modifier = Modifier.size(24.dp))
        }
    }
}

/** Renders the first page of a PDF, or a downsampled photo, for the attachment card. */
private fun renderThumbnail(context: Context, attachment: FileAttachment): Bitmap? = runCatching {
    val copy = cachedAttachmentCopy(context, attachment) ?: return null
    if (attachment.type == AttachmentType.IMAGE) {
        BitmapFactory.decodeFile(copy.path, BitmapFactory.Options().apply { inSampleSize = 4 })
    } else {
        ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.openPage(0).use { page ->
                    val width = 900
                    val height = (width.toFloat() * page.height / page.width).toInt()
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                        it.eraseColor(android.graphics.Color.WHITE)
                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }
}.getOrNull()

// ---- Expiry date sheet ----

/**
 * Month calendar in a bottom sheet. Days before [minDate] are disabled; today is circled
 * and the chosen day filled. Reports the end of the chosen day, or null for "No expiry".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpiryDateSheet(
    initial: Long?,
    minDate: Long,
    onDismiss: () -> Unit,
    onDone: (Long?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by rememberSaveable { mutableLongStateOf(startOfDay(initial ?: (minDate + 7 * DAY_MS))) }
    var month by rememberSaveable { mutableLongStateOf(startOfMonth(selected)) }
    val minDay = startOfDay(minDate)
    val today = startOfDay(System.currentTimeMillis())

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = White) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.notice_expiry_title), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
                if (initial != null) {
                    TextButton(onClick = { onDone(null) }) {
                        Text(stringResource(R.string.notice_no_expiry), color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(onClick = onDismiss, shape = CircleShape, color = White, border = BorderStroke(1.dp, FieldBorder)) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.notice_close), tint = TextPrimary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = addMonths(month, -1) }, enabled = month > startOfMonth(minDay)) {
                    Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.notice_prev_month), tint = TextPrimary)
                }
                Text(
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(month)),
                    Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { month = addMonths(month, 1) }) {
                    Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.notice_next_month), tint = TextPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            val weekdays = remember { weekdayInitials() }
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { day ->
                    Text(day, Modifier.weight(1f), color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))
            monthGrid(month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                            if (day != null) {
                                val enabled = day >= minDay
                                val isSelected = day == selected
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .then(if (isSelected) Modifier.background(BrandBlue) else Modifier)
                                        .then(if (day == today && !isSelected) Modifier.border(2.dp, BrandBlue, CircleShape) else Modifier)
                                        .clickable(enabled = enabled) { selected = day },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        Calendar.getInstance().apply { timeInMillis = day }.get(Calendar.DAY_OF_MONTH).toString(),
                                        color = when {
                                            isSelected -> White
                                            enabled -> TextPrimary
                                            else -> TextMuted
                                        },
                                        fontSize = AppTextSize.BodyLarge,
                                        fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onDone(endOfDay(selected)) },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Text(
                    stringResource(R.string.notice_expiry_done, SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(selected))),
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfDay(millis: Long): Long = startOfDay(millis) + DAY_MS - 60_000

private fun startOfMonth(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(millis)
    set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis

private fun addMonths(millis: Long, months: Int): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    add(Calendar.MONTH, months)
}.timeInMillis

/** Day starts for [monthStart]'s month, padded with nulls so the first day lands on its weekday. */
private fun monthGrid(monthStart: Long): List<Long?> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    val lead = (cal.get(Calendar.DAY_OF_WEEK) - cal.firstDayOfWeek + 7) % 7
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = MutableList<Long?>(lead) { null }
    repeat(days) { offset ->
        cells += Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(Calendar.DAY_OF_MONTH, offset)
        }.timeInMillis
    }
    while (cells.size % 7 != 0) cells += null
    return cells
}

private fun weekdayInitials(): List<String> {
    val cal = Calendar.getInstance()
    val format = SimpleDateFormat("EEEEE", Locale.getDefault())
    return (0 until 7).map { i ->
        cal.set(Calendar.DAY_OF_WEEK, (cal.firstDayOfWeek - 1 + i) % 7 + 1)
        format.format(cal.time)
    }
}

// ---- Dates ----

/** "Today, 02:30 PM" / "Yesterday, 04:10 PM" / "05 Oct 2026". */
@Composable
internal fun shortTimeLabel(millis: Long): String = when {
    isDaysAgo(millis, 0) -> stringResource(R.string.notice_today_at, clockLabel(millis))
    isDaysAgo(millis, 1) -> stringResource(R.string.notice_yesterday_at, clockLabel(millis))
    else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}

/** "Wed, 07 Oct 2026 · 09:00 AM". */
@Composable
internal fun longTimeLabel(millis: Long): String =
    "${SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(millis))} · ${clockLabel(millis)}"

/** "Mon, 26 Oct 2026". */
internal fun dayLabel(millis: Long): String = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))

/** "05 Oct 2026". */
internal fun plainDate(millis: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

// ---- Share / export ----

/** In-app link to a notice. TODO: switch to the college's https deep-link domain. */
internal fun noticeLink(notice: Notice) = "gradit://notices/${notice.id}"

internal fun shareNotice(context: Context, notice: Notice) {
    val text = context.getString(R.string.notice_share_text, notice.title, previewText(notice.body).take(200), noticeLink(notice))
    val intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, notice.title).putExtra(Intent.EXTRA_TEXT, text),
        null,
    )
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.notice_no_app, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Writes the notice and its read report to a CSV file and opens the share sheet for it.
 * TODO: include per-recipient read status from the notices API.
 */
internal fun exportReadReport(context: Context, notice: Notice) {
    fun cell(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
    val rows = listOf(
        listOf("Title", notice.title),
        listOf("Category", context.getString(notice.category.label)),
        listOf("Status", context.getString(notice.status().label)),
        listOf("Classes", classesLabel(notice.classes)),
        listOf("Roles", notice.roles.joinToString(", ") { context.getString(it.label) }),
        listOf("Published", notice.publishedAt?.let { dayLabel(it) }.orEmpty()),
        listOf("Expires", notice.expiresAt?.let { dayLabel(it) }.orEmpty()),
        listOf("Recipients", notice.recipientCount.toString()),
        listOf("Read", notice.readCount.toString()),
        listOf("Not read", (notice.recipientCount - notice.readCount).coerceAtLeast(0).toString()),
    )
    val csv = rows.joinToString("\n") { row -> row.joinToString(",") { cell(it) } } + "\n"
    val dir = File(context.cacheDir, "shared/notices").apply { mkdirs() }
    val safeName = notice.title.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').take(40).ifBlank { "notice" }
    val file = File(dir, "${safeName}_read_report.csv").apply { writeText(csv) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.notice_export_subject, notice.title))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        null,
    )
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.notice_no_app, Toast.LENGTH_SHORT).show()
    }
}
