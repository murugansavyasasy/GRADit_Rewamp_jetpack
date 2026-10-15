package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AttachmentIcon
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.IMAGE_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.PDF_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.readAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.RadioBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * "Text Msg" composer for management and faculty: who to send to, subject, a
 * formatted message (with templates, attachment and voice typing) and delivery
 * options. Recipients can't reply.
 */
@Composable
fun ComposeMessageScreen(
    onBack: () -> Unit,
    onSent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComposeMessageViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readAttachment(context, it) }?.let(viewModel::onAttachmentPicked)
    }
    val dictation = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !spoken.isNullOrBlank()) viewModel.onDictated(spoken)
    }

    ComposeMessageContent(
        uiState = uiState,
        playback = rememberVoicePlaybackController(),
        onBack = onBack,
        onSendAsSelected = viewModel::onSendAsSelected,
        onRecipientQueryChange = viewModel::onRecipientQueryChange,
        onAddRecipient = viewModel::onAddRecipient,
        onRemoveRecipient = viewModel::onRemoveRecipient,
        onRemoveLastRecipient = viewModel::onRemoveLastRecipient,
        onSubjectChange = viewModel::onSubjectChange,
        onMessageChange = viewModel::onMessageChange,
        onToggleBold = viewModel::onToggleBold,
        onToggleItalic = viewModel::onToggleItalic,
        onToggleBullets = viewModel::onToggleBullets,
        onInsertLink = viewModel::onInsertLink,
        onTemplateSelected = viewModel::onTemplateSelected,
        onAttach = { attachmentPicker.launch(IMAGE_MIME_TYPES + PDF_MIME_TYPES) },
        onRemoveAttachment = viewModel::onRemoveAttachment,
        onRemoveVoice = viewModel::onRemoveVoice,
        onNotifyBySmsChange = viewModel::onNotifyBySmsChange,
        onScheduleToggle = { enabled ->
            if (enabled) pickSchedule(context, viewModel::onScheduleChange) else viewModel.onScheduleChange(null)
        },
        onEditSchedule = { pickSchedule(context, viewModel::onScheduleChange) },
        onDictate = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.msg_dictation_prompt))
            try {
                dictation.launch(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.msg_dictation_unavailable, Toast.LENGTH_SHORT).show()
            }
        },
        onSend = {
            viewModel.onSend()?.let { result ->
                Toast.makeText(context, sentConfirmation(context, result), Toast.LENGTH_LONG).show()
                onSent()
            }
        },
        modifier = modifier,
    )
}

/** "Message sent to 62 parents" / "Message scheduled for 12 Oct, 9:00 AM". */
private fun sentConfirmation(context: Context, result: SendResult): String {
    val scheduledAt = result.scheduledAt
    return if (scheduledAt != null) {
        context.getString(R.string.msg_scheduled_toast, scheduleLabel(context, scheduledAt))
    } else {
        context.resources.getQuantityString(R.plurals.msg_sent_toast, result.count, result.count)
    }
}

private fun scheduleLabel(context: Context, millis: Long): String =
    "${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))}, " +
        DateFormat.getTimeFormat(context).format(Date(millis))

/** Date then time picker; reports a future time, or tells the user to pick one. */
private fun pickSchedule(context: Context, onPicked: (Long) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (picked > System.currentTimeMillis()) {
                        onPicked(picked)
                    } else {
                        Toast.makeText(context, R.string.msg_schedule_past, Toast.LENGTH_SHORT).show()
                    }
                },
                now.get(Calendar.HOUR_OF_DAY) + 1,
                0,
                DateFormat.is24HourFormat(context),
            ).show()
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH),
    ).apply { datePicker.minDate = now.timeInMillis }.show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeMessageContent(
    uiState: ComposeMessageUiState,
    playback: VoicePlaybackController,
    onBack: () -> Unit,
    onSendAsSelected: (SendAs) -> Unit,
    onRecipientQueryChange: (String) -> Unit,
    onAddRecipient: (RecipientGroup) -> Unit,
    onRemoveRecipient: (RecipientGroup) -> Unit,
    onRemoveLastRecipient: () -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (TextFieldValue) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleBullets: () -> Unit,
    onInsertLink: (String) -> Unit,
    onTemplateSelected: (MessageTemplate) -> Unit,
    onAttach: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onRemoveVoice: () -> Unit,
    onNotifyBySmsChange: (Boolean) -> Unit,
    onScheduleToggle: (Boolean) -> Unit,
    onEditSchedule: () -> Unit,
    onDictate: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTemplates by rememberSaveable { mutableStateOf(false) }
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .imePadding(),
    ) {
        MessagesTopBar(title = stringResource(R.string.msg_compose_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            FieldTitle(stringResource(R.string.msg_send_as))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SendAs.entries.forEach { option ->
                    SendAsOption(
                        text = stringResource(option.label),
                        selected = option == uiState.sendAs,
                        onClick = { onSendAsSelected(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            FieldTitle(stringResource(R.string.msg_to))
            RecipientField(
                recipients = uiState.recipients,
                query = uiState.recipientQuery,
                onQueryChange = onRecipientQueryChange,
                onRemove = onRemoveRecipient,
                onRemoveLast = onRemoveLastRecipient,
            )
            if (uiState.suggestions.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                RecipientSuggestions(suggestions = uiState.suggestions, onSelect = onAddRecipient)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (uiState.recipients.isEmpty()) {
                    stringResource(R.string.msg_no_recipients)
                } else {
                    pluralStringResource(uiState.audience.receiveCount, uiState.recipientCount, uiState.recipientCount)
                },
                color = TextSecondary,
                fontSize = AppTextSize.Label,
            )

            FieldTitle(stringResource(R.string.msg_subject))
            OutlinedTextField(
                value = uiState.subject,
                onValueChange = onSubjectChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.msg_subject_hint), color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = appTextFieldColors(),
            )

            FieldTitle(stringResource(R.string.msg_message))
            MessageEditor(
                uiState = uiState,
                playback = playback,
                onMessageChange = onMessageChange,
                onToggleBold = onToggleBold,
                onToggleItalic = onToggleItalic,
                onToggleBullets = onToggleBullets,
                onOpenLink = { showLinkDialog = true },
                onOpenTemplates = { showTemplates = true },
                onAttach = onAttach,
                onRemoveAttachment = onRemoveAttachment,
                onRemoveVoice = onRemoveVoice,
            )

            Spacer(Modifier.height(16.dp))
            DeliveryOptions(
                notifyBySms = uiState.notifyBySms,
                scheduledAt = uiState.scheduledAt,
                onNotifyBySmsChange = onNotifyBySmsChange,
                onScheduleToggle = onScheduleToggle,
                onEditSchedule = onEditSchedule,
            )
            Spacer(Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = onDictate,
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(16.dp),
                color = White,
                border = BorderStroke(1.5.dp, BrandGreen),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic_outline),
                        contentDescription = stringResource(R.string.msg_dictation_desc),
                        tint = BrandGreen,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Button(
                onClick = onSend,
                enabled = uiState.canSend,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = White,
                    disabledContainerColor = BrandBlue.copy(alpha = 0.4f),
                    disabledContentColor = White,
                ),
            ) {
                Icon(painter = painterResource(R.drawable.ic_send), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (uiState.recipients.isEmpty()) {
                        stringResource(R.string.msg_send)
                    } else {
                        pluralStringResource(uiState.audience.sendTo, uiState.recipientCount, uiState.recipientCount)
                    },
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    // Two lines so longer translations of "Send to 62 parents" aren't cut off.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (showTemplates) {
        ModalBottomSheet(onDismissRequest = { showTemplates = false }, containerColor = White) {
            TemplatesSheet(
                templates = uiState.templates,
                onSelect = { template ->
                    onTemplateSelected(template)
                    showTemplates = false
                },
            )
        }
    }
    if (showLinkDialog) {
        LinkDialog(
            onDismiss = { showLinkDialog = false },
            onInsert = { url ->
                onInsertLink(url)
                showLinkDialog = false
            },
        )
    }
}

@Composable
private fun FieldTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        color = TextPrimary,
        fontSize = AppTextSize.Body,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun SendAsOption(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) SelectedRowBackground else White,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) BrandBlue else FieldBorder),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = text,
                color = if (selected) BrandBlue else TextPrimary,
                fontSize = AppTextSize.Label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipientField(
    recipients: List<RecipientGroup>,
    query: String,
    onQueryChange: (String) -> Unit,
    onRemove: (RecipientGroup) -> Unit,
    onRemoveLast: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .background(White, RoundedCornerShape(16.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) BrandBlue else FieldBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recipients.forEach { group ->
            Surface(shape = RoundedCornerShape(50), color = SelectedRowBackground) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(group.label, color = BrandBlue, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.msg_remove_recipient_desc, group.label),
                        tint = BrandBlue,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onRemove(group) }
                            .padding(2.dp),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 140.dp)
                .height(34.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.msg_to_hint), color = TextMuted, fontSize = AppTextSize.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        // Hardware / IME backspace in an empty field removes the last chip.
                        if (event.key == Key.Backspace && query.isEmpty() && recipients.isNotEmpty()) {
                            onRemoveLast()
                            true
                        } else {
                            false
                        }
                    },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = AppTextSize.Body),
                cursorBrush = SolidColor(BrandBlue),
                interactionSource = interaction,
            )
        }
    }
}

@Composable
private fun RecipientSuggestions(suggestions: List<RecipientGroup>, onSelect: (RecipientGroup) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
        shadowElevation = 2.dp,
    ) {
        Column {
            suggestions.forEachIndexed { index, group ->
                if (index > 0) HorizontalDivider(color = FieldBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(group) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(group.label, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.Body)
                    Text(
                        pluralStringResource(R.plurals.msg_members, group.memberCount, group.memberCount),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageEditor(
    uiState: ComposeMessageUiState,
    playback: VoicePlaybackController,
    onMessageChange: (TextFieldValue) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleBullets: () -> Unit,
    onOpenLink: () -> Unit,
    onOpenTemplates: () -> Unit,
    onAttach: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onRemoveVoice: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (focused) {
                    Modifier
                        .border(4.dp, BrandBlue.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(3.dp)
                } else {
                    Modifier
                },
            )
            .background(White, RoundedCornerShape(18.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) BrandBlue else FieldBorder, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenBackground, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarButton(R.drawable.ic_format_bold, stringResource(R.string.msg_format_bold), uiState.isBold, onToggleBold)
            ToolbarButton(R.drawable.ic_format_italic, stringResource(R.string.msg_format_italic), uiState.isItalic, onToggleItalic)
            ToolbarButton(R.drawable.ic_format_list, stringResource(R.string.msg_format_list), false, onToggleBullets)
            ToolbarButton(R.drawable.ic_link, stringResource(R.string.msg_format_link), false, onOpenLink)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenTemplates) {
                Text(stringResource(R.string.msg_templates), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = FieldBorder)
        uiState.voice?.let { clip ->
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VoicePlayerBar(
                    key = "compose-voice",
                    clip = clip,
                    controller = playback,
                    style = VoicePlayerStyle.FULL,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    playback.stop()
                    onRemoveVoice()
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.msg_remove_voice_desc),
                        tint = TextSecondary,
                    )
                }
            }
        }
        Box(modifier = Modifier.padding(16.dp)) {
            if (uiState.message.text.isEmpty()) {
                Text(stringResource(R.string.msg_message_hint), color = TextMuted, fontSize = AppTextSize.BodyLarge)
            }
            BasicTextField(
                value = uiState.message,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = AppTextSize.BodyLarge, lineHeight = 26.sp),
                cursorBrush = SolidColor(BrandBlue),
                interactionSource = interaction,
            )
        }
        HorizontalDivider(color = FieldBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            uiState.attachment?.let { attachment ->
                AttachmentPill(attachment = attachment, onRemove = onRemoveAttachment, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onAttach) {
                Icon(
                    painter = painterResource(R.drawable.ic_attach_file),
                    contentDescription = stringResource(R.string.msg_attach_desc),
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.msg_char_count, uiState.message.text.length, ComposeMessageUiState.MAX_MESSAGE_LENGTH),
                color = TextSecondary,
                fontSize = AppTextSize.Label,
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (active) SelectedRowBackground else ScreenBackground,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = if (active) BrandBlue else TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun AttachmentPill(attachment: FileAttachment, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = ScreenBackground) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachmentIcon(attachment.type, size = 20.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = attachment.name,
                modifier = Modifier.weight(1f, fill = false),
                color = TextPrimary,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.msg_remove_attachment_desc),
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onRemove)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun DeliveryOptions(
    notifyBySms: Boolean,
    scheduledAt: Long?,
    onNotifyBySmsChange: (Boolean) -> Unit,
    onScheduleToggle: (Boolean) -> Unit,
    onEditSchedule: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OptionRow(
                title = stringResource(R.string.msg_notify_sms),
                subtitle = stringResource(R.string.msg_notify_sms_subtitle),
                checked = notifyBySms,
                onCheckedChange = onNotifyBySmsChange,
            )
            HorizontalDivider(color = FieldBorder)
            OptionRow(
                title = stringResource(R.string.msg_schedule),
                subtitle = if (scheduledAt == null) {
                    stringResource(R.string.msg_schedule_now)
                } else {
                    stringResource(R.string.msg_schedule_at, scheduleLabel(context, scheduledAt))
                },
                checked = scheduledAt != null,
                onCheckedChange = onScheduleToggle,
                onRowClick = if (scheduledAt != null) onEditSchedule else null,
            )
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onRowClick != null) Modifier.clickable(onClick = onRowClick) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Label)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = BrandGreen,
                checkedBorderColor = BrandGreen,
                uncheckedThumbColor = White,
                uncheckedTrackColor = RadioBorder,
                uncheckedBorderColor = RadioBorder,
            ),
        )
    }
}

@Composable
private fun TemplatesSheet(templates: List<MessageTemplate>, onSelect: (MessageTemplate) -> Unit) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.msg_templates),
            color = TextPrimary,
            fontSize = AppTextSize.H5,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.msg_templates_subtitle), color = TextSecondary, fontSize = AppTextSize.Label)
        Spacer(Modifier.height(12.dp))
        templates.forEach { template ->
            Surface(
                onClick = { onSelect(template) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                color = White,
                border = BorderStroke(1.dp, FieldBorder),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(template.title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        template.body,
                        color = TextSecondary,
                        fontSize = AppTextSize.Label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkDialog(onDismiss: () -> Unit, onInsert: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf("https://") }
    val valid = Regex("""https?://[^\s]+\.[^\s]+""").matches(url.trim())
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = { Text(stringResource(R.string.msg_link_title), color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = appTextFieldColors(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onInsert(url) }, enabled = valid) {
                Text(stringResource(R.string.msg_link_insert), color = if (valid) BrandBlue else TextMuted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.msg_cancel), color = TextSecondary)
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun ComposeMessagePreview() {
    GRADit_RewampTheme {
        val body = MessageRepository.templates("Dr. Meena Iyer").first().body
        ComposeMessageContent(
            uiState = ComposeMessageUiState(
                recipients = listOf(MessageRepository.defaultRecipientGroup),
                subject = "IA 2 preparation",
                message = TextFieldValue(body),
                attachment = FileAttachment("revision_plan.pdf", 180 * 1024L, AttachmentType.PDF),
            ),
            playback = rememberVoicePlaybackController(),
            onBack = {},
            onSendAsSelected = {},
            onRecipientQueryChange = {},
            onAddRecipient = {},
            onRemoveRecipient = {},
            onRemoveLastRecipient = {},
            onSubjectChange = {},
            onMessageChange = {},
            onToggleBold = {},
            onToggleItalic = {},
            onToggleBullets = {},
            onInsertLink = {},
            onTemplateSelected = {},
            onAttach = {},
            onRemoveAttachment = {},
            onRemoveVoice = {},
            onNotifyBySmsChange = {},
            onScheduleToggle = {},
            onEditSchedule = {},
            onDictate = {},
            onSend = {},
        )
    }
}
