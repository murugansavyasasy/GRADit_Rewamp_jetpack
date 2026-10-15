package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Search across received messages, with recent searches and highlighted matches.
 */
@Composable
fun MessageSearchScreen(
    onBack: () -> Unit,
    onOpenMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessageSearchViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MessageSearchContent(
        uiState = uiState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearchSubmitted,
        onRecentSelected = viewModel::onRecentSelected,
        onRemoveRecent = viewModel::onRemoveRecent,
        onClearRecents = viewModel::onClearRecents,
        onOpenMessage = { id ->
            viewModel.onSearchSubmitted()
            onOpenMessage(id)
        },
        modifier = modifier,
    )
}

@Composable
private fun MessageSearchContent(
    uiState: MessageSearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRecentSelected: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
    onOpenMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.msg_search_hint), color = TextMuted) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(22.dp),
                    )
                },
                trailingIcon = if (uiState.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.msg_search_clear_desc),
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch()
                    focusManager.clearFocus()
                }),
                colors = appTextFieldColors(),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.recentSearches.isNotEmpty()) {
                item(key = "recent-header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.msg_recent_searches),
                            modifier = Modifier.weight(1f),
                            color = TextPrimary,
                            fontSize = AppTextSize.BodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        TextButton(onClick = onClearRecents) {
                            Text(
                                stringResource(R.string.msg_recent_clear),
                                color = BrandBlue,
                                fontSize = AppTextSize.Body,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                items(uiState.recentSearches, key = { "recent-$it" }) { recent ->
                    RecentSearchRow(
                        text = recent,
                        onClick = { onRecentSelected(recent) },
                        onRemove = { onRemoveRecent(recent) },
                    )
                }
            }

            val query = uiState.query.trim()
            if (query.isNotEmpty()) {
                item(key = "results-header") {
                    Text(
                        text = if (uiState.results.isEmpty()) {
                            stringResource(R.string.msg_search_no_results, query)
                        } else {
                            pluralStringResource(R.plurals.msg_search_results, uiState.results.size, uiState.results.size, query)
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        color = TextPrimary,
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                items(uiState.results, key = { it.id }) { message ->
                    SearchResultCard(message = message, query = query, onClick = { onOpenMessage(message.id) })
                }
            }
        }
    }
}

@Composable
private fun RecentSearchRow(text: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(text = text, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge)
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.msg_recent_remove_desc, text),
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SearchResultCard(message: InboxMessage, query: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
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
                Text(
                    text = highlight(message.title, query),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        if (message.type == MessageType.VOICE) R.string.msg_type_voice else R.string.msg_type_text,
                    ),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                )
            }
        }
    }
}

/** [text] with every case-insensitive occurrence of [query] highlighted in amber. */
internal fun highlight(text: String, query: String): AnnotatedString = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    var index = 0
    while (index < text.length) {
        val match = text.indexOf(query, startIndex = index, ignoreCase = true)
        if (match < 0) {
            append(text.substring(index))
            break
        }
        append(text.substring(index, match))
        withStyle(SpanStyle(background = AmberChipBackground, color = OnboardingAmberCard)) {
            append(text.substring(match, match + query.length))
        }
        index = match + query.length
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MessageSearchPreview() {
    GRADit_RewampTheme {
        MessageSearchContent(
            uiState = MessageSearchUiState(
                query = "exam",
                recentSearches = listOf("fee", "holiday", "timetable"),
                results = MessageRepository.inbox.value.filter { it.title.contains("exam", ignoreCase = true) },
            ),
            onBack = {},
            onQueryChange = {},
            onSearch = {},
            onRecentSelected = {},
            onRemoveRecent = {},
            onClearRecents = {},
            onOpenMessage = {},
        )
    }
}
