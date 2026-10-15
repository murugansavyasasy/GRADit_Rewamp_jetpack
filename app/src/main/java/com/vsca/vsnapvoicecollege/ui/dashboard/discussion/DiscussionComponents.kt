package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AttachmentIcon
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import com.vsca.vsnapvoicecollege.ui.components.openAttachment
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Circular initials avatar tinted by the author's accent. */
@Composable
internal fun AuthorAvatar(
    author: DiscussionAuthor,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(author.accent.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = author.initials,
            color = author.accent.content,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** "Student" / "Teacher" pill shown next to an author's name. */
@Composable
internal fun RoleBadge(role: DiscussionAuthorRole, modifier: Modifier = Modifier) {
    val (background, content) = if (role.isStaff) {
        OnboardingGreenDisc to BrandGreen
    } else {
        SelectedRowBackground to BrandBlue
    }
    Pill(
        text = stringResource(role.label),
        background = background,
        content = content,
        modifier = modifier,
    )
}

/** "✓ Answered" / "✓ Answered by …" / "◷ Unanswered" status pill. */
@Composable
internal fun StatusChip(
    post: DiscussionPost,
    modifier: Modifier = Modifier,
    showAnsweredBy: Boolean = true,
) {
    if (post.isAnswered) {
        val answeredBy = post.answeredBy.takeIf { showAnsweredBy }
        Pill(
            text = if (answeredBy != null) {
                stringResource(R.string.discussion_answered_by, answeredBy)
            } else {
                stringResource(R.string.discussion_answered)
            },
            background = OnboardingGreenDisc,
            content = BrandGreen,
            iconRes = R.drawable.ic_check,
            modifier = modifier,
        )
    } else {
        Pill(
            text = stringResource(R.string.discussion_unanswered),
            background = AmberChipBackground,
            content = AmberText,
            iconRes = R.drawable.ic_clock,
            modifier = modifier,
        )
    }
}

@Composable
internal fun Pill(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = background) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = content,
                fontSize = AppTextSize.Caption,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Heart + count; filled red once the user has liked it. */
@Composable
internal fun LikeButton(
    likes: Int,
    liked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
            contentDescription = stringResource(
                if (liked) R.string.discussion_unlike_desc else R.string.discussion_like_desc,
            ),
            tint = if (liked) BadgeRed else TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = likes.toString(),
            color = TextSecondary,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Compact file chip ("📄 Notes.pdf") shown on posts and replies; opens the file when tapped. */
@Composable
internal fun AttachmentChip(attachment: FileAttachment, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        onClick = { openAttachment(context, attachment) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ScreenBackground,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachmentIcon(attachment.type, size = 20.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = attachment.name,
                color = TextPrimary,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** "Today, 10:20 AM" / "Yesterday, 6:40 PM" / "12 Oct, 9:05 AM". */
@Composable
internal fun postedLabel(millis: Long): String {
    val time = clockLabel(millis)
    return when {
        isDaysAgo(millis, 0) -> stringResource(R.string.discussion_today, time)
        isDaysAgo(millis, 1) -> stringResource(R.string.discussion_yesterday, time)
        else -> "${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))}, $time"
    }
}

