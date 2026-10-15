package com.vsca.vsnapvoicecollege.ui.dashboard.discussion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.vsca.vsnapvoicecollege.ui.components.CategoryChip
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.dashedBorder
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.DashedRing
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Discussion board: searchable, filterable list of doubts / posts with a "New post"
 * action, or the "No discussions yet" state when the board is empty. Shared by every
 * role — the role-specific bottom bar is provided by the app shell.
 */
@Composable
fun DiscussionBoardScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    onNewPost: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscussionBoardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiscussionBoardContent(
        uiState = uiState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onFilterSelected = viewModel::onFilterSelected,
        onOpenPost = onOpenPost,
        onToggleLike = viewModel::onToggleLike,
        onNewPost = onNewPost,
        modifier = modifier,
    )
}

@Composable
private fun DiscussionBoardContent(
    uiState: DiscussionBoardUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (DiscussionFilter) -> Unit,
    onOpenPost: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onNewPost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.discussion_title),
                    color = TextPrimary,
                    fontSize = AppTextSize.H4,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            if (uiState.hasAnyPosts) {
                Spacer(Modifier.height(16.dp))
                SearchBar(
                    query = uiState.query,
                    onQueryChange = onQueryChange,
                    hint = stringResource(R.string.discussion_search_hint),
                    containerColor = White,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DiscussionFilter.entries.forEach { filter ->
                        CategoryChip(
                            text = stringResource(filter.label),
                            selected = filter == uiState.filter,
                            onClick = { onFilterSelected(filter) },
                        )
                    }
                }
                PostList(
                    posts = uiState.posts,
                    onOpenPost = onOpenPost,
                    onToggleLike = onToggleLike,
                    modifier = Modifier.weight(1f),
                )
            } else {
                EmptyBoard(onNewPost = onNewPost, modifier = Modifier.weight(1f))
            }
        }

        if (uiState.hasAnyPosts) {
            ExtendedFloatingActionButton(
                onClick = onNewPost,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(50),
                containerColor = BrandBlue,
                contentColor = White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.discussion_new_post),
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
    }
}

@Composable
private fun PostList(
    posts: List<DiscussionPost>,
    onOpenPost: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (posts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = stringResource(R.string.discussion_no_results),
                color = TextSecondary,
                fontSize = AppTextSize.Body,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        // Bottom padding keeps the last card clear of the "New post" button.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onClick = { onOpenPost(post.id) },
                onToggleLike = { onToggleLike(post.id) },
            )
        }
    }
}

@Composable
private fun PostCard(
    post: DiscussionPost,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(author = post.author, size = 42.dp, fontSize = AppTextSize.Label)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.author.name,
                            modifier = Modifier.weight(1f, fill = false),
                            color = TextPrimary,
                            fontSize = AppTextSize.Body,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        RoleBadge(post.author.role)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.discussion_meta, post.subject, postedLabel(post.postedAt)),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = post.title,
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (post.body.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = post.body,
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            StatusChip(post)
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = FieldBorder)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat_bubble_outline),
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.discussion_reply_count,
                        post.replies.size,
                        post.replies.size,
                    ),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(20.dp))
                LikeButton(likes = post.likes, liked = post.likedByMe, onClick = onToggleLike)
            }
        }
    }
}

@Composable
private fun EmptyBoard(onNewPost: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 150.dp)
                .background(SelectedRowBackground, RoundedCornerShape(24.dp))
                .dashedBorder(color = DashedRing, cornerRadius = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_forum),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.discussion_empty_title),
            color = TextPrimary,
            fontSize = AppTextSize.H4,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.discussion_empty_subtitle),
            color = TextSecondary,
            fontSize = AppTextSize.Body,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onNewPost,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.discussion_new_post),
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscussionBoardPreview() {
    GRADit_RewampTheme {
        DiscussionBoardContent(
            uiState = DiscussionBoardUiState(
                posts = DiscussionRepository.posts.value,
                hasAnyPosts = true,
            ),
            onBack = {},
            onQueryChange = {},
            onFilterSelected = {},
            onOpenPost = {},
            onToggleLike = {},
            onNewPost = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscussionBoardEmptyPreview() {
    GRADit_RewampTheme {
        DiscussionBoardContent(
            uiState = DiscussionBoardUiState(),
            onBack = {},
            onQueryChange = {},
            onFilterSelected = {},
            onOpenPost = {},
            onToggleLike = {},
            onNewPost = {},
        )
    }
}
