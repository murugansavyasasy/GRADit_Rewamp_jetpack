package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.CategoryChip
import com.vsca.vsnapvoicecollege.ui.components.Segment
import com.vsca.vsnapvoicecollege.ui.components.SegmentedControl
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * "New message" for management: choose Text or Voice, then write a new message or
 * reuse one from history, before picking recipients in the composer.
 */
@Composable
fun NewMessageScreen(
    onBack: () -> Unit,
    onContinueToRecipients: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewMessageViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val startRecording = {
        if (!viewModel.onStartRecording()) {
            Toast.makeText(context, R.string.msg_record_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(context, R.string.msg_record_permission, Toast.LENGTH_SHORT).show()
    }

    NewMessageContent(
        uiState = uiState,
        playback = rememberVoicePlaybackController(),
        onBack = onBack,
        onTypeSelected = viewModel::onTypeSelected,
        onTabSelected = viewModel::onTabSelected,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onStartRecording = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        },
        onStopRecording = viewModel::onStopRecording,
        onDiscardRecording = viewModel::onDiscardRecording,
        onHistoryFilterSelected = viewModel::onHistoryFilterSelected,
        onSelectRecipients = {
            viewModel.onSelectRecipients()
            onContinueToRecipients()
        },
        onSendFromHistory = { message ->
            viewModel.onSendFromHistory(message)
            onContinueToRecipients()
        },
        modifier = modifier,
    )
}

@Composable
private fun NewMessageContent(
    uiState: NewMessageUiState,
    playback: VoicePlaybackController,
    onBack: () -> Unit,
    onTypeSelected: (MessageType) -> Unit,
    onTabSelected: (NewMessageTab) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    onHistoryFilterSelected: (HistoryFilter) -> Unit,
    onSelectRecipients: () -> Unit,
    onSendFromHistory: (SentMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .imePadding(),
    ) {
        MessagesTopBar(title = stringResource(R.string.msg_new_message_title), onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SegmentedControl(
                options = listOf(
                    Segment(stringResource(R.string.msg_type_text_short), R.drawable.ic_chat_bubble_outline),
                    Segment(stringResource(R.string.msg_type_voice_short), R.drawable.ic_mic_outline),
                ),
                selectedIndex = uiState.type.ordinal,
                onSelect = { onTypeSelected(MessageType.entries[it]) },
            )
            Spacer(Modifier.height(12.dp))
            SegmentedControl(
                options = listOf(
                    Segment(stringResource(R.string.msg_tab_new)),
                    Segment(stringResource(R.string.msg_tab_history)),
                ),
                selectedIndex = uiState.tab.ordinal,
                onSelect = { onTabSelected(NewMessageTab.entries[it]) },
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (uiState.tab) {
                NewMessageTab.NEW -> NewMessageForm(
                    uiState = uiState,
                    playback = playback,
                    onTitleChange = onTitleChange,
                    onDescriptionChange = onDescriptionChange,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onDiscardRecording = onDiscardRecording,
                )
                NewMessageTab.HISTORY -> HistoryList(
                    uiState = uiState,
                    playback = playback,
                    onFilterSelected = onHistoryFilterSelected,
                    onSend = onSendFromHistory,
                )
            }
        }

        Column(modifier = Modifier.background(White)) {
            HorizontalDivider(color = FieldBorder)
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                ) {
                    Text(stringResource(R.string.msg_cancel), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
                if (uiState.tab == NewMessageTab.NEW) {
                    Button(
                        onClick = onSelectRecipients,
                        enabled = uiState.canContinue,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue,
                            contentColor = White,
                            disabledContainerColor = BrandBlue.copy(alpha = 0.4f),
                            disabledContentColor = White,
                        ),
                    ) {
                        Text(stringResource(R.string.msg_select_recipients), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewMessageForm(
    uiState: NewMessageUiState,
    playback: VoicePlaybackController,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        FormLabel(stringResource(R.string.msg_field_title))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.msg_field_title_hint), color = TextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = appTextFieldColors(),
        )
        Spacer(Modifier.height(18.dp))
        if (uiState.type == MessageType.TEXT) {
            FormLabel(stringResource(R.string.msg_field_description))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                placeholder = { Text(stringResource(R.string.msg_field_description_hint), color = TextMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = appTextFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.msg_char_count, uiState.description.length, NewMessageUiState.MAX_DESCRIPTION_LENGTH),
                modifier = Modifier.fillMaxWidth(),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
                textAlign = TextAlign.End,
            )
        } else {
            FormLabel(stringResource(R.string.msg_field_voice))
            Spacer(Modifier.height(8.dp))
            VoiceRecorderCard(
                uiState = uiState,
                playback = playback,
                onStart = onStartRecording,
                onStop = onStopRecording,
                onDiscard = onDiscardRecording,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = TextPrimary,
        fontSize = AppTextSize.Body,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun VoiceRecorderCard(
    uiState: NewMessageUiState,
    playback: VoicePlaybackController,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val recording = uiState.recording
            when {
                uiState.isRecording -> {
                    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
                        initialValue = 1f,
                        targetValue = 0.3f,
                        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                        label = "rec-dot",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(pulse)
                                .clip(CircleShape)
                                .background(BadgeRed),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${formatClipTimePadded(uiState.recordingElapsedMs)} / ${formatClipTimePadded(VoiceRecorder.MAX_CLIP_MS)}",
                            color = TextPrimary,
                            fontSize = AppTextSize.H5,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    RoundIconButton(
                        iconRes = R.drawable.ic_stop,
                        contentDescription = stringResource(R.string.msg_record_stop),
                        color = BadgeRed,
                        onClick = onStop,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.msg_record_in_progress), color = TextSecondary, fontSize = AppTextSize.Label)
                }
                recording != null -> {
                    VoicePlayerBar(key = "new-recording", clip = recording, controller = playback, style = VoicePlayerStyle.FULL)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        playback.stop()
                        onDiscard()
                    }) {
                        Text(stringResource(R.string.msg_record_again), color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    RoundIconButton(
                        iconRes = R.drawable.ic_mic,
                        contentDescription = stringResource(R.string.msg_record_start),
                        color = BrandBlue,
                        onClick = onStart,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.msg_record_tap), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.msg_record_limit), color = TextSecondary, fontSize = AppTextSize.Caption)
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = Modifier.size(68.dp), shape = CircleShape, color = color) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun HistoryList(
    uiState: NewMessageUiState,
    playback: VoicePlaybackController,
    onFilterSelected: (HistoryFilter) -> Unit,
    onSend: (SentMessage) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HistoryFilter.entries.forEach { filter ->
                    CategoryChip(
                        text = stringResource(filter.label),
                        selected = filter == uiState.historyFilter,
                        onClick = { onFilterSelected(filter) },
                    )
                }
            }
        }
        if (uiState.history.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.msg_history_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    color = TextSecondary,
                    fontSize = AppTextSize.Body,
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(uiState.history, key = { it.id }) { message ->
            HistoryCard(message = message, playback = playback, onSend = { onSend(message) })
        }
    }
}

@Composable
private fun HistoryCard(message: SentMessage, playback: VoicePlaybackController, onSend: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(SelectedRowBackground, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (message.type == MessageType.VOICE) R.drawable.ic_mic_outline else R.drawable.ic_chat_bubble_outline,
                    ),
                    contentDescription = stringResource(
                        if (message.type == MessageType.VOICE) R.string.msg_type_voice else R.string.msg_type_text,
                    ),
                    tint = BrandBlue,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = message.title,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(numericDateLabel(message.sentAt), color = TextSecondary, fontSize = AppTextSize.Caption)
                        Text(
                            clockLabel(message.sentAt),
                            color = TextSecondary,
                            fontSize = AppTextSize.Caption,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val voice = message.voice
                if (voice != null) {
                    VoicePlayerBar(key = message.id, clip = voice, controller = playback, style = VoicePlayerStyle.FULL)
                } else {
                    Text(
                        text = message.body,
                        color = TextSecondary,
                        fontSize = AppTextSize.Label,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSend,
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    Text(stringResource(R.string.msg_send), fontWeight = FontWeight.Bold, fontSize = AppTextSize.BodyLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NewMessagePreview() {
    GRADit_RewampTheme {
        NewMessageContent(
            uiState = NewMessageUiState(),
            playback = rememberVoicePlaybackController(),
            onBack = {},
            onTypeSelected = {},
            onTabSelected = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onStartRecording = {},
            onStopRecording = {},
            onDiscardRecording = {},
            onHistoryFilterSelected = {},
            onSelectRecipients = {},
            onSendFromHistory = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NewMessageHistoryPreview() {
    GRADit_RewampTheme {
        NewMessageContent(
            uiState = NewMessageUiState(tab = NewMessageTab.HISTORY, history = MessageRepository.history.value),
            playback = rememberVoicePlaybackController(),
            onBack = {},
            onTypeSelected = {},
            onTabSelected = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onStartRecording = {},
            onStopRecording = {},
            onDiscardRecording = {},
            onHistoryFilterSelected = {},
            onSelectRecipients = {},
            onSendFromHistory = {},
        )
    }
}
