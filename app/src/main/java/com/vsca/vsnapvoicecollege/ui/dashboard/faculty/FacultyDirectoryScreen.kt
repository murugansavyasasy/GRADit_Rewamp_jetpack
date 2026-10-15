package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.FilterDropdown
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.Segment
import com.vsca.vsnapvoicecollege.ui.components.SegmentedControl
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
import com.vsca.vsnapvoicecollege.ui.theme.DestructiveRed
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tinted header strip of a department / division group. */
private val GroupHeaderBackground = Color(0xFFF7F8FC)

/**
 * Faculty directory for management and teachers: today's presence, cascading filters,
 * three groupings, a department summary and exports of the current selection.
 */
@Composable
fun FacultyDirectoryScreen(
    onBack: () -> Unit,
    onOpenFaculty: (String) -> Unit,
    onOpenFreePeriods: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacultyDirectoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val directory = uiState.directory
    // The college and its head count are known before today's list arrives.
    val college = directory ?: FacultyRepository.directory.value
    LazyColumn(
        modifier = modifier.fillMaxSize().background(ScreenBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.faculty_title),
                subtitle = stringResource(R.string.faculty_subtitle, college.institution, college.faculty.size),
                onBack = onBack,
                trailing = {
                    SquareIconButton(
                        iconRes = R.drawable.ic_download,
                        contentDescription = stringResource(R.string.faculty_export),
                        onClick = { if (uiState.load == LoadState.READY) viewModel.openExport() },
                    )
                },
            )
        }
        when (uiState.load) {
            LoadState.LOADING -> item(key = "loading") { LoadingPlaceholder() }
            LoadState.ERROR -> item(key = "error") { LoadError(viewModel::load) }
            LoadState.READY -> if (directory != null) {
                if (!uiState.isOnline && uiState.lastSyncedAt > 0) {
                    item(key = "offline") {
                        OfflineBanner(stringResource(R.string.faculty_offline_staff, clockLabel(uiState.lastSyncedAt)), Modifier.padding(horizontal = 16.dp))
                    }
                }
                item(key = "stats") {
                    val c = uiState.counts
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(c.total, stringResource(R.string.faculty_stat_faculty), Modifier.weight(1f))
                        StatTile(c.present, stringResource(R.string.faculty_stat_present), Modifier.weight(1f), background = Tiles.presentBg, valueColor = PresentGreen)
                        StatTile(
                            c.away, stringResource(R.string.faculty_stat_away), Modifier.weight(1f),
                            background = if (c.away > 0) Tiles.awayBg else White,
                            valueColor = if (c.away > 0) BadgeRed else TextPrimary,
                        )
                        StatTile(
                            directory.freePeriods.size, stringResource(R.string.faculty_stat_free), Modifier.weight(1f),
                            background = Tiles.amberBg, valueColor = OnboardingAmberCard, labelColor = OnboardingAmberCard, border = Tiles.amberBorder,
                            onClick = onOpenFreePeriods,
                        )
                    }
                }
                item(key = "search") {
                    Surface(Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                        SearchBar(uiState.filter.query, viewModel::onQueryChange, stringResource(R.string.faculty_search_hint), containerColor = White)
                    }
                }
                item(key = "filters") { FilterCard(uiState, directory, viewModel) }
                if (uiState.filter.isActive) item(key = "chips") { ActiveChips(uiState.filter, directory, viewModel) }
                item(key = "headline") {
                    Text(
                        headline(uiState, directory),
                        Modifier.padding(horizontal = 16.dp),
                        color = TextPrimary,
                        fontSize = AppTextSize.H6,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                item(key = "view") {
                    SegmentedControl(
                        options = listOf(
                            Segment(stringResource(R.string.faculty_view_department)),
                            Segment(stringResource(R.string.faculty_view_division)),
                            Segment(stringResource(R.string.faculty_view_all)),
                        ),
                        selectedIndex = uiState.view.ordinal,
                        onSelect = { viewModel.onView(DirectoryView.entries[it]) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        height = 52.dp,
                    )
                }
                val faculty = uiState.faculty
                if (faculty.isEmpty()) {
                    item(key = "empty") { EmptyResults(viewModel::onResetAll) }
                } else {
                    when (uiState.view) {
                        DirectoryView.BY_DEPARTMENT -> items(directory.byDepartment(faculty, uiState.filter), key = { "d-" + it.department.id }) { group ->
                            val present = group.faculty.count { it.presence.status == PresenceStatus.PRESENT }
                            GroupCard(
                                title = if (uiState.filter.streamId == null) {
                                    stringResource(R.string.faculty_dept_in_stream, group.department.name, directory.stream(group.department.streamId)?.name.orEmpty())
                                } else {
                                    group.department.name
                                },
                                subtitle = stringResource(R.string.faculty_group_present, group.faculty.size, present),
                                expanded = group.department.id !in uiState.collapsed,
                                onToggle = { viewModel.onToggleGroup(group.department.id) },
                                onSummary = { viewModel.openSummary(group.department.id) },
                            ) {
                                group.faculty.forEachIndexed { i, f ->
                                    if (i > 0) HorizontalDivider(color = FieldBorder)
                                    FacultyRow(f, directory, showDepartment = false, onClick = { onOpenFaculty(f.id) })
                                }
                            }
                        }
                        DirectoryView.BY_DIVISION -> items(directory.byDivision(faculty, uiState.filter), key = { "v-" + it.division.id }) { group ->
                            val d = group.division
                            GroupCard(
                                title = stringResource(R.string.faculty_division_label, d.programme, d.semester, d.section),
                                subtitle = group.classTeacher?.let { stringResource(R.string.faculty_division_teacher, it.name, group.faculty.size) }
                                    ?: stringResource(R.string.faculty_division_count, group.faculty.size),
                                expanded = d.id !in uiState.collapsed,
                                onToggle = { viewModel.onToggleGroup(d.id) },
                            ) {
                                group.faculty.forEachIndexed { i, f ->
                                    if (i > 0) HorizontalDivider(color = FieldBorder)
                                    FacultyRow(f, directory, showDepartment = true, onClick = { onOpenFaculty(f.id) })
                                }
                            }
                        }
                        DirectoryView.ALL -> item(key = "all") {
                            FacultyCard(Modifier.padding(horizontal = 16.dp)) {
                                Column {
                                    faculty.sortedWith(compareBy({ !it.isHod }, { it.name.substringAfter(' ') })).forEachIndexed { i, f ->
                                        if (i > 0) HorizontalDivider(color = FieldBorder)
                                        FacultyRow(f, directory, showDepartment = true, onClick = { onOpenFaculty(f.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (directory != null) {
        when (val sheet = uiState.sheet) {
            DirectorySheet.Stream -> StreamSheet(directory, uiState.filter.streamId, viewModel::onStream, viewModel::closeSheet)
            is DirectorySheet.Departments -> DepartmentSheet(directory, uiState.filter, sheet.draft, viewModel)
            is DirectorySheet.Summary -> SummarySheet(directory, sheet.departmentId, viewModel)
            is DirectorySheet.Export -> ExportSheet(sheet, uiState, directory, viewModel)
            null -> Unit
        }
    }
}

@Composable
private fun headline(state: DirectoryUiState, directory: FacultyDirectory): String {
    val count = state.faculty.size
    val stream = directory.stream(state.filter.streamId)
    val depts = state.filter.departmentIds.mapNotNull(directory::department).sortedBy { it.name }
    return when {
        stream == null -> stringResource(R.string.faculty_headline_all, count, directory.departments.size)
        depts.isEmpty() -> stringResource(R.string.faculty_headline_stream, count, stream.name, directory.departmentsIn(stream.id).size)
        else -> stringResource(R.string.faculty_headline_departments, count, stream.name, depts.joinToString(", ") { it.name })
    }
}

// ---- Filters ----

@Composable
private fun FilterCard(state: DirectoryUiState, directory: FacultyDirectory, vm: FacultyDirectoryViewModel) {
    val filter = state.filter
    FacultyCard(Modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StepField(
                step = 1,
                label = stringResource(R.string.faculty_step_stream),
                value = directory.stream(filter.streamId)?.name ?: stringResource(R.string.faculty_all_streams),
                active = filter.streamId != null,
                onClick = vm::openStreams,
            )
            if (filter.streamId == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_info_outline), null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.faculty_choose_stream), color = TextSecondary, fontSize = AppTextSize.Body)
                }
            } else {
                val depts = filter.departmentIds.mapNotNull(directory::department).sortedBy { it.name }
                StepField(
                    step = 2,
                    label = stringResource(R.string.faculty_step_department),
                    value = if (depts.isEmpty()) stringResource(R.string.faculty_all_departments) else depts.joinToString(", ") { it.name },
                    active = depts.isNotEmpty(),
                    onClick = vm::openDepartments,
                )
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                StepNumber(3, active = false)
                val all = stringResource(R.string.faculty_filter_all)
                val semLabel = stringResource(R.string.faculty_filter_sem)
                FilterDropdown(
                    label = semLabel,
                    value = filter.semester?.let { stringResource(R.string.faculty_sem_short, it) } ?: all,
                    selected = filter.semester?.let { stringResource(R.string.faculty_sem_short, it) } ?: all,
                    options = listOf<Pair<String, () -> Unit>>(all to { vm.onSemester(null) }) +
                        directory.semesterOptions(filter).map { sem -> stringResource(R.string.faculty_sem_short, sem) to { vm.onSemester(sem) } },
                    highlighted = filter.semester != null,
                )
                FilterDropdown(
                    label = stringResource(R.string.faculty_filter_division),
                    value = filter.section ?: all,
                    selected = filter.section ?: all,
                    options = listOf<Pair<String, () -> Unit>>(all to { vm.onSection(null) }) +
                        directory.sectionOptions(filter).map { sec -> sec to { vm.onSection(sec) } },
                    highlighted = filter.section != null,
                )
            }
            Text(stringResource(R.string.faculty_optional), color = TextSecondary, fontSize = AppTextSize.Body)
        }
    }
}

@Composable
private fun StepField(step: Int, label: String, value: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (active) SelectedRowBackground else White,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) BrandBlue else FieldBorder),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            StepNumber(step, active = true)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TextSecondary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Medium)
                Text(value, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold, maxLines = 2)
            }
            Icon(painterResource(R.drawable.ic_expand_more), null, tint = TextPrimary)
        }
    }
}

@Composable
private fun StepNumber(step: Int, active: Boolean) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(if (active) BrandBlue else BadgeNeutralBackground), contentAlignment = Alignment.Center) {
        Text(step.toString(), color = if (active) White else TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveChips(filter: DirectoryFilter, directory: FacultyDirectory, vm: FacultyDirectoryViewModel) {
    FlowRow(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        directory.stream(filter.streamId)?.let { RemovableChip(it.name, vm::onRemoveStream) }
        filter.departmentIds.mapNotNull(directory::department).sortedBy { it.name }.forEach { d -> RemovableChip(d.name) { vm.onRemoveDepartment(d.id) } }
        filter.semester?.let { RemovableChip(stringResource(R.string.faculty_sem_short, it), vm::onRemoveSemester) }
        filter.section?.let { RemovableChip(stringResource(R.string.faculty_division_chip, it), vm::onRemoveSection) }
        if (filter.query.isNotBlank()) RemovableChip(stringResource(R.string.faculty_query_chip, filter.query.trim()), vm::onClearQuery)
        Text(
            stringResource(R.string.faculty_reset_all),
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = vm::onResetAll).padding(horizontal = 8.dp, vertical = 10.dp),
            color = DestructiveRed,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Composable
private fun RemovableChip(text: String, onRemove: () -> Unit) {
    Surface(onClick = onRemove, shape = RoundedCornerShape(50), color = SelectedRowBackground, border = BorderStroke(1.dp, Color(0xFFB9C6F2))) {
        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.faculty_remove_filter, text), tint = BrandBlue, modifier = Modifier.size(18.dp))
        }
    }
}

// ---- Groups and rows ----

@Composable
private fun GroupCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSummary: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    FacultyCard(Modifier.padding(horizontal = 16.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(GroupHeaderBackground).clickable(onClick = onToggle).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                        if (onSummary != null) {
                            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSummary).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.faculty_summary), color = BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Body)
                }
                Icon(
                    painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                    stringResource(if (expanded) R.string.faculty_collapse else R.string.faculty_expand, title),
                    tint = TextPrimary,
                )
            }
            if (expanded) {
                HorizontalDivider(color = FieldBorder)
                content()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacultyRow(f: Faculty, directory: FacultyDirectory, showDepartment: Boolean, onClick: () -> Unit) {
    val classTeacher = directory.classTeacherOf(f.id).isNotEmpty()
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.align(Alignment.Top)) { InitialsAvatar(f.initials) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(f.name, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
            if (f.isHod || classTeacher) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (f.isHod) HodBadge()
                    if (classTeacher) ClassTeacherBadge()
                }
            }
            Text(
                if (showDepartment) stringResource(R.string.faculty_designation_dept, f.designation, directory.department(f.departmentId)?.name.orEmpty()) else f.designation,
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
            )
            Text(f.specialisation, color = TextSecondary, fontSize = AppTextSize.Body)
            PresencePill(f.presence, Modifier.padding(top = 2.dp))
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = TextPrimary, modifier = Modifier.size(24.dp))
    }
}

// ---- States ----

@Composable
internal fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp)).background(FieldBorder))
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(18.dp)).background(FieldBorder))
        SkeletonCards(2)
    }
}

@Composable
internal fun LoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    FacultyCard(modifier.padding(horizontal = 16.dp)) {
        StateMessage(
            iconRes = R.drawable.ic_wifi,
            iconTint = BadgeRed,
            iconBackground = AlertBackground,
            title = stringResource(R.string.faculty_error_title),
            subtitle = stringResource(R.string.faculty_error_body),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Icon(painterResource(R.drawable.ic_refresh), null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.faculty_try_again), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyResults(onReset: () -> Unit) {
    FacultyCard(Modifier.padding(horizontal = 16.dp)) {
        StateMessage(
            iconRes = R.drawable.ic_people_outline,
            iconTint = BrandBlue,
            iconBackground = SelectedRowBackground,
            title = stringResource(R.string.faculty_empty_title),
            subtitle = stringResource(R.string.faculty_empty_body),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onReset, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, FieldBorder), modifier = Modifier.height(52.dp)) {
                Text(stringResource(R.string.faculty_reset_filters), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---- Sheets ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FacultySheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = White) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) { content() }
    }
}

@Composable
private fun StreamSheet(directory: FacultyDirectory, selected: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    FacultySheet(onDismiss) {
        SheetHeader(stringResource(R.string.faculty_step_stream), stringResource(R.string.faculty_stream_sheet_sub), onDismiss)
        Spacer(Modifier.height(18.dp))
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioCard(
                stringResource(R.string.faculty_all_streams),
                stringResource(R.string.faculty_stream_counts, directory.faculty.size, directory.departments.size),
                selected = selected == null,
                onClick = { onPick(null) },
            )
            directory.streams.forEach { s ->
                RadioCard(
                    s.name,
                    stringResource(R.string.faculty_stream_counts, directory.filtered(DirectoryFilter(streamId = s.id)).size, directory.departmentsIn(s.id).size),
                    selected = selected == s.id,
                    onClick = { onPick(s.id) },
                )
            }
        }
    }
}

@Composable
private fun DepartmentSheet(directory: FacultyDirectory, filter: DirectoryFilter, draft: Set<String>, vm: FacultyDirectoryViewModel) {
    val stream = directory.stream(filter.streamId) ?: return
    val departments = directory.departmentsIn(stream.id)
    val preview = directory.filtered(filter.copy(departmentIds = draft, semester = null, section = null)).size
    FacultySheet(vm::closeSheet) {
        SheetHeader(stringResource(R.string.faculty_department_sheet), stringResource(R.string.faculty_department_sheet_sub, stream.name), vm::closeSheet)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CheckCard(stringResource(R.string.faculty_all_departments), directory.filtered(DirectoryFilter(streamId = stream.id)).size, checked = false, bold = true) {
                vm.onToggleDepartmentDraft(null)
            }
            departments.forEach { d ->
                CheckCard(d.name, directory.facultyIn(d).size, checked = d.id in draft) { vm.onToggleDepartmentDraft(d.id) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = vm::onClearDepartmentDraft,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FieldBorder),
            ) { Text(stringResource(R.string.faculty_clear), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold) }
            Button(
                onClick = vm::onApplyDepartments,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) { Text(stringResource(R.string.faculty_show_count, preview), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1) }
        }
    }
}

@Composable
private fun CheckCard(title: String, count: Int, checked: Boolean, bold: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (checked) SelectedRowBackground else White,
        border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) BrandBlue else FieldBorder),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                Modifier.size(26.dp),
                shape = RoundedCornerShape(7.dp),
                color = if (checked) BrandBlue else White,
                border = if (checked) null else BorderStroke(2.dp, Color(0xFFB4BACB)),
            ) {
                if (checked) Icon(painterResource(R.drawable.ic_check), null, tint = White, modifier = Modifier.padding(4.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(title, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold)
            Text(count.toString(), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummarySheet(directory: FacultyDirectory, departmentId: String, vm: FacultyDirectoryViewModel) {
    val dept = directory.department(departmentId) ?: return
    val people = directory.facultyIn(dept)
    val c = counts(people)
    val hod = directory.hodOf(dept.id)
    FacultySheet(vm::closeSheet) {
        SheetHeader(
            stringResource(R.string.faculty_summary_title, dept.name),
            stringResource(R.string.faculty_summary_sub, directory.stream(dept.streamId)?.name.orEmpty(), todayLabel()),
            vm::closeSheet,
        )
        Spacer(Modifier.height(16.dp))
        if (hod != null) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = SelectedRowBackground) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(hod.initials, background = Color(0xFFDDE5FC), content = BrandBlue)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(stringResource(R.string.faculty_hod_label), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                        Text(hod.name, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                        Text(
                            listOfNotNull(hod.designation, presenceText(hod.presence)).joinToString(" · "),
                            color = TextSecondary,
                            fontSize = AppTextSize.Body,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(c.total, stringResource(R.string.faculty_stat_faculty), Modifier.weight(1f))
            StatTile(c.present, stringResource(R.string.faculty_stat_present), Modifier.weight(1f), background = Tiles.presentBg, valueColor = PresentGreen)
            StatTile(c.away, stringResource(R.string.faculty_stat_away_inline), Modifier.weight(1f), background = if (c.away > 0) Tiles.awayBg else White, valueColor = if (c.away > 0) BadgeRed else TextPrimary)
        }
        val covered = directory.divisionsCovered(dept.id)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.faculty_divisions_covered, covered.size), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            covered.forEach { d ->
                Surface(shape = RoundedCornerShape(10.dp), color = ScreenBackground, border = BorderStroke(1.dp, FieldBorder)) {
                    Text(
                        stringResource(R.string.faculty_division_short, d.programme, d.semester, d.section),
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = TextPrimary,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { vm.onShowOnly(dept.id) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
        ) {
            Icon(painterResource(R.drawable.ic_people_outline), null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.faculty_show_only, dept.name), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun presenceText(p: Presence): String = when (p.status) {
    PresenceStatus.PRESENT -> p.punchMinute?.let { stringResource(R.string.faculty_present_at, timeOfDay(it)) } ?: stringResource(R.string.faculty_present)
    PresenceStatus.ABSENT -> stringResource(R.string.faculty_absent)
    PresenceStatus.ON_LEAVE -> stringResource(R.string.faculty_on_leave)
    PresenceStatus.NOT_IN -> stringResource(R.string.faculty_not_in)
}

/** "today, Wed 07 Oct". */
@Composable
internal fun todayLabel(): String =
    stringResource(R.string.faculty_today_date, SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date()))

@Composable
private fun ExportSheet(sheet: DirectorySheet.Export, state: DirectoryUiState, directory: FacultyDirectory, vm: FacultyDirectoryViewModel) {
    val context = LocalContext.current
    val count = state.faculty.size
    val result = sheet.result
    FacultySheet(vm::closeSheet) {
        if (result == null) {
            SheetHeader(stringResource(R.string.faculty_export_title), stringResource(R.string.faculty_export_sub, count), vm::closeSheet)
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioCard(stringResource(R.string.faculty_export_pdf), stringResource(R.string.faculty_export_pdf_sub), sheet.format == ExportFormat.PDF, { vm.onExportFormat(ExportFormat.PDF) })
                RadioCard(stringResource(R.string.faculty_export_excel), stringResource(R.string.faculty_export_excel_sub), sheet.format == ExportFormat.EXCEL, { vm.onExportFormat(ExportFormat.EXCEL) })
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = ScreenBackground, border = BorderStroke(1.dp, FieldBorder)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.faculty_export_file_name), color = TextSecondary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                        Text(
                            directory.exportBaseName(state.filter) + "." + sheet.format.extension,
                            color = TextPrimary,
                            fontSize = AppTextSize.H6,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.faculty_export_includes, count), color = TextSecondary, fontSize = AppTextSize.Body)
                    }
                }
                if (state.exportFailed) {
                    Text(stringResource(R.string.faculty_export_failed), color = BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = vm::onExport,
                    enabled = !sheet.exporting && count > 0,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    if (sheet.exporting) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.5.dp)
                    } else {
                        Icon(painterResource(R.drawable.ic_download), null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(if (sheet.format == ExportFormat.PDF) R.string.faculty_export_as_pdf else R.string.faculty_export_as_excel),
                            fontSize = AppTextSize.BodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        } else {
            SheetHeader(stringResource(R.string.faculty_export_title), stringResource(R.string.faculty_export_saved), vm::closeSheet)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(PresentBackground), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = PresentGreen, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.faculty_export_complete), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
                Text(
                    result.name,
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { FacultyExport.open(context, result) }.padding(4.dp),
                    color = BrandBlue,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(
                        R.string.faculty_export_saved_detail,
                        result.count,
                        stringResource(if (result.format == ExportFormat.PDF) R.string.faculty_export_pdf else R.string.faculty_export_excel_short),
                        SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date(result.savedAt)) + ", " + clockLabel(result.savedAt),
                    ),
                    color = TextSecondary,
                    fontSize = AppTextSize.Body,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = vm::closeSheet,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                ) { Text(stringResource(R.string.faculty_done), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { FacultyExport.share(context, result) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    Icon(painterResource(R.drawable.ic_share_nodes), null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.faculty_share_file), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
