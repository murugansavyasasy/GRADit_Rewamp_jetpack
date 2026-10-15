package com.vsca.vsnapvoicecollege.ui.dashboard.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.ConfirmDialog
import com.vsca.vsnapvoicecollege.ui.components.DialogButtons
import com.vsca.vsnapvoicecollege.ui.components.DialogCard
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Longest folder name allowed. */
private const val MAX_FOLDER_NAME = 40

private sealed interface FolderDialog {
    data object Create : FolderDialog
    data class Rename(val folder: FileFolder) : FolderDialog
    data class Delete(val folder: FileFolder, val fileCount: Int) : FolderDialog
}

/** Staff folders: create, rename and delete (files in a deleted folder move to "Unsorted"). */
@Composable
fun FoldersScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val folders by FileRepository.folders.collectAsStateWithLifecycle()
    val files by FileRepository.files.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<FolderDialog?>(null) }
    val counts = files.groupingBy { it.folderId }.eachCount()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(ScreenBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.files_folders_title),
                subtitle = stringResource(
                    R.string.files_folders_subtitle,
                    pluralStringResource(R.plurals.files_folder_count, folders.size, folders.size),
                    pluralStringResource(R.plurals.files_count, files.size, files.size),
                ),
                onBack = onBack,
            )
        }
        item(key = "create") {
            Button(
                onClick = { dialog = FolderDialog.Create },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Icon(painterResource(R.drawable.ic_create_new_folder), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.files_create_folder), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(folders, key = { _, folder -> folder.id }) { index, folder ->
            val count = counts[folder.id] ?: 0
            ListCardSegment(index = index, last = index == folders.lastIndex) {
                Row(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(AmberChipBackground, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_folder_outline), contentDescription = null, tint = OnboardingAmberCard, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                        Text(pluralStringResource(R.plurals.files_count, count, count), color = TextSecondary, fontSize = AppTextSize.Body)
                    }
                    IconButton(onClick = { dialog = FolderDialog.Rename(folder) }) {
                        Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.files_rename_folder_desc, folder.name), tint = TextPrimary)
                    }
                    IconButton(onClick = { dialog = FolderDialog.Delete(folder, count) }) {
                        Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.files_delete_folder_desc, folder.name), tint = BadgeRed)
                    }
                }
                HorizontalDivider(color = FieldBorder)
            }
        }
    }

    when (val current = dialog) {
        FolderDialog.Create -> FolderNameDialog(
            title = stringResource(R.string.files_new_folder),
            confirm = stringResource(R.string.files_create),
            initial = "",
            existing = folders.map { it.name },
            onDismiss = { dialog = null },
            onConfirm = { name ->
                FileRepository.createFolder(name)
                dialog = null
            },
        )
        is FolderDialog.Rename -> FolderNameDialog(
            title = stringResource(R.string.files_rename_folder),
            confirm = stringResource(R.string.files_save),
            initial = current.folder.name,
            existing = folders.filter { it.id != current.folder.id }.map { it.name },
            onDismiss = { dialog = null },
            onConfirm = { name ->
                FileRepository.renameFolder(current.folder.id, name)
                dialog = null
            },
        )
        is FolderDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.files_delete_folder_title, current.folder.name),
            body = pluralStringResource(R.plurals.files_delete_folder_body, current.fileCount, current.fileCount),
            confirm = stringResource(R.string.files_delete_folder),
            destructive = true,
            onDismiss = { dialog = null },
            onConfirm = {
                FileRepository.deleteFolder(current.folder.id)
                dialog = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    confirm: String,
    initial: String,
    existing: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val name = value.text.trim()
    val duplicate = existing.any { it.equals(name, ignoreCase = true) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    DialogCard(onDismiss = onDismiss) {
        Text(title, color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.files_folder_name), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.text.length <= MAX_FOLDER_NAME) value = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            placeholder = { Text(stringResource(R.string.files_folder_name_hint)) },
            singleLine = true,
            isError = duplicate,
            supportingText = if (duplicate) {
                { Text(stringResource(R.string.files_folder_exists), color = BadgeRed) }
            } else {
                null
            },
            shape = RoundedCornerShape(16.dp),
            colors = appTextFieldColors(),
        )
        Spacer(Modifier.height(14.dp))
        DialogButtons(
            confirm = confirm,
            confirmEnabled = name.isNotEmpty() && !duplicate,
            onDismiss = onDismiss,
            onConfirm = { onConfirm(name) },
        )
    }
}
