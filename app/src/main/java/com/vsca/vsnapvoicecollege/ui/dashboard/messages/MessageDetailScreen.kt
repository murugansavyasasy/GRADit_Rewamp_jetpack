package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AttachmentType
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.renderMarkup
import com.vsca.vsnapvoicecollege.ui.components.saveToDownloads
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppDimens
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A single received message: sender, full text or voice clip, attachment, and a
 * "Got it" button that lets the college know it was seen.
 */
@Composable
fun MessageDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessageDetailViewModel = viewModel(),
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    MessageDetailContent(
        message = message,
        playback = rememberVoicePlaybackController(),
        onBack = onBack,
        onAcknowledge = viewModel::onAcknowledge,
        onMarkUnread = {
            viewModel.onMarkUnread()
            Toast.makeText(context, R.string.msg_marked_unread, Toast.LENGTH_SHORT).show()
            onBack()
        },
        onDownload = { saveToDownloads(context, it) },
        modifier = modifier,
    )
}

@Composable
private fun MessageDetailContent(
    message: InboxMessage?,
    playback: VoicePlaybackController,
    onBack: () -> Unit,
    onAcknowledge: () -> Unit,
    onMarkUnread: () -> Unit,
    onDownload: (FileAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        MessagesTopBar(
            title = stringResource(R.string.msg_detail_title),
            onBack = onBack,
            actions = { if (message != null) DetailMenu(onMarkUnread = onMarkUnread) },
        )

        if (message == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.msg_not_found),
                    color = TextSecondary,
                    fontSize = AppTextSize.Body,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SenderAvatar(
                    initials = message.sender.initials,
                    accent = message.sender.department.accent,
                    size = 56.dp,
                    fontSize = AppTextSize.BodyLarge,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    SenderLine(sender = message.sender)
                    Spacer(Modifier.height(2.dp))
                    Text(fullDateLabel(message.sentAt), color = TextSecondary, fontSize = AppTextSize.Label)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = message.title,
                color = TextPrimary,
                fontSize = AppTextSize.H3,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp,
            )
            Spacer(Modifier.height(16.dp))
            message.voice?.let { clip ->
                VoicePlayerBar(key = message.id, clip = clip, controller = playback, style = VoicePlayerStyle.FULL)
                Spacer(Modifier.height(16.dp))
            }
            if (message.body.isNotBlank()) {
                Text(
                    text = renderMarkup(message.body),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    lineHeight = 26.sp,
                )
                Spacer(Modifier.height(20.dp))
            }
            message.attachment?.let { attachment ->
                AttachmentCard(attachment = attachment, onDownload = { onDownload(attachment) })
                Spacer(Modifier.height(16.dp))
            }
            Text(
                text = stringResource(R.string.msg_sent_to, message.sentTo),
                color = TextSecondary,
                fontSize = AppTextSize.Label,
            )
            Spacer(Modifier.height(24.dp))
        }

        AcknowledgeBar(acknowledgedAt = message.acknowledgedAt, onAcknowledge = onAcknowledge)
    }
}

@Composable
private fun DetailMenu(onMarkUnread: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TopBarIconButton(
            iconRes = R.drawable.ic_more_vert,
            contentDescription = stringResource(R.string.msg_more_options),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.msg_mark_unread), color = TextPrimary) },
                onClick = {
                    expanded = false
                    onMarkUnread()
                },
            )
        }
    }
}

@Composable
private fun AttachmentCard(attachment: FileAttachment, onDownload: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(AlertBackground, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_file),
                    contentDescription = null,
                    tint = BadgeRed,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(attachment.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(
                        R.string.msg_attachment_meta,
                        stringResource(if (attachment.type == AttachmentType.PDF) R.string.msg_file_pdf else R.string.msg_file_image),
                        fileSizeLabel(attachment.sizeBytes),
                    ),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                )
            }
            Surface(
                onClick = onDownload,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = SelectedRowBackground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = stringResource(R.string.msg_download_desc, attachment.name),
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/** "Got it" until the user confirms, then the green "Seen" confirmation. */
@Composable
private fun AcknowledgeBar(acknowledgedAt: Long?, onAcknowledge: () -> Unit) {
    Column(modifier = Modifier.background(White)) {
        HorizontalDivider(color = FieldBorder)
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (acknowledgedAt == null) {
                PrimaryButton(text = stringResource(R.string.msg_got_it), onClick = onAcknowledge)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.msg_got_it_hint), color = TextSecondary, fontSize = AppTextSize.Label)
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.ButtonHeight),
                    shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
                    color = OnboardingGreenDisc,
                ) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.msg_seen),
                            color = BrandGreen,
                            fontSize = AppTextSize.BodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        R.string.msg_seen_at,
                        // %1$s = date, %2$s = time (translations order them for their grammar).
                        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(acknowledgedAt)),
                        clockLabel(acknowledgedAt),
                    ),
                    color = BrandGreen,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MessageDetailPreview() {
    GRADit_RewampTheme {
        MessageDetailContent(
            message = MessageRepository.inbox.value.first(),
            playback = rememberVoicePlaybackController(),
            onBack = {},
            onAcknowledge = {},
            onMarkUnread = {},
            onDownload = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MessageDetailSeenPreview() {
    GRADit_RewampTheme {
        MessageDetailContent(
            message = MessageRepository.inbox.value.first().copy(acknowledgedAt = System.currentTimeMillis()),
            playback = rememberVoicePlaybackController(),
            onBack = {},
            onAcknowledge = {},
            onMarkUnread = {},
            onDownload = {},
        )
    }
}
