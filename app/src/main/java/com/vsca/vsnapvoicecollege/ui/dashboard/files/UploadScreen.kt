package com.vsca.vsnapvoicecollege.ui.dashboard.files

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.CategoryChip
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.FormCard
import com.vsca.vsnapvoicecollege.ui.components.IconLine
import com.vsca.vsnapvoicecollege.ui.components.SwitchRow
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.pickDate
import com.vsca.vsnapvoicecollege.ui.components.pickDateTime
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Step 1 of uploading: add files (camera, gallery, files, video), then title,
 * description, category, folder, audience and delivery options.
 */
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onBack: () -> Unit,
    onPreview: () -> Unit,
    onPublished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun addUris(uris: List<Uri>) {
        if (uris.isNotEmpty()) viewModel.onFilesPicked(uris.map { readPickedFile(context, it) })
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) addUris(listOf(uri.toUri()))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_FILES_PER_UPLOAD)) { addUris(it) }
    val videos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_FILES_PER_UPLOAD)) { addUris(it) }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { addUris(it) }

    val tryProceed: (() -> Unit) -> Unit = { next ->
        val blocker = uiState.blocker
        if (blocker != null) Toast.makeText(context, blocker, Toast.LENGTH_SHORT).show() else next()
    }

    Column(modifier = modifier.fillMaxSize().background(ScreenBackground).imePadding()) {
        TitledTopBar(
            title = stringResource(R.string.files_upload_files),
            subtitle = stringResource(R.string.files_upload_step1),
            onBack = onBack,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            UploadProblemsBanner(uiState)
            FormCard {
                SectionTitle(stringResource(R.string.files_add_files))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceButton(R.drawable.ic_camera_outline, stringResource(R.string.files_source_camera), Modifier.weight(1f)) {
                        val uri = newCameraPhotoUri(context)
                        cameraUri = uri.toString()
                        camera.launch(uri)
                    }
                    SourceButton(R.drawable.ic_image_outline, stringResource(R.string.files_source_gallery), Modifier.weight(1f)) {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    SourceButton(R.drawable.ic_file, stringResource(R.string.files_source_files), Modifier.weight(1f)) {
                        documents.launch(FileFormat.documentMimeTypes)
                    }
                    SourceButton(R.drawable.ic_videocam_outline, stringResource(R.string.files_source_video), Modifier.weight(1f)) {
                        videos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                }
                Spacer(Modifier.height(12.dp))
                LimitsBox()
                if (uiState.items.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(
                            R.string.files_upload_progress_summary,
                            uiState.acceptedCount,
                            MAX_FILES_PER_UPLOAD,
                            uiState.readyItems.size,
                        ),
                        color = TextPrimary,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = FieldBorder)
                    uiState.items.forEach { item ->
                        UploadRow(item, onRetry = { viewModel.onRetry(item.id) }, onRemove = { viewModel.onRemove(item.id) })
                    }
                }
            }
            DetailsCard(uiState, viewModel)
            AudienceCard(uiState, viewModel)
            DeliveryCard(uiState, viewModel)
            Spacer(Modifier.height(8.dp))
        }

        Column(Modifier.background(White)) {
            HorizontalDivider(color = FieldBorder)
            Row(
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.onSaveDraft()
                        Toast.makeText(context, R.string.files_draft_saved, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Text(stringResource(R.string.files_save_draft), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { tryProceed(onPreview) },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_visibility_outline), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.files_preview), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                PublishButton(modifier = Modifier.weight(1f)) {
                    tryProceed { if (viewModel.onPublish()) onPublished() }
                }
            }
        }
    }
}

@Composable
internal fun PublishButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
    ) {
        Icon(painterResource(R.drawable.ic_send), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.files_publish), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun UploadProblemsBanner(uiState: UploadUiState) {
    val failed = uiState.failedCount
    val rejected = uiState.rejectedCount
    if (failed == 0 && rejected == 0) return
    val parts = listOfNotNull(
        if (failed > 0) pluralStringResource(R.plurals.files_failed_banner, failed, failed) else null,
        if (rejected > 0) pluralStringResource(R.plurals.files_rejected_banner, rejected, rejected) else null,
    )
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AlertBackground) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = BadgeRed, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(parts.joinToString(" "), color = BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun FieldLabel(text: String, optional: Boolean = false) {
    Text(
        text = buildAnnotatedString {
            append(text)
            if (optional) {
                append(" ")
                withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Normal)) {
                    append(stringResource(R.string.files_optional))
                }
            }
        },
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        color = TextPrimary,
        fontSize = AppTextSize.BodyLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SourceButton(@DrawableRes icon: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(44.dp).background(SelectedRowBackground, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, color = TextPrimary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun LimitsBox() {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = ScreenBackground) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LimitLine(stringResource(R.string.files_limit_images_label), stringResource(R.string.files_limit_images))
            LimitLine(stringResource(R.string.files_limit_docs_label), stringResource(R.string.files_limit_docs))
            LimitLine(stringResource(R.string.files_limit_videos_label), stringResource(R.string.files_limit_videos))
            Text(stringResource(R.string.files_limit_count, MAX_FILES_PER_UPLOAD), color = TextSecondary, fontSize = AppTextSize.Label)
        }
    }
}

@Composable
private fun LimitLine(label: String, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) { append(label) }
            append(" ")
            append(value)
        },
        color = TextSecondary,
        fontSize = AppTextSize.Label,
        lineHeight = 19.sp,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadRow(item: UploadItem, onRetry: () -> Unit, onRemove: () -> Unit) {
    val format = item.format
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        if (format != null) FileTile(format, size = 56.dp) else UnknownTile()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (format != null) TypeChip(format)
                Text(fileSizeLabel(item.sizeBytes), color = TextSecondary, fontSize = AppTextSize.Label, modifier = Modifier.align(Alignment.CenterVertically))
                when (item.status) {
                    UploadStatus.Uploaded -> StatusText(stringResource(R.string.files_uploaded), BrandGreen)
                    is UploadStatus.Failed -> StatusText(stringResource(R.string.files_upload_failed_short), BadgeRed)
                    is UploadStatus.Rejected -> StatusText(stringResource(R.string.files_not_added), BadgeRed)
                    is UploadStatus.Uploading -> Unit
                }
            }
            when (val status = item.status) {
                is UploadStatus.Uploading -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.files_uploading, (status.progress * 100).toInt()),
                        color = BrandBlue,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = BrandBlue,
                        trackColor = FieldBorder,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
                is UploadStatus.Failed -> {
                    Spacer(Modifier.height(6.dp))
                    ErrorLine(stringResource(R.string.files_upload_failed_at, (status.progress * 100).toInt()))
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, BrandBlue),
                    ) {
                        Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.files_retry), color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }
                is UploadStatus.Rejected -> {
                    Spacer(Modifier.height(6.dp))
                    ErrorLine(
                        when (status.reason) {
                            RejectReason.UNSUPPORTED -> stringResource(R.string.files_reject_unsupported)
                            RejectReason.TOO_LARGE -> stringResource(
                                R.string.files_reject_too_large,
                                fileSizeLabel(format?.kind?.maxBytes ?: 0L),
                            )
                            RejectReason.LIMIT -> stringResource(R.string.files_reject_limit, MAX_FILES_PER_UPLOAD)
                        },
                    )
                }
                UploadStatus.Uploaded -> Unit
            }
        }
        IconButton(onClick = onRemove) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.files_remove_file, item.name), tint = TextSecondary)
        }
    }
    HorizontalDivider(color = FieldBorder)
}

@Composable
private fun UnknownTile() {
    Box(Modifier.size(56.dp).background(BadgeNeutralBackground, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_file), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun StatusText(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, color = color, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
}

@Composable
private fun ErrorLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = BadgeRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = BadgeRed, fontSize = AppTextSize.Body, lineHeight = 20.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsCard(uiState: UploadUiState, viewModel: UploadViewModel) {
    val folders by FileRepository.folders.collectAsStateWithLifecycle()
    FormCard {
        FieldLabel(stringResource(R.string.files_field_title))
        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.files_field_title_hint), color = TextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = appTextFieldColors(),
        )
        FieldLabel(stringResource(R.string.files_field_description), optional = true)
        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
            placeholder = { Text(stringResource(R.string.files_field_description_hint), color = TextMuted) },
            shape = RoundedCornerShape(16.dp),
            colors = appTextFieldColors(),
        )
        FieldLabel(stringResource(R.string.files_field_category))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FileCategory.entries.forEach { category ->
                CategoryChip(
                    text = stringResource(category.label),
                    selected = category == uiState.category,
                    onClick = { viewModel.onCategorySelected(category) },
                )
            }
        }
        FieldLabel(stringResource(R.string.files_field_folder))
        val unsorted = stringResource(R.string.files_unsorted)
        DropdownField(
            value = folders.firstOrNull { it.id == uiState.folderId }?.name ?: unsorted,
            options = folders.map { it.name } + unsorted,
            onOptionSelected = { name -> viewModel.onFolderSelected(folders.firstOrNull { it.name == name }?.id) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudienceCard(uiState: UploadUiState, viewModel: UploadViewModel) {
    FormCard {
        SectionTitle(stringResource(R.string.files_field_audience))
        FieldLabel(stringResource(R.string.files_field_class))
        DropdownField(
            value = uiState.group?.label.orEmpty(),
            options = FileRepository.audienceGroups.map { it.label },
            onOptionSelected = { label ->
                FileRepository.audienceGroups.firstOrNull { it.label == label }?.let(viewModel::onGroupSelected)
            },
        )
        FieldLabel(stringResource(R.string.files_field_who))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AudienceRole.entries.forEach { role ->
                CategoryChip(
                    text = stringResource(role.label),
                    selected = role in uiState.roles,
                    onClick = { viewModel.onRoleToggled(role) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        IconLine(
            R.drawable.ic_people_outline,
            pluralStringResource(R.plurals.files_people_receive, uiState.audienceCount, uiState.audienceCount),
            color = TextPrimary,
        )
    }
}

@Composable
private fun DeliveryCard(uiState: UploadUiState, viewModel: UploadViewModel) {
    val context = LocalContext.current
    FormCard {
        SectionTitle(stringResource(R.string.files_field_delivery))
        SwitchRow(
            title = stringResource(R.string.files_schedule_later),
            subtitle = uiState.scheduledAt?.let { stringResource(R.string.files_schedule_on, scheduleLabel(it)) }
                ?: stringResource(R.string.files_publish_now),
            checked = uiState.scheduledAt != null,
            onCheckedChange = { on ->
                if (on) pickDateTime(context) { viewModel.onScheduleChange(it) } else viewModel.onScheduleChange(null)
            },
        )
        HorizontalDivider(color = FieldBorder)
        SwitchRow(
            title = stringResource(R.string.files_expiry),
            subtitle = uiState.expiresAt?.let { stringResource(R.string.files_visible_until, longDateLabel(it)) }
                ?: stringResource(R.string.files_no_expiry),
            checked = uiState.expiresAt != null,
            onCheckedChange = { on ->
                if (on) pickDate(context) { viewModel.onExpiryChange(it) } else viewModel.onExpiryChange(null)
            },
        )
        HorizontalDivider(color = FieldBorder)
        SwitchRow(
            title = stringResource(R.string.files_allow_download),
            subtitle = stringResource(if (uiState.downloadAllowed) R.string.files_download_allowed_sub else R.string.files_view_only_sub),
            checked = uiState.downloadAllowed,
            onCheckedChange = viewModel::onDownloadAllowedChange,
        )
        HorizontalDivider(color = FieldBorder)
        SwitchRow(
            title = stringResource(R.string.files_notify_sms),
            subtitle = stringResource(R.string.files_notify_sms_sub),
            checked = uiState.notifySms,
            onCheckedChange = viewModel::onNotifySmsChange,
        )
    }
}
