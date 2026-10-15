package com.vsca.vsnapvoicecollege.ui.dashboard.files

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
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
 * Files the college shared with a parent's child or a student. Files open online;
 * copies are only saved to the phone when the user taps Download.
 */
@Composable
fun FamilyFilesScreen(
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyFilesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isParent = SessionPreferences.getRole(LocalContext.current) == Role.PARENT
    FamilyFilesContent(
        uiState = uiState,
        isParent = isParent,
        actions = FileListActions(
            onBack = onBack,
            onTab = viewModel::onTabSelected,
            onQuery = viewModel::onQueryChange,
            onCategory = viewModel::onCategorySelected,
            onOpenFile = onOpenFile,
        ),
        modifier = modifier,
    )
}

@Composable
private fun FamilyFilesContent(
    uiState: FilesUiState,
    isParent: Boolean,
    actions: FileListActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(ScreenBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.files_title),
                subtitle = if (isParent) {
                    stringResource(R.string.files_for_child, FileRepository.CHILD_NAME)
                } else {
                    stringResource(R.string.files_view_online)
                },
                onBack = actions.onBack,
            )
        }
        if (!uiState.isOnline) {
            item(key = "offline") {
                OfflineBanner(
                    text = stringResource(
                        R.string.files_offline_family,
                        "${longDateLabel(uiState.lastSyncedAt).substringBeforeLast(' ')}, ${clockLabel(uiState.lastSyncedAt)}",
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        item(key = "tabs") { FileTypeTabs(uiState.options.tab, uiState.counts, actions.onTab) }
        item(key = "search") {
            FilesSearch(uiState.options.query, stringResource(R.string.files_search_family_hint), actions.onQuery)
        }
        item(key = "category") {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                CategoryFilter(uiState.options.category, actions.onCategory)
            }
        }
        when {
            uiState.loading -> {
                item(key = "loading") { SkeletonCards(2, Modifier.padding(horizontal = 16.dp)) }
                item(key = "note") { OnlineOnlyNote() }
                item(key = "powered") { PoweredByRow(Modifier.fillMaxWidth().padding(vertical = 16.dp), textColor = TextSecondary) }
            }
            uiState.totalCount == 0 -> item(key = "empty") {
                Text(
                    text = stringResource(R.string.files_empty_family),
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    color = TextSecondary,
                    fontSize = AppTextSize.Body,
                )
            }
            uiState.files.isEmpty() -> item(key = "none") { NoMatches() }
            else -> itemsIndexed(uiState.files, key = { _, file -> file.id }) { index, file ->
                ListCardSegment(index = index, last = index == uiState.files.lastIndex) {
                    FamilyFileRow(file, onClick = { actions.onOpenFile(file.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FamilyFileRow(file: SharedFile, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.Top) {
        FileTile(file.format, size = 68.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = displayName(file),
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (file.isNew) {
                    Spacer(Modifier.width(8.dp))
                    LabelPill(stringResource(R.string.files_new), BrandBlue, White)
                }
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TypeChip(file.format)
                Text(
                    text = stringResource(R.string.files_meta, fileSizeLabel(file.sizeBytes), shortDateLabel(file.publishedAt)),
                    modifier = Modifier.align(Alignment.CenterVertically),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.files_meta, file.sender, stringResource(file.category.label)),
                color = TextSecondary,
                fontSize = AppTextSize.Body,
            )
        }
    }
    HorizontalDivider(color = FieldBorder)
}

@Composable
private fun OnlineOnlyNote() {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = TextMuted, fontSize = AppTextSize.Caption)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.files_online_note), color = TextSecondary, fontSize = AppTextSize.Body, lineHeight = 20.sp)
    }
}

// ---- Videos (Video quick action for parents and students) ----

/** Recorded class videos with a search box. */
@Composable
fun LessonVideosScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val needle = query.trim()
    val videos = FileRepository.lessonVideos.filter {
        needle.isEmpty() || it.title.contains(needle, true) || it.subject.contains(needle, true) ||
            it.teacher?.contains(needle, true) == true
    }
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(title = stringResource(R.string.files_title), onBack = onBack)
        FilesSearch(query, stringResource(R.string.files_search_short_hint)) { query = it }
        if (videos.isEmpty()) {
            NoMatches()
            return@Column
        }
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = White,
            border = BorderStroke(1.dp, FieldBorder),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                videos.forEachIndexed { index, video ->
                    if (index > 0) HorizontalDivider(color = FieldBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // TODO: stream lesson videos once the media server is connected.
                                android.widget.Toast.makeText(context, R.string.files_video_not_bundled, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(56.dp).background(SelectedRowBackground, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_videocam_outline), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(video.title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = listOfNotNull(video.subject, video.teacher, durationLabel(video.durationMs).trimStart('0'))
                                    .joinToString(" · "),
                                color = TextSecondary,
                                fontSize = AppTextSize.Label,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(dayMonthLabel(video.postedAt), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FamilyFilesPreview() {
    GRADit_RewampTheme {
        val files = FileRepository.files.value.filter { it.isVisibleToFamily }
        FamilyFilesContent(
            uiState = FilesUiState(
                loading = false,
                totalCount = files.size,
                counts = FileTypeTab.entries.associateWith { tab -> files.count(tab::matches) },
                files = FileSort.NEWEST.apply(files),
            ),
            isParent = true,
            actions = FileListActions({}, {}, {}, onCategory = {}, onOpenFile = {}),
        )
    }
}
