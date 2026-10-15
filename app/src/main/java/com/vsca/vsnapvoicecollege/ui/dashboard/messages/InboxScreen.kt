package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.dashedBorder
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.DashedRing
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Read-only list of messages the college sent to a parent / student, with
 * All / Unread / Text / Voice filters and (for parents) a child switcher.
 */
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playback = rememberVoicePlaybackController()

    InboxContent(
        uiState = uiState,
        playback = playback,
        onBack = onBack,
        onOpenSearch = onOpenSearch,
        onOpenMessage = onOpenMessage,
        onFilterSelected = viewModel::onFilterSelected,
        onChildSelected = viewModel::onChildSelected,
        onMarkAllRead = viewModel::onMarkAllRead,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun InboxContent(
    uiState: InboxUiState,
    playback: VoicePlaybackController,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMessage: (String) -> Unit,
    onFilterSelected: (InboxFilter) -> Unit,
    onChildSelected: (LinkedChild) -> Unit,
    onMarkAllRead: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        MessagesTopBar(
            title = stringResource(uiState.filter.screenTitle),
            onBack = onBack,
            actions = {
                TopBarIconButton(
                    iconRes = R.drawable.ic_search,
                    contentDescription = stringResource(R.string.msg_search_desc),
                    onClick = onOpenSearch,
                )
                OverflowMenu(onMarkAllRead = onMarkAllRead)
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (uiState.showChildSelector && uiState.selectedChild != null) {
                item(key = "child") {
                    ChildSelector(
                        selected = uiState.selectedChild,
                        children = uiState.children,
                        onSelect = onChildSelected,
                    )
                }
            }
            item(key = "banner") { InfoBanner(stringResource(R.string.msg_read_only_banner)) }
            item(key = "filters") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    InboxFilter.entries.forEach { filter ->
                        CountChip(
                            text = stringResource(filter.label),
                            count = uiState.counts[filter] ?: 0,
                            selected = filter == uiState.filter,
                            onClick = { onFilterSelected(filter) },
                        )
                    }
                }
            }

            when (uiState.loadState) {
                LoadState.LOADING -> item(key = "loading") { LoadingSkeleton() }
                LoadState.ERROR -> item(key = "error") { LoadError(onRetry = onRetry, onGoBack = onBack) }
                LoadState.READY -> if (uiState.sections.isEmpty()) {
                    item(key = "empty") { EmptyInbox(filter = uiState.filter) }
                } else {
                    uiState.sections.forEach { (section, messages) ->
                        item(key = "section-${section.name}") {
                            SectionLabel(stringResource(section.label), modifier = Modifier.padding(top = 6.dp))
                        }
                        items(messages, key = { it.id }) { message ->
                            InboxMessageCard(
                                message = message,
                                playback = playback,
                                onClick = { onOpenMessage(message.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(onMarkAllRead: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TopBarIconButton(
            iconRes = R.drawable.ic_more_vert,
            contentDescription = stringResource(R.string.msg_more_options),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.msg_mark_all_read), color = TextPrimary) },
                onClick = {
                    expanded = false
                    onMarkAllRead()
                },
            )
        }
    }
}

@Composable
private fun ChildSelector(
    selected: LinkedChild,
    children: List<LinkedChild>,
    onSelect: (LinkedChild) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            color = White,
            border = BorderStroke(1.dp, FieldBorder),
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SenderAvatar(
                    initials = selected.initials,
                    accent = MessageAccent.BLUE,
                    size = 36.dp,
                    fontSize = AppTextSize.Caption,
                )
                Spacer(Modifier.width(10.dp))
                Text(selected.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(selected.course, color = TextSecondary, fontSize = AppTextSize.Label)
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = stringResource(R.string.msg_switch_child_desc),
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            children.forEach { child ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(child.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(child.course, color = TextSecondary, fontSize = AppTextSize.Caption)
                        }
                    },
                    leadingIcon = {
                        SenderAvatar(child.initials, MessageAccent.BLUE, size = 32.dp, fontSize = AppTextSize.CaptionSmall)
                    },
                    onClick = {
                        expanded = false
                        onSelect(child)
                    },
                )
            }
        }
    }
}

@Composable
private fun InboxMessageCard(
    message: InboxMessage,
    playback: VoicePlaybackController,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = if (message.isRead) BorderStroke(1.dp, FieldBorder) else BorderStroke(1.5.dp, DashedRing),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            SenderAvatar(
                initials = message.sender.initials,
                accent = message.sender.department.accent,
                size = 46.dp,
                fontSize = AppTextSize.Label,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SenderLine(sender = message.sender, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(listTimeLabel(message.sentAt), color = TextSecondary, fontSize = AppTextSize.Caption)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MessageTypeIcon(message.type)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = message.title,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = if (message.isRead) FontWeight.SemiBold else FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!message.isRead) {
                        Spacer(Modifier.width(8.dp))
                        NewMarker()
                    }
                }
                Spacer(Modifier.height(6.dp))
                val voice = message.voice
                if (message.type == MessageType.VOICE && voice != null) {
                    VoicePlayerBar(key = message.id, clip = voice, controller = playback)
                } else {
                    Text(
                        text = message.summary,
                        color = TextSecondary,
                        fontSize = AppTextSize.Label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MessageTypeIcon(type: MessageType) {
    Icon(
        painter = painterResource(
            if (type == MessageType.VOICE) R.drawable.ic_mic_outline else R.drawable.ic_chat_bubble_outline,
        ),
        contentDescription = stringResource(
            if (type == MessageType.VOICE) R.string.msg_type_voice else R.string.msg_type_text,
        ),
        tint = BrandBlue,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun EmptyInbox(filter: InboxFilter) {
    if (filter == InboxFilter.UNREAD) {
        StateMessage(
            iconRes = R.drawable.ic_check,
            iconTint = BrandGreen,
            iconBackground = OnboardingGreenDisc,
            title = stringResource(R.string.msg_caught_up_title),
            subtitle = stringResource(R.string.msg_caught_up_subtitle),
            modifier = Modifier.padding(top = 48.dp),
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 180.dp, height = 135.dp)
                .background(SelectedRowBackground, RoundedCornerShape(24.dp))
                .dashedBorder(color = DashedRing, cornerRadius = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chat_bubble_outline),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.msg_empty_title),
            color = TextPrimary,
            fontSize = AppTextSize.H4,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.msg_empty_subtitle),
            color = TextSecondary,
            fontSize = AppTextSize.Body,
        )
    }
}

@Composable
private fun LoadError(onRetry: () -> Unit, onGoBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        StateMessage(
            iconRes = R.drawable.ic_error,
            iconTint = BadgeRed,
            iconBackground = AlertBackground,
            title = stringResource(R.string.msg_error_title),
            subtitle = stringResource(R.string.msg_error_subtitle),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.msg_retry), fontWeight = FontWeight.Bold, fontSize = AppTextSize.BodyLarge)
                }
                OutlinedButton(
                    onClick = onGoBack,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, BrandBlue),
                ) {
                    Text(
                        stringResource(R.string.msg_go_back),
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = AppTextSize.BodyLarge,
                    )
                }
            }
        }
    }
}

/** Pulsing placeholder cards shown while messages load. */
@Composable
private fun LoadingSkeleton() {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Column(
        modifier = Modifier.alpha(pulse),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBar(width = 0.18f, height = 14)
        repeat(SKELETON_CARDS) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = White,
                border = BorderStroke(1.dp, FieldBorder),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(FieldBorder),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SkeletonBar(width = 0.45f, height = 12)
                        SkeletonBar(width = 0.8f, height = 14)
                        SkeletonBar(width = 0.6f, height = 12)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonBar(width: Float, height: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth(width)
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(FieldBorder),
    )
}

private const val SKELETON_CARDS = 4

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InboxPreview() {
    GRADit_RewampTheme {
        val messages = MessageRepository.inbox.value.filter { it.childId == "aarav" }
        InboxContent(
            uiState = InboxUiState(
                loadState = LoadState.READY,
                children = MessageRepository.children,
                selectedChild = MessageRepository.children.first(),
                counts = InboxFilter.entries.associateWith { f -> messages.count(f::matches) },
                sections = InboxSection.entries.mapNotNull { s ->
                    messages.filter { sectionFor(it.sentAt) == s }.takeIf { it.isNotEmpty() }?.let { s to it }
                },
            ),
            playback = rememberVoicePlaybackController(),
            onBack = {},
            onOpenSearch = {},
            onOpenMessage = {},
            onFilterSelected = {},
            onChildSelected = {},
            onMarkAllRead = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InboxLoadingPreview() {
    GRADit_RewampTheme {
        InboxContent(
            uiState = InboxUiState(loadState = LoadState.LOADING),
            playback = rememberVoicePlaybackController(),
            onBack = {},
            onOpenSearch = {},
            onOpenMessage = {},
            onFilterSelected = {},
            onChildSelected = {},
            onMarkAllRead = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InboxErrorPreview() {
    GRADit_RewampTheme {
        InboxContent(
            uiState = InboxUiState(loadState = LoadState.ERROR),
            playback = rememberVoicePlaybackController(),
            onBack = {},
            onOpenSearch = {},
            onOpenMessage = {},
            onFilterSelected = {},
            onChildSelected = {},
            onMarkAllRead = {},
            onRetry = {},
        )
    }
}
