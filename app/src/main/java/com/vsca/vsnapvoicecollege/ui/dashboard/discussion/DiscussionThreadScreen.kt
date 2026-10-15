package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.IMAGE_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.PDF_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.dashedBorder
import com.vsca.vsnapvoicecollege.ui.components.readAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.DashedRing
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenRing
import com.vsca.vsnapvoicecollege.ui.theme.RadioBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SearchFieldBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Widest a reply bubble grows before wrapping. */
private val MaxBubbleWidth = 300.dp

/**
 * A single discussion: the pinned question, its replies (best answer highlighted,
 * the user's own replies on the right) and a reply composer. Unanswered threads
 * show a "waiting for a teacher" banner instead of replies.
 */
@Composable
fun DiscussionThreadScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscussionThreadViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiscussionThreadContent(
        uiState = uiState,
        onBack = onBack,
        onDraftChange = viewModel::onDraftChange,
        onAttachmentPicked = viewModel::onAttachmentPicked,
        onRemoveAttachment = viewModel::onRemoveAttachment,
        onSend = viewModel::onSend,
        onToggleReplyLike = viewModel::onToggleReplyLike,
        modifier = modifier,
    )
}

@Composable
private fun DiscussionThreadContent(
    uiState: DiscussionThreadUiState,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onAttachmentPicked: (FileAttachment) -> Unit,
    onRemoveAttachment: () -> Unit,
    onSend: () -> Unit,
    onToggleReplyLike: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val post = uiState.post
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .imePadding(),
    ) {
        ThreadHeader(post = post, onBack = onBack)
        HorizontalDivider(color = FieldBorder)

        if (post == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.discussion_not_found),
                    color = TextSecondary,
                    fontSize = AppTextSize.Body,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        val listState = rememberLazyListState()
        // Scroll to a reply the user just sent (but not when the thread first opens).
        var shownReplies by remember { mutableIntStateOf(post.replies.size) }
        LaunchedEffect(post.replies.size) {
            if (post.replies.size > shownReplies) {
                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
            }
            shownReplies = post.replies.size
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "question") { PinnedQuestionCard(post) }
            if (post.replies.isEmpty()) {
                if (!post.isAnswered) {
                    item(key = "waiting") { WaitingBanner() }
                }
                item(key = "no_replies") { NoRepliesCard() }
            } else {
                item(key = "replies_divider") { RepliesDivider(post.replies.size) }
                items(post.replies, key = { it.id }) { reply ->
                    if (reply.isMine) {
                        MyReply(reply = reply, onToggleLike = { onToggleReplyLike(reply.id) })
                    } else {
                        OtherReply(reply = reply, onToggleLike = { onToggleReplyLike(reply.id) })
                    }
                }
            }
        }

        ReplyComposer(
            draft = uiState.draft,
            attachment = uiState.draftAttachment,
            canSend = uiState.canSend,
            onDraftChange = onDraftChange,
            onAttachmentPicked = onAttachmentPicked,
            onRemoveAttachment = onRemoveAttachment,
            onSend = onSend,
        )
    }
}

@Composable
private fun ThreadHeader(post: DiscussionPost?, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post?.title ?: stringResource(R.string.discussion_title),
                color = TextPrimary,
                fontSize = AppTextSize.H5,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 24.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (post != null) {
                Text(
                    text = post.subject,
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (post != null) {
            Spacer(Modifier.width(8.dp))
            StatusChip(post = post, showAnsweredBy = false)
        }
    }
}

@Composable
private fun PinnedQuestionCard(post: DiscussionPost) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = White,
        border = BorderStroke(1.dp, DashedRing),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_push_pin),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.discussion_pinned_question),
                    color = BrandBlue,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(author = post.author, size = 42.dp, fontSize = AppTextSize.Label)
                Spacer(Modifier.width(12.dp))
                Column {
                    AuthorName(author = post.author)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = postedLabel(post.postedAt),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                }
            }
            if (post.body.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = post.body,
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    lineHeight = 24.sp,
                )
            }
            post.attachment?.let { attachment ->
                Spacer(Modifier.height(14.dp))
                AttachmentChip(attachment)
            }
        }
    }
}

@Composable
private fun AuthorName(author: DiscussionAuthor) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = author.name,
            color = TextPrimary,
            fontSize = AppTextSize.Body,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        RoleBadge(author.role)
    }
}

@Composable
private fun RepliesDivider(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
        Text(
            text = pluralStringResource(R.plurals.discussion_reply_count, count, count),
            modifier = Modifier.padding(horizontal = 12.dp),
            color = TextSecondary,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
    }
}

/** A reply from someone else: author row, then a white (or green "best answer") bubble. */
@Composable
private fun OtherReply(reply: DiscussionReply, onToggleLike: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuthorAvatar(author = reply.author, size = 30.dp, fontSize = AppTextSize.CaptionSmall)
            Spacer(Modifier.width(8.dp))
            AuthorName(author = reply.author)
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.widthIn(max = MaxBubbleWidth),
            shape = RoundedCornerShape(20.dp),
            color = if (reply.isBestAnswer) OnboardingGreenDisc else White,
            border = BorderStroke(1.dp, if (reply.isBestAnswer) OnboardingGreenRing else FieldBorder),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                if (reply.isBestAnswer) {
                    Pill(
                        text = stringResource(R.string.discussion_best_answer),
                        background = BrandGreen,
                        content = White,
                        iconRes = R.drawable.ic_check,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                ReplyBody(reply = reply, textColor = TextPrimary)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = clockLabel(reply.postedAt), color = TextSecondary, fontSize = AppTextSize.Caption)
            Spacer(Modifier.width(12.dp))
            LikeButton(likes = reply.likes, liked = reply.likedByMe, onClick = onToggleLike)
        }
    }
}

/** The user's own reply: a brand-blue bubble aligned to the right. */
@Composable
private fun MyReply(reply: DiscussionReply, onToggleLike: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = MaxBubbleWidth),
            shape = RoundedCornerShape(20.dp),
            color = BrandBlue,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                ReplyBody(reply = reply, textColor = White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.discussion_you_meta, clockLabel(reply.postedAt)),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
            )
            Spacer(Modifier.width(12.dp))
            LikeButton(likes = reply.likes, liked = reply.likedByMe, onClick = onToggleLike)
        }
    }
}

@Composable
private fun ReplyBody(reply: DiscussionReply, textColor: Color) {
    if (reply.text.isNotBlank()) {
        Text(
            text = reply.text,
            color = textColor,
            fontSize = AppTextSize.BodyLarge,
            lineHeight = 24.sp,
        )
    }
    reply.attachment?.let { attachment ->
        if (reply.text.isNotBlank()) Spacer(Modifier.height(10.dp))
        AttachmentChip(attachment)
    }
}

@Composable
private fun WaitingBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AmberChipBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                tint = OnboardingAmberCard,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.discussion_waiting_banner),
                color = OnboardingAmberCard,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun NoRepliesCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(color = RadioBorder, cornerRadius = 20.dp, strokeWidth = 1.dp)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chat_bubble_outline),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.discussion_no_replies_title),
            color = TextPrimary,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.discussion_no_replies_subtitle),
            color = TextSecondary,
            fontSize = AppTextSize.Label,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReplyComposer(
    draft: String,
    attachment: FileAttachment?,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onAttachmentPicked: (FileAttachment) -> Unit,
    onRemoveAttachment: () -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readAttachment(context, it) }?.let(onAttachmentPicked)
    }

    Surface(color = White) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = FieldBorder)
            if (attachment != null) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AttachmentChip(attachment = attachment, modifier = Modifier.weight(1f, fill = false))
                    IconButton(onClick = onRemoveAttachment) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.discussion_remove_attachment_desc),
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { picker.launch(IMAGE_MIME_TYPES + PDF_MIME_TYPES) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_attach_file),
                        contentDescription = stringResource(R.string.discussion_attach_desc),
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                TextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.discussion_reply_hint), color = TextMuted) },
                    maxLines = 4,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SearchFieldBackground,
                        unfocusedContainerColor = SearchFieldBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BrandBlue,
                    ),
                )
                Spacer(Modifier.width(10.dp))
                Surface(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = if (canSend) BrandBlue else BrandBlue.copy(alpha = 0.5f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = stringResource(R.string.discussion_send_desc),
                            tint = White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscussionThreadAnsweredPreview() {
    GRADit_RewampTheme {
        DiscussionThreadContent(
            uiState = DiscussionThreadUiState(post = DiscussionRepository.posts.value.first()),
            onBack = {},
            onDraftChange = {},
            onAttachmentPicked = {},
            onRemoveAttachment = {},
            onSend = {},
            onToggleReplyLike = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscussionThreadUnansweredPreview() {
    GRADit_RewampTheme {
        DiscussionThreadContent(
            uiState = DiscussionThreadUiState(post = DiscussionRepository.posts.value.last()),
            onBack = {},
            onDraftChange = {},
            onAttachmentPicked = {},
            onRemoveAttachment = {},
            onSend = {},
            onToggleReplyLike = {},
        )
    }
}
