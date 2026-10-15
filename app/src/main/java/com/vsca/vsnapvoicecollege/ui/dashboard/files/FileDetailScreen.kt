package com.vsca.vsnapvoicecollege.ui.dashboard.files

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.ConfirmDialog
import com.vsca.vsnapvoicecollege.ui.components.DialogButtons
import com.vsca.vsnapvoicecollege.ui.components.DialogCard
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.FormCard
import com.vsca.vsnapvoicecollege.ui.components.SwitchRow
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.RadioBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A shared file as staff see it: preview, sharing details, and Edit / Replace / Move /
 * Delete actions.
 */
@Composable
fun FileDetailScreen(fileId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val files by FileRepository.files.collectAsStateWithLifecycle()
    val folders by FileRepository.folders.collectAsStateWithLifecycle()
    val file = files.firstOrNull { it.id == fileId }
    val context = LocalContext.current
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var showMove by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }

    if (file == null) {
        Column(modifier.fillMaxSize().background(ScreenBackground)) {
            TitledTopBar(title = stringResource(R.string.files_details_title), onBack = onBack)
            Text(stringResource(R.string.files_not_found), modifier = Modifier.padding(32.dp), color = TextSecondary)
        }
        return
    }

    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val picked = readPickedFile(context, uri)
        val format = picked.format
        when {
            format == null || format.kind != file.kind ->
                Toast.makeText(context, R.string.files_replace_wrong_type, Toast.LENGTH_SHORT).show()
            picked.sizeBytes > format.kind.maxBytes ->
                Toast.makeText(
                    context,
                    context.getString(R.string.files_reject_too_large, "${format.kind.maxBytes / (1024 * 1024)} MB"),
                    Toast.LENGTH_SHORT,
                ).show()
            else -> {
                // TODO: upload the replacement to the files API before swapping it in.
                FileRepository.replaceContent(file.id, picked.name, format, picked.sizeBytes, uri.toString())
                Toast.makeText(context, R.string.files_replaced, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.files_details_title),
            subtitle = stringResource(
                R.string.files_meta,
                stringResource(file.category.label),
                FileRepository.folderName(file.folderId) ?: stringResource(R.string.files_unsorted),
            ),
            onBack = onBack,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            key(file.uri) { FilePreview(file) }
            FormCard {
                Text(file.title, color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                Spacer(Modifier.height(8.dp))
                TypeChip(file.format)
                Spacer(Modifier.height(6.dp))
                Text(technicalLine(file), color = TextSecondary, fontSize = AppTextSize.Label)
                Spacer(Modifier.height(8.dp))
                InfoRow(stringResource(R.string.files_field_category), stringResource(file.category.label))
                InfoRow(stringResource(R.string.files_field_folder), FileRepository.folderName(file.folderId) ?: stringResource(R.string.files_unsorted))
                InfoRow(
                    stringResource(R.string.files_field_audience),
                    "${audienceLine(file)} (${"%,d".format(file.audienceCount)})",
                )
                InfoRow(stringResource(R.string.files_field_schedule), scheduleText(file))
                InfoRow(stringResource(R.string.files_expiry), expiryText(file))
                InfoRow(
                    stringResource(R.string.files_field_download),
                    stringResource(if (file.downloadAllowed) R.string.files_allowed else R.string.files_not_allowed),
                )
                file.viewedCount?.takeIf { !file.isScheduled() }?.let {
                    Spacer(Modifier.height(12.dp))
                    ViewedProgress(it, file.audienceCount)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(R.drawable.ic_edit, stringResource(R.string.files_edit_details), Modifier.weight(1f)) { showEdit = true }
                ActionButton(R.drawable.ic_swap, stringResource(R.string.files_replace_file), Modifier.weight(1f)) {
                    replacePicker.launch(FileFormat.entries.filter { it.kind == file.kind }.map { it.mimeType }.distinct().toTypedArray())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(R.drawable.ic_drive_file_move, stringResource(R.string.files_move_to_folder), Modifier.weight(1f)) { showMove = true }
                ActionButton(R.drawable.ic_delete, stringResource(R.string.files_delete), Modifier.weight(1f), tint = BadgeRed) { showDelete = true }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showEdit) {
        EditDetailsDialog(file = file, onDismiss = { showEdit = false }) { title, description, category, download ->
            FileRepository.updateDetails(file.id, title, description, category, download)
            showEdit = false
        }
    }
    if (showMove) {
        MoveToFolderSheet(
            currentFolderId = file.folderId,
            folders = folders,
            onDismiss = { showMove = false },
            onMove = { folderId ->
                FileRepository.moveFile(file.id, folderId)
                showMove = false
                Toast.makeText(
                    context,
                    context.getString(R.string.files_moved_to, FileRepository.folderName(folderId) ?: context.getString(R.string.files_unsorted)),
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.files_delete_file_title),
            body = stringResource(R.string.files_delete_file_body, file.title, file.audienceCount),
            confirm = stringResource(R.string.files_delete),
            destructive = true,
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                FileRepository.deleteFile(file.id)
                onBack()
            },
        )
    }
}

/** "IA2_timetable_Sem5A.pdf · 640 KB · 3 pages" / "… · 4032 × 3024" / "… · 04:32". */
@Composable
internal fun technicalLine(file: SharedFile): String {
    val context = LocalContext.current
    val dimensions by produceState<Pair<Int, Int>?>(initialValue = null, file.uri) {
        if (file.kind == FileKind.IMAGE && file.uri != null) {
            value = withContext(Dispatchers.IO) { imageDimensions(context, file.uri) }
        }
    }
    val extra = when (file.kind) {
        FileKind.PDF -> file.pageCount?.let { stringResource(R.string.files_pages, it) }
        FileKind.DOCUMENT -> file.pageCount?.let { stringResource(R.string.files_slides, it) }
        FileKind.VIDEO -> file.durationMs?.let { durationLabel(it) }
        FileKind.IMAGE -> dimensions?.let { (w, h) -> "$w × $h" }
    }
    return listOfNotNull(file.fileName, fileSizeLabel(file.sizeBytes), extra).joinToString(" · ")
}

@Composable
private fun scheduleText(file: SharedFile): String {
    val scheduled = file.scheduledAt?.takeIf { file.isScheduled() }
    return when {
        scheduled != null -> stringResource(R.string.files_scheduled_for, scheduleLabel(scheduled))
        isDaysAgo(file.publishedAt, 0) -> stringResource(R.string.files_published_today, clockLabel(file.publishedAt))
        else -> stringResource(R.string.files_published_on, "${shortDateLabel(file.publishedAt)}, ${clockLabel(file.publishedAt)}")
    }
}

@Composable
private fun expiryText(file: SharedFile): String {
    val expires = file.expiresAt ?: return stringResource(R.string.files_no_expiry)
    val date = longDateLabel(expires)
    return if (file.expiryIsEndOfSemester) stringResource(R.string.files_end_of_semester, date.substringAfter(' ')) else date
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        HorizontalDivider(color = FieldBorder)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
            Text(label, modifier = Modifier.width(110.dp), color = TextSecondary, fontSize = AppTextSize.BodyLarge)
            Text(value, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActionButton(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, FieldBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = White),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = tint, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun EditDetailsDialog(
    file: SharedFile,
    onDismiss: () -> Unit,
    onSave: (String, String, FileCategory, Boolean) -> Unit,
) {
    var title by remember { mutableStateOf(file.title) }
    var description by remember { mutableStateOf(file.description) }
    var category by remember { mutableStateOf(file.category) }
    var download by remember { mutableStateOf(file.downloadAllowed) }
    val categoryLabels = FileCategory.entries.map { stringResource(it.label) }
    DialogCard(onDismiss = onDismiss) {
        Text(stringResource(R.string.files_edit_details), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(UploadUiState.MAX_TITLE) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.files_field_title)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = appTextFieldColors(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(UploadUiState.MAX_DESCRIPTION) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            label = { Text(stringResource(R.string.files_field_description)) },
            placeholder = { Text(stringResource(R.string.files_field_description_hint), color = TextMuted) },
            shape = RoundedCornerShape(14.dp),
            colors = appTextFieldColors(),
        )
        Spacer(Modifier.height(10.dp))
        DropdownField(
            value = stringResource(category.label),
            options = categoryLabels,
            onOptionSelected = { label -> category = FileCategory.entries[categoryLabels.indexOf(label)] },
        )
        SwitchRow(
            title = stringResource(R.string.files_allow_download),
            subtitle = stringResource(if (download) R.string.files_download_allowed_sub else R.string.files_view_only_sub),
            checked = download,
            onCheckedChange = { download = it },
        )
        Spacer(Modifier.height(6.dp))
        DialogButtons(
            confirm = stringResource(R.string.files_save),
            confirmEnabled = title.isNotBlank(),
            onDismiss = onDismiss,
            onConfirm = { onSave(title, description, category, download) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToFolderSheet(
    currentFolderId: String?,
    folders: List<FileFolder>,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit,
) {
    var selected by remember { mutableStateOf(currentFolderId) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val options: List<Pair<String?, String>> = folders.map { it.id to it.name } + (null to stringResource(R.string.files_unsorted))
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = White) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.files_move_to_folder), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.files_move_note), color = TextSecondary, fontSize = AppTextSize.Body)
                }
                IconButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.files_close), tint = TextPrimary)
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(options, key = { it.first ?: UNSORTED_FOLDER_ID }) { (id, name) ->
                    val isSelected = id == selected
                    Surface(
                        onClick = { selected = id },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) SelectedRowBackground else White,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandBlue else FieldBorder),
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selected = id },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandBlue, unselectedColor = RadioBorder),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(if (id == currentFolderId) R.string.files_current_folder else R.string.files_move_here),
                                    color = TextSecondary,
                                    fontSize = AppTextSize.Body,
                                )
                            }
                            if (isSelected) {
                                Text(stringResource(R.string.files_selected), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onMove(selected) },
                enabled = selected != currentFolderId,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Text(stringResource(R.string.files_move_here), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
