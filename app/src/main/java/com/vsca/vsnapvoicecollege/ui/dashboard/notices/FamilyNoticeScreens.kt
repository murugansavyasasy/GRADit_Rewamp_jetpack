package com.vsca.vsnapvoicecollege.ui.dashboard.notices

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.DashedRing
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.util.Locale

/** The notice board for parents and students. */
@Composable
fun FamilyNoticeBoardScreen(
    onBack: () -> Unit,
    onOpenNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyNoticeBoardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FamilyNoticeBoardContent(uiState, onBack, viewModel::onQueryChange, viewModel::onFilter, viewModel::refresh, onOpenNotice, modifier)
}

@Composable
private fun FamilyNoticeBoardContent(
    uiState: FamilyNoticeBoardUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onFilter: (FamilyFilter) -> Unit,
    onRetry: () -> Unit,
    onOpenNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.notice_board_title),
                subtitle = stringResource(R.string.notice_unread_subtitle, uiState.unreadCount, NoticeRepository.COLLEGE),
                onBack = onBack,
            )
        }
        if (!uiState.isOnline && uiState.load != NoticeLoad.ERROR) {
            item(key = "offline") {
                OfflineBanner(
                    stringResource(R.string.notice_offline_banner, clockLabel(uiState.lastSyncedAt)),
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        item(key = "search") {
            SearchBar(
                query = uiState.query,
                onQueryChange = onQuery,
                hint = stringResource(R.string.notice_search_hint),
                containerColor = White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
            )
        }
        item(key = "chips") { FilterChips(uiState.filter, uiState.unreadCount, onFilter) }
        when {
            uiState.load == NoticeLoad.LOADING -> item(key = "loading") { SkeletonCards(3, Modifier.padding(16.dp)) }
            uiState.load == NoticeLoad.ERROR -> item(key = "error") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_wifi,
                        iconTint = BadgeRed,
                        iconBackground = AlertBackground,
                        title = stringResource(R.string.notice_error_family_title),
                        subtitle = stringResource(R.string.notice_error_body),
                    ) { RetryButton(onRetry) }
                }
            }
            uiState.isEmpty -> item(key = "empty") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_campaign,
                        iconTint = BrandBlue,
                        iconBackground = SelectedRowBackground,
                        title = stringResource(if (uiState.filter == FamilyFilter.Unread) R.string.notice_no_unread_title else R.string.notice_none_title),
                        subtitle = stringResource(if (uiState.filter == FamilyFilter.Unread) R.string.notice_no_unread_body else R.string.notice_none_body),
                    )
                }
            }
            else -> {
                if (uiState.pinned.isNotEmpty()) {
                    item(key = "pinned") {
                        Surface(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = SelectedRowBackground.copy(alpha = 0.6f),
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(Modifier.padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(R.drawable.ic_place_outline), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.notice_pinned).uppercase(Locale.getDefault()),
                                        color = BrandBlue,
                                        fontSize = AppTextSize.BodyLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp,
                                    )
                                }
                                uiState.pinned.forEach { notice ->
                                    FamilyNoticeCard(notice, inPinnedBox = true, onClick = { onOpenNotice(notice.id) })
                                }
                            }
                        }
                    }
                }
                uiState.sections.forEach { (section, notices) ->
                    item(key = "section-" + section.name) {
                        Text(
                            stringResource(section.label).uppercase(Locale.getDefault()),
                            Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp),
                            color = TextPrimary,
                            fontSize = AppTextSize.BodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                        )
                    }
                    items(notices, key = { it.id }) { notice ->
                        FamilyNoticeCard(notice, inPinnedBox = false, onClick = { onOpenNotice(notice.id) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(selected: FamilyFilter, unread: Int, onSelect: (FamilyFilter) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        familyFilterChips.forEach { filter ->
            val isSelected = filter == selected
            Surface(
                onClick = { onSelect(filter) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) BrandBlue else White,
                border = if (isSelected) null else BorderStroke(1.dp, FieldBorder),
            ) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (filter) {
                            FamilyFilter.All -> stringResource(R.string.notice_filter_all)
                            FamilyFilter.Unread -> stringResource(R.string.notice_filter_unread)
                            FamilyFilter.Pinned -> stringResource(R.string.notice_pinned)
                            is FamilyFilter.Category -> stringResource(filter.category.label)
                        },
                        color = if (isSelected) White else TextPrimary,
                        fontSize = AppTextSize.BodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (filter == FamilyFilter.Unread && unread > 0) {
                        Spacer(Modifier.width(10.dp))
                        Surface(shape = RoundedCornerShape(50), color = if (isSelected) White.copy(alpha = 0.25f) else BrandBlue) {
                            Text(unread.toString(), Modifier.padding(horizontal = 9.dp, vertical = 1.dp), color = White, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyNoticeCard(notice: Notice, inPinnedBox: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val expired = notice.status() == NoticeStatus.EXPIRED
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, if (inPinnedBox) DashedRing else FieldBorder),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NoticePills(notice, showPinned = notice.isPinnedNow(), modifier = Modifier.weight(1f)) {
                    if (expired) LabelPill(stringResource(R.string.notice_status_expired), BadgeNeutralBackground, TextSecondary)
                }
                if (!notice.isReadByMe) {
                    Spacer(Modifier.width(8.dp))
                    NewDot()
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                notice.title,
                color = TextPrimary,
                fontSize = AppTextSize.H6,
                fontWeight = if (notice.isReadByMe) FontWeight.Bold else FontWeight.ExtraBold,
                lineHeight = 25.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(previewText(notice.body), color = TextSecondary, fontSize = AppTextSize.BodyLarge, lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = FieldBorder)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorRow(notice.author, shortTimeLabel(notice.effectiveTime), Modifier.weight(1f), avatarSize = 40.dp)
                if (notice.attachments.isNotEmpty()) {
                    Icon(painterResource(R.drawable.ic_attach_file), stringResource(R.string.notice_has_attachments), tint = TextSecondary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(notice.attachments.size.toString(), color = TextSecondary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** A notice as a parent / student opens it; opening marks it read. */
@Composable
fun FamilyNoticeScreen(noticeId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val notices by NoticeRepository.notices.collectAsStateWithLifecycle()
    val notice = notices.firstOrNull { it.id == noticeId }
    val context = LocalContext.current
    LaunchedEffect(noticeId) { NoticeRepository.markRead(noticeId) }
    if (notice == null) {
        Column(modifier.fillMaxSize().background(ScreenBackground)) {
            TitledTopBar(title = stringResource(R.string.notice_detail_title), onBack = onBack)
            Text(stringResource(R.string.notice_not_found), Modifier.padding(32.dp), color = TextSecondary)
        }
        return
    }
    val expired = notice.status() == NoticeStatus.EXPIRED
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.notice_detail_title),
            onBack = onBack,
            trailing = {
                SquareIconButton(R.drawable.ic_share_nodes, stringResource(R.string.notice_share), onClick = { shareNotice(context, notice) }, tint = BrandBlue)
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            if (expired && notice.expiresAt != null) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = ExpiredBannerColor) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            expiredBannerText(
                                stringResource(R.string.notice_expired_banner, plainDate(notice.expiresAt)),
                                stringResource(R.string.notice_status_expired),
                            ),
                            color = TextPrimary,
                            fontSize = AppTextSize.Body,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }
            NoticeCard(
                notice = notice,
                time = longTimeLabel(notice.effectiveTime),
                showPinned = notice.isPinnedNow(),
                extraPill = { if (expired) LabelPill(stringResource(R.string.notice_status_expired), BadgeNeutralBackground, TextSecondary) },
            )
            AttachmentsSection(notice)
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.background(White)) {
            HorizontalDivider(color = FieldBorder)
            Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        NoticeRepository.markUnread(notice.id)
                        Toast.makeText(context, R.string.notice_marked_unread, Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                ) {
                    Icon(painterResource(R.drawable.ic_drafts_outline), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.notice_mark_unread), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Button(
                    onClick = { shareNotice(context, notice) },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    Icon(painterResource(R.drawable.ic_share_nodes), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.notice_share), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FamilyNoticeBoardPreview() {
    GRADit_RewampTheme {
        val notices = NoticeRepository.notices.value.filter { it.isVisibleTo(Role.PARENT) }
        FamilyNoticeBoardContent(
            uiState = FamilyNoticeBoardUiState(
                load = NoticeLoad.READY,
                unreadCount = notices.count { !it.isReadByMe },
                pinned = notices.filter { it.isPinnedNow() },
                sections = listOf(NoticeSection.EARLIER to notices.filterNot { it.isPinnedNow() }),
            ),
            onBack = {}, onQuery = {}, onFilter = {}, onRetry = {}, onOpenNotice = {},
        )
    }
}

/** The banner sentence with the word "Expired" (in the user's language) in bold, wherever it falls. */
private fun expiredBannerText(sentence: String, word: String) = buildAnnotatedString {
    val at = sentence.indexOf(word, ignoreCase = true)
    if (at < 0) {
        append(sentence)
    } else {
        append(sentence.substring(0, at))
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(sentence.substring(at, at + word.length)) }
        append(sentence.substring(at + word.length))
    }
}

/** Light grey fill behind the read-only expired banner. */
private val ExpiredBannerColor = Color(0xFFE9EBF1)
