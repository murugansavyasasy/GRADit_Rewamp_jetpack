package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AttachmentIcon
import com.vsca.vsnapvoicecollege.ui.components.CategoryChip
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.FormCard
import com.vsca.vsnapvoicecollege.ui.components.IMAGE_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.IconLine
import com.vsca.vsnapvoicecollege.ui.components.PDF_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.SwitchRow
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.pickDateTime
import com.vsca.vsnapvoicecollege.ui.components.readAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.launch

/**
 * Create / edit notice: title, formatted message, category, priority, audience,
 * attachments and delivery (schedule, expiry, pin, SMS). Problems are listed at the
 * top once the user tries to publish.
 */
@Composable
fun NoticeComposeScreen(
    viewModel: NoticeComposeViewModel,
    onBack: () -> Unit,
    onPreview: () -> Unit,
    onPublished: () -> Unit,
    onDraftSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var showExpiry by rememberSaveable { mutableStateOf(false) }

    val addAttachment: (android.net.Uri?) -> Unit = { uri ->
        uri?.let { readAttachment(context, it) }?.let { attachment ->
            if (!viewModel.onAttachment(attachment)) {
                Toast.makeText(context, context.getString(R.string.notice_attach_limit, NoticeComposeUiState.MAX_ATTACHMENTS), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), addAttachment)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), addAttachment)

    val tryGo: (() -> Unit) -> Unit = { next ->
        if (viewModel.checkReady()) next() else scope.launch { scroll.animateScrollTo(0) }
    }

    Column(modifier = modifier.fillMaxSize().background(ScreenBackground).imePadding()) {
        TitledTopBar(
            title = stringResource(if (uiState.mode == ComposeMode.EDIT) R.string.notice_edit_title else R.string.notice_create),
            onBack = onBack,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            if (uiState.showErrors && uiState.issues.isNotEmpty()) IssuesBanner(uiState.issues)
            MessageFields(uiState, viewModel)
            CategoryCard(uiState, viewModel)
            AudienceCard(uiState, viewModel)
            AttachmentsCard(
                attachments = uiState.attachments,
                onAddPdf = { pdfPicker.launch(PDF_MIME_TYPES) },
                onAddImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = viewModel::onRemoveAttachment,
            )
            DeliveryCard(
                uiState = uiState,
                onSchedule = { on -> if (on) pickDateTime(context) { viewModel.onSchedule(it) } else viewModel.onSchedule(null) },
                onExpiry = { showExpiry = true },
                onPinned = viewModel::onPinned,
                onSms = viewModel::onSms,
            )
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.background(White)) {
            HorizontalDivider(color = FieldBorder)
            Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // A live notice can't go back to being a draft; its changes go out with Update.
                if (!uiState.editingLive) {
                    OutlinedButton(
                        onClick = {
                            if (uiState.title.isBlank()) {
                                Toast.makeText(context, R.string.notice_title_error, Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onSaveDraft()
                                Toast.makeText(context, R.string.notice_draft_saved, Toast.LENGTH_SHORT).show()
                                onDraftSaved()
                            }
                        },
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, FieldBorder),
                    ) {
                        Text(stringResource(R.string.notice_save_draft), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = { tryGo(onPreview) },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_visibility_outline), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.notice_preview), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Button(
                    onClick = { tryGo { if (viewModel.onPublish()) onPublished() } },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    Text(publishLabel(uiState), fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }

    if (showExpiry) {
        ExpiryDateSheet(
            initial = uiState.expiresAt,
            minDate = uiState.scheduledAt ?: System.currentTimeMillis(),
            onDismiss = { showExpiry = false },
            onDone = {
                viewModel.onExpiry(it)
                showExpiry = false
            },
        )
    }
}

/** "Publish", "Schedule" or (for a live notice) "Update". */
@Composable
internal fun publishLabel(state: NoticeComposeUiState): String = stringResource(
    when {
        state.isScheduled -> R.string.notice_schedule
        state.editingLive -> R.string.notice_update
        else -> R.string.notice_publish
    },
)

@Composable
private fun IssuesBanner(issues: List<NoticeIssue>) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = AlertBackground) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = BadgeRed, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    pluralStringResource(R.plurals.notice_fix_things, issues.size, issues.size),
                    color = BadgeRed,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(4.dp))
                issues.forEach { issue ->
                    Text("• " + stringResource(issue.label), color = BadgeRed, fontSize = AppTextSize.Body, lineHeight = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun MessageFields(uiState: NoticeComposeUiState, viewModel: NoticeComposeViewModel) {
    val titleError = uiState.showErrors && NoticeIssue.TITLE_MISSING in uiState.issues
    FormCard {
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.notice_field_title))
                withStyle(SpanStyle(color = BadgeRed)) { append(" *") }
            },
            color = TextPrimary,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.notice_field_title_hint), color = TextMuted) },
            singleLine = true,
            isError = titleError,
            shape = RoundedCornerShape(16.dp),
            colors = appTextFieldColors(),
        )
        if (titleError) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = BadgeRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.notice_title_error), color = BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
            }
        }
        Counter(uiState.title.length, NoticeComposeUiState.MAX_TITLE)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.notice_field_message), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        MessageEditor(uiState, viewModel)
        Counter(uiState.body.text.length, NoticeComposeUiState.MAX_BODY)
    }
}

@Composable
private fun Counter(length: Int, max: Int) {
    Text(
        stringResource(R.string.notice_char_count, length, max),
        Modifier.fillMaxWidth().padding(top = 8.dp),
        color = TextSecondary,
        fontSize = AppTextSize.Label,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun MessageEditor(uiState: NoticeComposeUiState, viewModel: NoticeComposeViewModel) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(18.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) BrandBlue else FieldBorder, RoundedCornerShape(18.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(ScreenBackground, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            ToolbarButton(R.drawable.ic_format_bold, stringResource(R.string.notice_format_bold), viewModel::onToggleBold)
            Spacer(Modifier.width(6.dp))
            ToolbarButton(R.drawable.ic_format_list, stringResource(R.string.notice_format_list), viewModel::onToggleBullets)
        }
        HorizontalDivider(color = FieldBorder)
        Box(Modifier.padding(16.dp)) {
            if (uiState.body.text.isEmpty()) {
                Text(stringResource(R.string.notice_field_message_hint), color = TextMuted, fontSize = AppTextSize.BodyLarge)
            }
            BasicTextField(
                value = uiState.body,
                onValueChange = viewModel::onBodyChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = AppTextSize.BodyLarge, lineHeight = 26.sp),
                cursorBrush = SolidColor(BrandBlue),
                interactionSource = interaction,
            )
        }
    }
}

@Composable
private fun ToolbarButton(@DrawableRes iconRes: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(iconRes), description, tint = TextPrimary, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryCard(uiState: NoticeComposeUiState, viewModel: NoticeComposeViewModel) {
    FormCard {
        SectionTitle(stringResource(R.string.notice_field_category))
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NoticeCategory.entries.forEach { category ->
                SelectChip(
                    text = stringResource(category.label),
                    selected = category == uiState.category,
                    onClick = { viewModel.onCategory(category) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionTitle(stringResource(R.string.notice_field_priority))
        Spacer(Modifier.height(12.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = BadgeNeutralBackground) {
            Row(Modifier.padding(4.dp)) {
                NoticePriority.entries.forEach { priority ->
                    val selected = priority == uiState.priority
                    Surface(
                        onClick = { viewModel.onPriority(priority) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) White else BadgeNeutralBackground,
                        shadowElevation = if (selected) 1.dp else 0.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(priority.label),
                                color = when {
                                    selected && priority == NoticePriority.URGENT -> BadgeRed
                                    selected -> TextPrimary
                                    else -> TextSecondary
                                },
                                fontSize = AppTextSize.BodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        if (uiState.priority == NoticePriority.URGENT) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.notice_urgent_hint), color = TextSecondary, fontSize = AppTextSize.Label)
        }
    }
}

/** Pill chip with a check mark when selected (category, class and role pickers). */
@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) BrandBlue else White,
        border = if (selected) null else BorderStroke(1.dp, FieldBorder),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, color = if (selected) White else TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudienceCard(uiState: NoticeComposeUiState, viewModel: NoticeComposeViewModel) {
    val error = uiState.showErrors && NoticeIssue.NO_AUDIENCE in uiState.issues
    FormCard {
        SectionTitle(stringResource(R.string.notice_field_audience))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.notice_field_classes), color = TextSecondary, fontSize = AppTextSize.Body)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NoticeRepository.classes.forEach { group ->
                SelectChip(group.label, group.id in uiState.classIds) { viewModel.onClassToggled(group.id) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.notice_field_who), color = TextSecondary, fontSize = AppTextSize.Body)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NoticeRole.entries.forEach { role ->
                CategoryChip(text = stringResource(role.label), selected = role in uiState.roles, onClick = { viewModel.onRoleToggled(role) })
            }
        }
        Spacer(Modifier.height(14.dp))
        if (error) {
            IconLine(R.drawable.ic_error, stringResource(R.string.notice_audience_error), color = BadgeRed)
        } else {
            IconLine(R.drawable.ic_people_outline, recipientBreakdown(uiState), color = TextPrimary)
        }
    }
}

/** "198 recipients · 100 students, 98 parents". */
@Composable
internal fun recipientBreakdown(state: NoticeComposeUiState): String {
    val parts = NoticeRole.entries.filter { it in state.roles }.mapNotNull { role ->
        val count = state.count(role)
        if (count == 0) null else pluralStringResource(rolePlural(role), count, count)
    }
    val total = pluralStringResource(R.plurals.notice_recipients, state.recipientCount, state.recipientCount)
    return if (parts.isEmpty()) total else stringResource(R.string.notice_recipient_breakdown, total, parts.joinToString(", "))
}

internal fun rolePlural(role: NoticeRole): Int = when (role) {
    NoticeRole.STUDENTS -> R.plurals.notice_students
    NoticeRole.PARENTS -> R.plurals.notice_parents
    NoticeRole.STAFF -> R.plurals.notice_staff
}

@Composable
private fun AttachmentsCard(
    attachments: List<FileAttachment>,
    onAddPdf: () -> Unit,
    onAddImage: () -> Unit,
    onRemove: (FileAttachment) -> Unit,
) {
    FormCard {
        SectionTitle(stringResource(R.string.notice_field_attachments))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.notice_attach_hint, NoticeComposeUiState.MAX_ATTACHMENTS), color = TextSecondary, fontSize = AppTextSize.Label)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AddButton(R.drawable.ic_file, stringResource(R.string.notice_add_pdf), Modifier.weight(1f), onAddPdf)
            AddButton(R.drawable.ic_image_outline, stringResource(R.string.notice_add_image), Modifier.weight(1f), onAddImage)
        }
        attachments.forEach { attachment ->
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = ScreenBackground) {
                Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AttachmentIcon(attachment.type, size = 24.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(attachment.name, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(fileSizeLabel(attachment.sizeBytes), color = TextSecondary, fontSize = AppTextSize.Caption)
                    }
                    IconButton(onClick = { onRemove(attachment) }) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.notice_remove_file, attachment.name), tint = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddButton(@DrawableRes iconRes: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        color = SelectedRowBackground,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(iconRes), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DeliveryCard(
    uiState: NoticeComposeUiState,
    onSchedule: (Boolean) -> Unit,
    onExpiry: () -> Unit,
    onPinned: (Boolean) -> Unit,
    onSms: (Boolean) -> Unit,
) {
    val expiryError = uiState.showErrors && NoticeIssue.EXPIRY_BEFORE_PUBLISH in uiState.issues
    val scheduleError = uiState.showErrors && NoticeIssue.SCHEDULE_IN_PAST in uiState.issues
    FormCard {
        SectionTitle(stringResource(R.string.notice_field_delivery))
        SwitchRow(
            title = stringResource(R.string.notice_schedule_later),
            subtitle = uiState.scheduledAt?.let { stringResource(R.string.notice_goes_out, longTimeLabel(it)) }
                ?: stringResource(R.string.notice_publish_now),
            checked = uiState.isScheduled,
            onCheckedChange = onSchedule,
        )
        if (scheduleError) IconLine(R.drawable.ic_error, stringResource(R.string.notice_issue_schedule_past), color = BadgeRed)
        HorizontalDivider(color = FieldBorder)
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onExpiry).padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.notice_expiry_title), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    uiState.expiresAt?.let { dayLabel(it) } ?: stringResource(R.string.notice_no_expiry_set),
                    color = if (expiryError) BadgeRed else TextSecondary,
                    fontSize = AppTextSize.Label,
                )
            }
            Icon(painterResource(R.drawable.ic_calendar), stringResource(R.string.notice_pick_expiry), tint = BrandBlue, modifier = Modifier.size(24.dp))
        }
        if (expiryError) IconLine(R.drawable.ic_error, stringResource(R.string.notice_issue_expiry), color = BadgeRed)
        HorizontalDivider(color = FieldBorder)
        SwitchRow(
            title = stringResource(R.string.notice_pin_title),
            subtitle = stringResource(if (uiState.expiresAt != null) R.string.notice_pin_until_expiry else R.string.notice_pin_until_unpinned),
            checked = uiState.pinned,
            onCheckedChange = onPinned,
        )
        HorizontalDivider(color = FieldBorder)
        SwitchRow(
            title = stringResource(R.string.notice_sms_title),
            subtitle = stringResource(R.string.notice_sms_subtitle),
            checked = uiState.sms,
            onCheckedChange = onSms,
        )
    }
}
