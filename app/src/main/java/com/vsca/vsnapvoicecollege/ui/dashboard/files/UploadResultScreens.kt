package com.vsca.vsnapvoicecollege.ui.dashboard.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.FormCard
import com.vsca.vsnapvoicecollege.ui.components.IconLine
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Step 2: how receivers will see the post in Campus → Files, before publishing. */
@Composable
fun UploadPreviewScreen(
    viewModel: UploadViewModel,
    onEdit: () -> Unit,
    onPublished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val items = uiState.readyItems
    val firstImage = items.firstOrNull { it.format?.kind == FileKind.IMAGE }
    val image by produceState<Bitmap?>(initialValue = null, firstImage?.uri) {
        val uri = firstImage?.uri ?: return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })
                }
            }.getOrNull()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.files_preview),
            subtitle = stringResource(R.string.files_upload_step2),
            onBack = onEdit,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = SelectedRowBackground) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(painterResource(R.drawable.ic_visibility_outline), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.files_preview_banner, rolesLabel(uiState.roles).lowercase(), uiState.group?.label.orEmpty()),
                        color = BrandBlue,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                    )
                }
            }
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = FieldBorder.copy(alpha = 0.6f)) {
                Surface(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = White,
                    border = BorderStroke(1.dp, FieldBorder),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                                Text(uiState.senderInitials, color = White, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uiState.senderName, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    stringResource(
                                        R.string.files_meta,
                                        uiState.scheduledAt?.let { scheduleLabel(it) }
                                            ?: stringResource(R.string.files_today_at, clockLabel(System.currentTimeMillis())),
                                        pluralStringResource(R.plurals.files_count, items.size, items.size),
                                    ),
                                    color = TextSecondary,
                                    fontSize = AppTextSize.Label,
                                )
                            }
                            uiState.category?.let { LabelPill(stringResource(it.label), AmberChipBackground, OnboardingAmberCard) }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(uiState.title, color = TextPrimary, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                        if (uiState.description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(uiState.description, color = TextSecondary, fontSize = AppTextSize.BodyLarge, lineHeight = 24.sp)
                        }
                        if (firstImage != null) {
                            Spacer(Modifier.height(14.dp))
                            Box(
                                Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp)).background(PhotoGradient),
                                contentAlignment = Alignment.Center,
                            ) {
                                val bitmap = image
                                if (bitmap != null) {
                                    Image(bitmap.asImageBitmap(), contentDescription = firstImage.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(painterResource(R.drawable.ic_image_outline), contentDescription = null, tint = White, modifier = Modifier.size(56.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        items.forEach { item ->
                            HorizontalDivider(color = FieldBorder)
                            PreviewFileRow(item) {
                                Toast.makeText(context, R.string.files_preview_download_note, Toast.LENGTH_SHORT).show()
                            }
                        }
                        uiState.expiresAt?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.files_available_until, longDateLabel(it)), color = TextSecondary, fontSize = AppTextSize.Body)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.background(White)) {
            HorizontalDivider(color = FieldBorder)
            Row(modifier = Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                ) {
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.files_edit), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
                PublishButton(modifier = Modifier.weight(1f)) {
                    if (viewModel.onPublish()) {
                        onPublished()
                    } else {
                        uiState.blocker?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewFileRow(item: UploadItem, onDownload: () -> Unit) {
    val format = item.format ?: return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        FileTile(format, size = 54.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeChip(format)
                Spacer(Modifier.width(8.dp))
                Text(fileSizeLabel(item.sizeBytes), color = TextSecondary, fontSize = AppTextSize.Label)
            }
        }
        Surface(
            onClick = onDownload,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
            color = White,
            border = BorderStroke(1.dp, FieldBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_download), stringResource(R.string.files_download), tint = BrandBlue, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** "Files published" confirmation with who was reached and how. */
@Composable
fun UploadPublishedScreen(
    viewModel: UploadViewModel,
    onUploadMore: () -> Unit,
    onViewFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.published ?: return
    Column(
        modifier = modifier.fillMaxSize().background(ScreenBackground).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        FormCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(OnboardingGreenDisc), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = BrandGreen, modifier = Modifier.size(52.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(if (summary.scheduledAt != null) R.string.files_scheduled_title else R.string.files_published_title),
                    color = TextPrimary,
                    fontSize = AppTextSize.H2,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(12.dp))
                val filesText = pluralStringResource(R.plurals.files_count, summary.fileCount, summary.fileCount)
                val peopleText = pluralStringResource(R.plurals.files_people, summary.people, summary.people)
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) { append(filesText) }
                        append(" ")
                        append(stringResource(R.string.files_shared_with))
                        append(" ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) { append(peopleText) }
                    },
                    color = TextSecondary,
                    fontSize = AppTextSize.BodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = listOfNotNull(
                        summary.groupLabel,
                        breakdown(summary).takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    color = TextSecondary,
                    fontSize = AppTextSize.BodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = ScreenBackground) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconLine(
                            R.drawable.ic_notifications,
                            summary.scheduledAt?.let { stringResource(R.string.files_push_scheduled, scheduleLabel(it)) }
                                ?: stringResource(R.string.files_push_sent),
                            color = TextPrimary,
                        )
                        summary.smsParents?.let {
                            IconLine(R.drawable.ic_chat_bubble_outline, pluralStringResource(R.plurals.files_sms_sent, it, it), color = TextPrimary)
                        }
                        val folder = summary.folderName ?: stringResource(R.string.files_unsorted)
                        IconLine(
                            R.drawable.ic_folder_outline,
                            summary.expiresAt?.let { stringResource(R.string.files_saved_in_until, folder, dayMonthLabel(it)) }
                                ?: stringResource(R.string.files_saved_in, folder),
                            color = TextPrimary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onUploadMore,
                modifier = Modifier.weight(1f).height(58.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FieldBorder),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.files_upload_more), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onViewFiles,
                modifier = Modifier.weight(1f).height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Text(stringResource(R.string.files_view_files), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
        PoweredByRow(Modifier.fillMaxWidth().padding(vertical = 24.dp), textColor = TextSecondary)
    }
}

/** "60 students and 57 parents" (only the roles that were included). */
@Composable
private fun breakdown(summary: PublishSummary): String {
    val parts = listOfNotNull(
        summary.students.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.files_students, it, it) },
        summary.parents.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.files_parents, it, it) },
        summary.staff.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.files_staff, it, it) },
    )
    return when (parts.size) {
        0 -> ""
        1 -> parts[0]
        else -> stringResource(R.string.files_and, parts.dropLast(1).joinToString(", "), parts.last())
    }
}
