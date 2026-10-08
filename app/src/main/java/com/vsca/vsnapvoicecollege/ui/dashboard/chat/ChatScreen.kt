package com.vsca.vsnapvoicecollege.ui.dashboard.chat

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.IconBadge
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Avatar tint per conversation row. */
private enum class AvatarAccent(val background: Color, val tint: Color) {
    BLUE(SelectedRowBackground, BrandBlue),
    GREEN(OnboardingGreenDisc, BrandGreen),
}

private data class Conversation(
    @param:StringRes val initials: Int,
    @param:StringRes val name: Int,
    @param:StringRes val subject: Int,
    @param:StringRes val preview: Int,
    @param:StringRes val time: Int,
    val unread: Int,
    val accent: AvatarAccent,
)

private val conversations = listOf(
    Conversation(
        initials = R.string.chat_c1_initials,
        name = R.string.chat_c1_name,
        subject = R.string.chat_c1_subject,
        preview = R.string.chat_c1_preview,
        time = R.string.chat_c1_time,
        unread = 2,
        accent = AvatarAccent.BLUE,
    ),
    Conversation(
        initials = R.string.chat_c2_initials,
        name = R.string.chat_c2_name,
        subject = R.string.chat_c2_subject,
        preview = R.string.chat_c2_preview,
        time = R.string.chat_c2_time,
        unread = 1,
        accent = AvatarAccent.GREEN,
    ),
    Conversation(
        initials = R.string.chat_c3_initials,
        name = R.string.chat_c3_name,
        subject = R.string.chat_c3_subject,
        preview = R.string.chat_c3_preview,
        time = R.string.chat_c3_time,
        unread = 0,
        accent = AvatarAccent.BLUE,
    ),
    Conversation(
        initials = R.string.chat_c4_initials,
        name = R.string.chat_c4_name,
        subject = R.string.chat_c4_subject,
        preview = R.string.chat_c4_preview,
        time = R.string.chat_c4_time,
        unread = 0,
        accent = AvatarAccent.GREEN,
    ),
    Conversation(
        initials = R.string.chat_c5_initials,
        name = R.string.chat_c5_name,
        subject = R.string.chat_c5_subject,
        preview = R.string.chat_c5_preview,
        time = R.string.chat_c5_time,
        unread = 0,
        accent = AvatarAccent.BLUE,
    ),
)

/**
 * Chat tab — the messages inbox. Hardcoded content; search filters by name/subject.
 */
@Composable
fun ChatScreen(
    onNewMessage: () -> Unit = {},
    onOpenConcern: () -> Unit = {},
    onOpenConversation: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = conversations.filter { conversation ->
        query.isBlank() ||
            stringResource(conversation.name).contains(query, ignoreCase = true) ||
            stringResource(conversation.subject).contains(query, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.chat_title),
                color = TextPrimary,
                fontSize = AppTextSize.H2,
                fontWeight = FontWeight.ExtraBold,
            )
            NewMessageButton(onClick = onNewMessage)
        }

        Spacer(Modifier.height(16.dp))
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            hint = stringResource(R.string.chat_search_hint),
        )

        Spacer(Modifier.height(16.dp))
        ConcernBanner(onClick = onOpenConcern)

        Spacer(Modifier.height(8.dp))
        visible.forEachIndexed { index, conversation ->
            if (index > 0) HorizontalDivider(color = FieldBorder)
            ConversationRow(conversation = conversation, onClick = onOpenConversation)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NewMessageButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = BrandBlue,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.chat_new_desc),
                tint = White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ConcernBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AmberChipBackground,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                iconRes = R.drawable.ic_flag,
                contentDescription = stringResource(R.string.chat_flag_desc),
                size = 40.dp,
                cornerRadius = 10.dp,
                iconSize = 20.dp,
                containerColor = White,
                iconTint = AmberText,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.chat_concern_title),
                    color = TextPrimary,
                    fontSize = AppTextSize.Body,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.chat_concern_sub),
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.chat_concern_open_desc),
                tint = AmberText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(initials = stringResource(conversation.initials), accent = conversation.accent)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = stringResource(conversation.name),
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(conversation.time),
                    color = TextMuted,
                    fontSize = AppTextSize.CaptionSmall,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(conversation.subject),
                color = BrandBlue,
                fontSize = AppTextSize.Caption,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(conversation.preview),
                    modifier = Modifier.weight(1f),
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (conversation.unread > 0) {
                    Spacer(Modifier.width(8.dp))
                    UnreadBadge(conversation.unread)
                }
            }
        }
    }
}

@Composable
private fun Avatar(initials: String, accent: AvatarAccent) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(accent.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = accent.tint,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(BrandGreen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            color = White,
            fontSize = AppTextSize.CaptionSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatPreview() {
    GRADit_RewampTheme {
        ChatScreen()
    }
}
