package com.vsca.vsnapvoicecollege.ui.dashboard.files

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.draw.clip
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
import com.vsca.vsnapvoicecollege.ui.components.FilterDropdown
import com.vsca.vsnapvoicecollege.ui.components.IconLine
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Files shared by the college, as management / faculty see them: type tabs, search,
 * folder / category / audience filters, sort, list or grid, and an upload button.
 */
@Composable
fun FilesScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    onUpload: () -> Unit,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FilesContent(
        uiState = uiState,
        actions = FileListActions(
            onBack = onBack,
            onTab = viewModel::onTabSelected,
            onQuery = viewModel::onQueryChange,
            onFolder = viewModel::onFolderSelected,
            onCategory = viewModel::onCategorySelected,
            onAudience = viewModel::onAudienceSelected,
            onSort = viewModel::onSortSelected,
            onLayout = viewModel::onLayoutChange,
            onOpenFile = onOpenFile,
        ),
        onOpenFolders = onOpenFolders,
        onUpload = onUpload,
        modifier = modifier,
    )
}

/** Callbacks shared by the staff and family file lists. */
internal class FileListActions(
    val onBack: () -> Unit,
    val onTab: (FileTypeTab) -> Unit,
    val onQuery: (String) -> Unit,
    val onFolder: (String?) -> Unit = {},
    val onCategory: (FileCategory?) -> Unit,
    val onAudience: (AudienceRole?) -> Unit = {},
    val onSort: (FileSort) -> Unit = {},
    val onLayout: (Boolean) -> Unit = {},
    val onOpenFile: (String) -> Unit,
)

@Composable
private fun FilesContent(
    uiState: FilesUiState,
    actions: FileListActions,
    onOpenFolders: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val empty = !uiState.loading && uiState.totalCount == 0
    Box(modifier = modifier.fillMaxSize().background(ScreenBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "header") {
                TitledTopBar(
                    title = stringResource(R.string.files_title),
                    subtitle = if (empty) {
                        stringResource(R.string.files_nothing_shared)
                    } else {
                        pluralStringResource(R.plurals.files_count, uiState.totalCount, uiState.totalCount)
                    },
                    onBack = actions.onBack,
                    trailing = {
                        SquareIconButton(R.drawable.ic_folder_outline, stringResource(R.string.files_folders_title), onOpenFolders)
                    },
                )
            }
            if (!uiState.isOnline) {
                item(key = "offline") {
                    OfflineBanner(
                        text = stringResource(R.string.files_offline_staff, clockLabel(uiState.lastSyncedAt)),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
            if (empty) {
                item(key = "empty") { EmptyFiles(onUpload) }
                return@LazyColumn
            }
            item(key = "tabs") { FileTypeTabs(uiState.options.tab, uiState.counts, actions.onTab) }
            item(key = "search") {
                FilesSearch(
                    query = uiState.options.query,
                    hint = stringResource(R.string.files_search_staff_hint),
                    onQueryChange = actions.onQuery,
                )
            }
            item(key = "filters") {
                ChipRow(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
                    FilterDropdown(
                        label = stringResource(R.string.files_filter_folder),
                        value = when (val id = uiState.options.folderId) {
                            null -> stringResource(R.string.files_filter_all)
                            UNSORTED_FOLDER_ID -> stringResource(R.string.files_unsorted)
                            else -> uiState.folders.firstOrNull { it.id == id }?.name ?: stringResource(R.string.files_filter_all)
                        },
                        options = listOf<Pair<String, () -> Unit>>(stringResource(R.string.files_filter_all) to { actions.onFolder(null) }) +
                            uiState.folders.map { folder -> folder.name to { actions.onFolder(folder.id) } } +
                            (stringResource(R.string.files_unsorted) to { actions.onFolder(UNSORTED_FOLDER_ID) }),
                    )
                    CategoryFilter(uiState.options.category, actions.onCategory)
                    FilterDropdown(
                        label = stringResource(R.string.files_filter_audience),
                        value = uiState.options.audience?.let { stringResource(it.label) } ?: stringResource(R.string.files_filter_all),
                        options = listOf<Pair<String, () -> Unit>>(stringResource(R.string.files_filter_all) to { actions.onAudience(null) }) +
                            AudienceRole.entries.map { role -> stringResource(role.label) to { actions.onAudience(role) } },
                    )
                }
            }
            item(key = "toolbar") {
                ListToolbar(
                    count = uiState.files.size,
                    sort = uiState.options.sort,
                    grid = uiState.options.gridView,
                    onSort = actions.onSort,
                    onLayout = actions.onLayout,
                )
            }
            when {
                uiState.loading -> item(key = "loading") { SkeletonCards(3, Modifier.padding(horizontal = 16.dp)) }
                uiState.files.isEmpty() -> item(key = "none") { NoMatches() }
                uiState.options.gridView -> {
                    val rows = uiState.files.chunked(2)
                    itemsIndexed(rows, key = { _, row -> "grid-" + row.first().id }) { _, row ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { file ->
                                StaffGridCard(file, onClick = { actions.onOpenFile(file.id) }, modifier = Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                else -> itemsIndexed(uiState.files, key = { _, file -> file.id }) { index, file ->
                    ListCardSegment(index = index, last = index == uiState.files.lastIndex) {
                        StaffListRow(file, onClick = { actions.onOpenFile(file.id) })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onUpload,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(68.dp),
            shape = CircleShape,
            containerColor = BrandBlue,
            contentColor = White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.files_upload), modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
internal fun FilesSearch(query: String, hint: String, onQueryChange: (String) -> Unit) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        hint = hint,
        containerColor = White,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
    )
}

@Composable
internal fun CategoryFilter(selected: FileCategory?, onSelect: (FileCategory?) -> Unit) {
    FilterDropdown(
        label = stringResource(R.string.files_filter_category),
        value = selected?.let { stringResource(it.label) } ?: stringResource(R.string.files_filter_all),
        options = listOf<Pair<String, () -> Unit>>(stringResource(R.string.files_filter_all) to { onSelect(null) }) +
            FileCategory.entries.map { category -> stringResource(category.label) to { onSelect(category) } },
    )
}

@Composable
private fun ListToolbar(
    count: Int,
    sort: FileSort,
    grid: Boolean,
    onSort: (FileSort) -> Unit,
    onLayout: (Boolean) -> Unit,
) {
    var sortOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.files_count, count, count),
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Box {
            Surface(
                onClick = { sortOpen = true },
                shape = RoundedCornerShape(50),
                color = White,
                border = BorderStroke(1.dp, FieldBorder),
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(sort.label), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(10.dp))
                    Icon(painterResource(R.drawable.ic_sort), stringResource(R.string.files_sort_desc), tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
            }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                FileSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.label), color = if (option == sort) BrandBlue else TextPrimary) },
                        onClick = {
                            sortOpen = false
                            onSort(option)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = BadgeNeutralBackground) {
            Row(modifier = Modifier.padding(3.dp)) {
                LayoutToggle(R.drawable.ic_grid_view, stringResource(R.string.files_grid_view), selected = grid) { onLayout(true) }
                LayoutToggle(R.drawable.ic_view_list, stringResource(R.string.files_list_view), selected = !grid) { onLayout(false) }
            }
        }
    }
}

@Composable
private fun LayoutToggle(iconRes: Int, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 44.dp, height = 40.dp),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) White else BadgeNeutralBackground,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(iconRes), description, tint = TextPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

/** One row of a single rounded white list container (rounded only at the ends, dividers between). */
@Composable
internal fun ListCardSegment(index: Int, last: Boolean, content: @Composable () -> Unit) {
    val shape = when {
        index == 0 && last -> RoundedCornerShape(20.dp)
        index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        last -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        else -> RoundedCornerShape(0.dp)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(White)
            .border(1.dp, FieldBorder, shape),
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffListRow(file: SharedFile, onClick: () -> Unit) {
    val scheduledAt = file.scheduledAt?.takeIf { file.isScheduled() }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        FileTile(file.format, size = 72.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName(file),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TypeChip(file.format)
                Text(
                    text = if (scheduledAt != null) {
                        stringResource(R.string.files_meta_scheduled, fileSizeLabel(file.sizeBytes), scheduleLabel(scheduledAt))
                    } else {
                        stringResource(R.string.files_meta, fileSizeLabel(file.sizeBytes), shortDateLabel(file.publishedAt))
                    },
                    modifier = Modifier.align(Alignment.CenterVertically),
                    color = TextSecondary,
                    fontSize = AppTextSize.Label,
                )
            }
            if (scheduledAt != null) {
                Spacer(Modifier.height(6.dp))
                LabelPill(stringResource(R.string.files_scheduled), AmberChipBackground, OnboardingAmberCard)
            }
            Spacer(Modifier.height(6.dp))
            IconLine(R.drawable.ic_people_outline, audienceLine(file))
            val viewed = file.viewedCount
            if (viewed != null && scheduledAt == null) {
                Spacer(Modifier.height(6.dp))
                ViewedProgress(viewed, file.audienceCount)
            }
        }
    }
    HorizontalDivider(color = FieldBorder)
}

@Composable
private fun StaffGridCard(file: SharedFile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheduledAt = file.scheduledAt?.takeIf { file.isScheduled() }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (file.kind == FileKind.VIDEO) FilesDark else file.format.tint.background),
                contentAlignment = Alignment.Center,
            ) {
                FileTile(file.format, size = 72.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = displayName(file),
                color = TextPrimary,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeChip(file.format)
                Spacer(Modifier.width(6.dp))
                Text(fileSizeLabel(file.sizeBytes), color = TextSecondary, fontSize = AppTextSize.Caption, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (scheduledAt != null) {
                    stringResource(R.string.files_scheduled_at, scheduleLabel(scheduledAt))
                } else {
                    shortDateLabel(file.publishedAt)
                },
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
            )
            Spacer(Modifier.height(4.dp))
            Text(audienceLine(file), color = TextSecondary, fontSize = AppTextSize.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            val viewed = file.viewedCount
            if (scheduledAt != null) {
                LabelPill(stringResource(R.string.files_scheduled), AmberChipBackground, OnboardingAmberCard)
            } else if (viewed != null) {
                ViewedProgress(viewed, file.audienceCount)
            }
        }
    }
}

/** "Hostel rules 2026–27.pdf" — the title with the file's extension. */
internal fun displayName(file: SharedFile): String = "${file.title}.${file.format.extension}"

@Composable
private fun EmptyFiles(onUpload: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(80.dp).clip(CircleShape).background(SelectedRowBackground), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_folder_outline), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.files_empty_title), color = TextPrimary, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.files_empty_staff_body), color = TextSecondary, fontSize = AppTextSize.Body, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onUpload,
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Icon(painterResource(R.drawable.ic_upload), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.files_upload_files), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun NoMatches() {
    Text(
        text = stringResource(R.string.files_no_matches),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        color = TextSecondary,
        fontSize = AppTextSize.Body,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FilesPreview() {
    GRADit_RewampTheme {
        val files = FileRepository.files.value
        FilesContent(
            uiState = FilesUiState(
                loading = false,
                totalCount = files.size,
                counts = FileTypeTab.entries.associateWith { tab -> files.count(tab::matches) },
                files = FileSort.NEWEST.apply(files),
                folders = FileRepository.folders.value,
            ),
            actions = FileListActions({}, {}, {}, onCategory = {}, onOpenFile = {}),
            onOpenFolders = {},
            onUpload = {},
        )
    }
}
