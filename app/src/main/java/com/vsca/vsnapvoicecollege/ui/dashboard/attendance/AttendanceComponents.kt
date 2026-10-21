package com.vsca.vsnapvoicecollege.ui.dashboard.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportFormat
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportedFile
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyExport
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultySheet
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.RadioCard
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.SheetHeader
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.DestructiveRed
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberRing
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---- Colours ----

/** Green used for present / good figures. */
val AttGreen = Color(0xFF17733F)

/** Light grey-blue behind "Not punched in yet". */
val AttNeutralTile = Color(0xFFE9ECF3)

/** Below the 75% minimum. */
const val MINIMUM_PERCENT = 75

/** 75–84% is "watch"; 85% and above is "good". */
const val WATCH_BELOW_PERCENT = 85

enum class AttendanceLevel { GOOD, WATCH, LOW }

fun levelOf(percent: Double): AttendanceLevel = when {
    percent < MINIMUM_PERCENT -> AttendanceLevel.LOW
    percent < WATCH_BELOW_PERCENT -> AttendanceLevel.WATCH
    else -> AttendanceLevel.GOOD
}

/** Bar / text colour for a percentage. */
fun levelColor(level: AttendanceLevel): Color = when (level) {
    AttendanceLevel.GOOD -> AttGreen
    AttendanceLevel.WATCH -> OnboardingAmberCard
    AttendanceLevel.LOW -> BadgeRed
}

fun levelBackground(level: AttendanceLevel): Color = when (level) {
    AttendanceLevel.GOOD -> OnboardingGreenDisc
    AttendanceLevel.WATCH -> AmberChipBackground
    AttendanceLevel.LOW -> AlertBackground
}

@Composable
fun levelLabel(level: AttendanceLevel): String = stringResource(
    when (level) {
        AttendanceLevel.GOOD -> R.string.att_level_good
        AttendanceLevel.WATCH -> R.string.att_level_watch
        AttendanceLevel.LOW -> R.string.att_level_low
    },
)

/** "87.3%" with one decimal, or "94%" with none. */
@Composable
fun percentLabel(percent: Double, decimals: Int = 0): String =
    stringResource(R.string.att_percent, String.format(Locale.getDefault(), "%.${decimals}f", percent))

// ---- Tiles, tabs, chips ----

/** Big number over a label on a tinted tile; selected tiles get a blue outline. */
@Composable
fun AttStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    background: Color = White,
    valueColor: Color = TextPrimary,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val border = if (selected) BorderStroke(2.dp, BrandBlue) else BorderStroke(1.dp, if (background == White) FieldBorder else background)
    val content: @Composable () -> Unit = {
        Column(Modifier.heightIn(min = 80.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.Center) {
            Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(label, color = TextPrimary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
        }
    }
    val shape = RoundedCornerShape(18.dp)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = background, border = border, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = background, border = border, content = content)
    }
}

/** Underlined tabs with a count bubble above each label ("102 / Today"). */
@Composable
fun CountTabs(labels: List<String>, counts: List<Int?>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                Column(Modifier.weight(1f).clickable { onSelect(index) }.padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    counts.getOrNull(index)?.let { count ->
                        Surface(shape = RoundedCornerShape(50), color = if (isSelected) BrandBlue else BadgeNeutralBackground) {
                            Text(
                                count.toString(),
                                Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                                color = if (isSelected) White else TextPrimary,
                                fontSize = AppTextSize.Caption,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        label,
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

/** Blue pill with an ✕ that removes one filter. */
@Composable
fun RemovableFilterChip(text: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onRemove, modifier = modifier, shape = RoundedCornerShape(50), color = SelectedRowBackground, border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f))) {
        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.att_remove_filter), tint = BrandBlue, modifier = Modifier.size(16.dp))
        }
    }
}

/** Red underlined "Reset all". */
@Composable
fun ResetAllLink(onClick: () -> Unit, text: String = stringResource(R.string.att_reset_all)) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(6.dp),
        color = DestructiveRed,
        fontSize = AppTextSize.Body,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline,
    )
}

/** Outlined "⤓ Export". */
@Composable
fun ExportButton(onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, FieldBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = White),
    ) {
        Icon(painterResource(R.drawable.ic_download), null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.att_export), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
    }
}

// ---- Progress ----

/** Rounded bar filled to [fraction]; [markerAt] draws the dashed 75% line. */
@Composable
fun PercentBar(fraction: Float, color: Color, modifier: Modifier = Modifier, markerAt: Float? = null, height: Dp = 8.dp) {
    BoxWithConstraints(modifier.fillMaxWidth().height(height + 8.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(BadgeNeutralBackground))
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(50)).background(color))
        markerAt?.let { at ->
            Canvas(Modifier.offset(x = maxWidth * at).width(2.dp).fillMaxHeight()) {
                drawLine(
                    BadgeRed,
                    Offset(size.width / 2, 0f),
                    Offset(size.width / 2, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                )
            }
        }
    }
}

/** Ring with the percentage inside. */
@Composable
fun PercentRing(percent: Double, color: Color, modifier: Modifier = Modifier, size: Dp = 96.dp, decimals: Int = 0) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            drawArc(color.copy(alpha = 0.15f), 0f, 360f, false, style = Stroke(stroke))
            drawArc(color, -90f, (360f * percent / 100f).toFloat(), false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(percentLabel(percent, decimals), color = color, fontSize = if (size >= 90.dp) AppTextSize.H5 else AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
    }
}

/** Small coloured dot + "Absent · 3" heading on a grouped list card. */
@Composable
fun GroupHeader(color: Color, title: String) {
    Row(Modifier.fillMaxWidth().background(ScreenBackground).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(12.dp))
        Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
    }
}

/** "Sundays and holidays aren’t counted as absent…" and the Powered-by row. */
@Composable
fun AttendanceFooter(modifier: Modifier = Modifier, note: String = stringResource(R.string.att_footer_rules)) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(note, Modifier.fillMaxWidth(), color = TextSecondary, fontSize = AppTextSize.Label, lineHeight = 20.sp)
        Spacer(Modifier.height(18.dp))
        PoweredByRow(textColor = TextSecondary)
    }
}

/** A white card that holds a list of rows. */
@Composable
fun ListCard(modifier: Modifier = Modifier, border: Color = FieldBorder, content: @Composable () -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, border)) {
        Column { content() }
    }
}

// ---- Export sheet (PDF / Excel → saved) ----

/**
 * "Export today's report": pick PDF or Excel, see the file name, export; then "Export
 * complete" with the saved file.
 */
@Composable
fun ExportFormatSheet(
    title: String,
    subtitle: String,
    fileBaseName: String,
    format: ExportFormat,
    exporting: Boolean,
    failed: Boolean,
    result: ExportedFile?,
    onFormat: (ExportFormat) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    FacultySheet(onDismiss) {
        SheetHeader(title, subtitle, onDismiss)
        Spacer(Modifier.height(18.dp))
        if (result == null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioCard(stringResource(R.string.att_export_pdf), stringResource(R.string.att_export_pdf_sub), format == ExportFormat.PDF, { onFormat(ExportFormat.PDF) })
                RadioCard(stringResource(R.string.att_export_excel), stringResource(R.string.att_export_excel_sub), format == ExportFormat.EXCEL, { onFormat(ExportFormat.EXCEL) })
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = ScreenBackground, border = BorderStroke(1.dp, FieldBorder)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.att_export_file_name), color = TextSecondary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                        Text("$fileBaseName.${format.extension}", color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (failed) Text(stringResource(R.string.att_export_failed), color = BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = onExport,
                    enabled = !exporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    if (exporting) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.5.dp)
                    } else {
                        Icon(painterResource(R.drawable.ic_download), null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(if (format == ExportFormat.PDF) R.string.att_export_as_pdf else R.string.att_export_as_excel),
                            fontSize = AppTextSize.BodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(OnboardingGreenDisc), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = AttGreen, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.att_export_complete), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
                Text(
                    result.name,
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { FacultyExport.open(context, result) }.padding(4.dp),
                    color = BrandBlue,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(
                        R.string.att_export_saved_detail,
                        result.count,
                        SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date(result.savedAt)) + ", " + clockLabel(result.savedAt),
                    ),
                    color = TextSecondary,
                    fontSize = AppTextSize.Body,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Text(stringResource(R.string.att_done), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Amber outline for the "watch" days on heat-map calendars. */
val WatchBorder = OnboardingAmberRing
