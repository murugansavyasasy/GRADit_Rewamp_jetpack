package com.vsca.vsnapvoicecollege.ui.dashboard.resume

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AppTextField
import com.vsca.vsnapvoicecollege.ui.components.ConfirmDialog
import com.vsca.vsnapvoicecollege.ui.components.DialogButtons
import com.vsca.vsnapvoicecollege.ui.components.DialogCard
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Labels and examples for the add / edit dialog of a section. */
private data class EntryFields(
    @param:StringRes val title: Int,
    @param:StringRes val titleHint: Int,
    @param:StringRes val detail: Int? = null,
    @param:StringRes val detailHint: Int? = null,
)

private fun fieldsFor(section: ResumeSection): EntryFields = when (section) {
    ResumeSection.PROJECTS -> EntryFields(R.string.resume_field_project, R.string.resume_hint_project, R.string.resume_field_project_detail, R.string.resume_hint_project_detail)
    ResumeSection.INTERNSHIPS -> EntryFields(R.string.resume_field_internship, R.string.resume_hint_internship, R.string.resume_field_internship_detail, R.string.resume_hint_internship_detail)
    ResumeSection.ACHIEVEMENTS -> EntryFields(R.string.resume_field_achievement, R.string.resume_hint_achievement)
    ResumeSection.LANGUAGES -> EntryFields(R.string.resume_field_language, R.string.resume_hint_language)
    else -> EntryFields(R.string.resume_field_skill, R.string.resume_hint_skill)
}

/** Add, edit and remove the entries of one section the student fills in. */
@Composable
fun ResumeSectionScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ResumeSectionViewModel = viewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val section = viewModel.section
    // null = closed, "" = adding, otherwise the id being edited.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var removing by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize().background(ScreenBackground).imePadding()) {
        TitledTopBar(
            title = stringResource(section.label),
            subtitle = stringResource(R.string.resume_section_subtitle),
            onBack = onBack,
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)) {
            if (entries.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                    StateMessage(
                        iconRes = section.icon,
                        iconTint = BrandBlue,
                        iconBackground = SelectedRowBackground,
                        title = stringResource(R.string.resume_section_empty_title),
                        subtitle = stringResource(R.string.resume_section_empty_body),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                    )
                }
            } else {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        entries.forEachIndexed { index, entry ->
                            if (index > 0) HorizontalDivider(color = FieldBorder)
                            EntryRow(entry, onEdit = { editing = entry.id }, onRemove = { removing = entry.id })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.resume_section_note), color = TextSecondary, fontSize = AppTextSize.Label, lineHeight = 19.sp)
        }
        PrimaryButton(
            text = stringResource(R.string.resume_add_entry),
            onClick = { editing = "" },
            modifier = Modifier.padding(16.dp),
        )
    }

    editing?.let { id ->
        val entry = entries.firstOrNull { it.id == id }
        EntryDialog(
            section = section,
            entry = entry,
            onDismiss = { editing = null },
            onSave = { title, detail ->
                viewModel.save(entry?.id, title, detail)
                editing = null
            },
        )
    }
    removing?.let { id ->
        val entry = entries.firstOrNull { it.id == id }
        if (entry == null) {
            removing = null
        } else {
            ConfirmDialog(
                title = stringResource(R.string.resume_remove_title),
                body = stringResource(R.string.resume_remove_body, entry.title),
                confirm = stringResource(R.string.resume_remove),
                destructive = true,
                onDismiss = { removing = null },
                onConfirm = {
                    viewModel.remove(id)
                    removing = null
                },
            )
        }
    }
}

@Composable
private fun EntryRow(entry: ResumeEntry, onEdit: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry.title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            entry.detail?.let { Text(it, color = TextSecondary, fontSize = AppTextSize.Label, lineHeight = 18.sp) }
        }
        IconButton(onClick = onEdit) {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.resume_edit), tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onRemove) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.resume_remove), tint = BadgeRed, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EntryDialog(section: ResumeSection, entry: ResumeEntry?, onDismiss: () -> Unit, onSave: (String, String?) -> Unit) {
    val fields = fieldsFor(section)
    var title by rememberSaveable { mutableStateOf(entry?.title.orEmpty()) }
    var detail by rememberSaveable { mutableStateOf(entry?.detail.orEmpty()) }
    DialogCard(onDismiss) {
        Text(
            stringResource(if (entry == null) R.string.resume_add_title else R.string.resume_edit_title, stringResource(section.label)),
            color = TextPrimary,
            fontSize = AppTextSize.H6,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppTextField(
                value = title,
                onValueChange = { title = it.take(MAX_TITLE) },
                label = stringResource(fields.title),
                placeholder = stringResource(fields.titleHint),
            )
            if (fields.detail != null && fields.detailHint != null) {
                AppTextField(
                    value = detail,
                    onValueChange = { detail = it.take(MAX_DETAIL) },
                    label = stringResource(fields.detail),
                    placeholder = stringResource(fields.detailHint),
                    singleLine = false,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        DialogButtons(
            confirm = stringResource(R.string.resume_save),
            confirmEnabled = title.isNotBlank(),
            onDismiss = onDismiss,
            onConfirm = { onSave(title, detail.takeIf { fields.detail != null }) },
        )
    }
}

private const val MAX_TITLE = 80
private const val MAX_DETAIL = 140
