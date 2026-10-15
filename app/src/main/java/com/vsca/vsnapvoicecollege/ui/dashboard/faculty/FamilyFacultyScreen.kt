package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.Segment
import com.vsca.vsnapvoicecollege.ui.components.SegmentedControl
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Faculty for parents: each child's teachers by semester — class teacher, subjects and
 * weekly load — with a quick way to message them.
 */
@Composable
fun FamilyFacultyScreen(
    onBack: () -> Unit,
    onOpenTeacher: (childId: String, semester: Int, teacherId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyFacultyViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffects(viewModel)
    val child = uiState.child
    val data = uiState.data
    LazyColumn(
        modifier = modifier.fillMaxSize().background(ScreenBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.faculty_title),
                subtitle = child?.let { stringResource(R.string.faculty_family_subtitle, it.firstName) },
                onBack = onBack,
            )
        }
        if (!uiState.isOnline && uiState.lastSyncedAt > 0 && uiState.load == LoadState.READY) {
            item(key = "offline") { OfflineBanner(familyOfflineText(uiState.lastSyncedAt), Modifier.padding(horizontal = 16.dp)) }
        }
        if (uiState.children.size > 1) {
            item(key = "children") {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.children.forEach { c -> ChildCard(c, c.id == uiState.childId, { viewModel.onChild(c.id) }, Modifier.weight(1f)) }
                }
            }
        }
        if (child != null) {
            item(key = "semesters") { SemesterChips(child, uiState.semester, viewModel::onSemester) }
        }
        when (uiState.load) {
            LoadState.LOADING -> item(key = "loading") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(22.dp)).background(FieldBorder))
                    SkeletonCards(2)
                }
            }
            LoadState.ERROR -> item(key = "error") { LoadError(viewModel::load) }
            LoadState.READY -> {
                val sem = uiState.semesterClass
                if (data != null && sem != null) {
                    item(key = "class-${data.child.id}-${sem.semester}") {
                        SemesterCard(data, sem, onMessage = viewModel::onMessage, onOpenTeacher = { onOpenTeacher(data.child.id, sem.semester, it) })
                    }
                    if (sem.loads.isEmpty()) {
                        item(key = "unassigned") { NotAssignedCard(data, sem, viewModel::onCurrentSemester) }
                        item(key = "powered") { PoweredByRow(Modifier.fillMaxWidth().wrapContentWidth().padding(vertical = 8.dp), textColor = TextSecondary) }
                    } else {
                        item(key = "tabs") {
                            SegmentedControl(
                                options = listOf(
                                    Segment(stringResource(R.string.faculty_tab_all_short)),
                                    Segment(stringResource(R.string.faculty_kind_theory)),
                                    Segment(stringResource(R.string.faculty_tab_labs)),
                                ),
                                selectedIndex = uiState.tab.ordinal,
                                onSelect = { viewModel.onTab(TeacherTab.entries[it]) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                height = 52.dp,
                            )
                        }
                        item(key = "teachers-${uiState.tab}") {
                            val loads = sem.loadsFor(uiState.tab)
                            FacultyCard(Modifier.padding(horizontal = 16.dp)) {
                                Column {
                                    loads.forEachIndexed { i, load ->
                                        if (i > 0) HorizontalDivider(color = FieldBorder)
                                        TeacherRow(
                                            data = data,
                                            sem = sem,
                                            load = load,
                                            tab = uiState.tab,
                                            showAway = data.status(sem.semester) == SemesterStatus.CURRENT,
                                            onClick = { onOpenTeacher(data.child.id, sem.semester, load.teacherId) },
                                            onMessage = { viewModel.onMessage(load.teacherId) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    MessageSheetHost(viewModel, data)
}

/** "You're offline. Showing the list saved this morning. Messages will send when you're back online." */
@Composable
private fun familyOfflineText(savedAt: Long): String {
    val hour = Calendar.getInstance().apply { timeInMillis = savedAt }.get(Calendar.HOUR_OF_DAY)
    return stringResource(
        when {
            hour < 12 -> R.string.faculty_offline_family_morning
            hour < 17 -> R.string.faculty_offline_family_afternoon
            else -> R.string.faculty_offline_family_evening
        },
    )
}

@Composable
private fun SemesterChips(child: Child, selected: Int, onSelect: (Int) -> Unit) {
    val numbers = (1..(child.currentSemester + 1).coerceAtMost(8)).toList()
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.faculty_semester), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            numbers.forEach { n ->
                val isSelected = n == selected
                val status = when {
                    n < child.currentSemester -> R.string.faculty_sem_done
                    n == child.currentSemester -> R.string.faculty_sem_current
                    else -> R.string.faculty_sem_next
                }
                Surface(
                    onClick = { onSelect(n) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) BrandBlue else White,
                    border = if (isSelected) null else BorderStroke(1.dp, FieldBorder),
                ) {
                    Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.faculty_sem_short, n),
                            color = if (isSelected) White else if (n > child.currentSemester) TextSecondary else TextPrimary,
                            fontSize = AppTextSize.BodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                        Text(
                            stringResource(status),
                            color = if (isSelected) White else TextSecondary,
                            fontSize = AppTextSize.Label,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SemesterCard(data: ChildFaculty, sem: SemesterClass, onMessage: (String) -> Unit, onOpenTeacher: (String) -> Unit) {
    val child = data.child
    val status = data.status(sem.semester)
    FacultyCard(Modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.faculty_division_label, child.programme, sem.semester, sem.section),
                        color = TextPrimary,
                        fontSize = AppTextSize.H6,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        if (status == SemesterStatus.CURRENT) {
                            stringResource(
                                if (sem.isOdd) R.string.faculty_odd_semester else R.string.faculty_even_semester,
                                "${sem.academicYearStart}–${(sem.academicYearStart + 1).toString().takeLast(2)}",
                                monthRangeLabel(sem.months),
                            )
                        } else {
                            monthRangeLabel(sem.months)
                        },
                        color = TextSecondary,
                        fontSize = AppTextSize.Body,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(status)
            }
            val classTeacher = data.teacher(sem.classTeacherId)
            if (classTeacher != null) {
                Spacer(Modifier.height(14.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = PresentBackground) {
                    Row(Modifier.clickable { onOpenTeacher(classTeacher.id) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(classTeacher.initials, size = 60.dp, background = White, content = PresentGreen)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_star), null, tint = PresentGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.faculty_badge_class_teacher), color = PresentGreen, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                            }
                            Text(classTeacher.name, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                            Text(
                                stringResource(R.string.faculty_parent_hours_line, classTeacher.designation, parentHoursLabel(classTeacher.parentHours)),
                                color = TextSecondary,
                                fontSize = AppTextSize.Body,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        MessageButton(stringResource(R.string.faculty_message_name, classTeacher.name), { onMessage(classTeacher.id) }, tint = PresentGreen)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (sem.loads.isEmpty()) {
                    stringResource(R.string.faculty_no_teachers_yet)
                } else {
                    listOf(
                        pluralStringResource(R.plurals.faculty_teachers, sem.teacherCount, sem.teacherCount),
                        pluralStringResource(R.plurals.faculty_subjects, sem.subjectCount, sem.subjectCount),
                        pluralStringResource(R.plurals.faculty_periods_a_week, sem.periodsPerWeek, sem.periodsPerWeek),
                    ).joinToString(" · ")
                },
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusPill(status: SemesterStatus) {
    val (label, bg, fg) = when (status) {
        SemesterStatus.COMPLETED -> Triple(R.string.faculty_status_completed, BadgeNeutralBackground, TextPrimary)
        SemesterStatus.CURRENT -> Triple(R.string.faculty_status_current, BrandBlue, White)
        SemesterStatus.UPCOMING -> Triple(R.string.faculty_status_upcoming, Tiles.amberBg, OnboardingAmberCard)
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(stringResource(label), Modifier.padding(horizontal = 14.dp, vertical = 5.dp), color = fg, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
    }
}

/** "Thu, 03:05 – 03:55 PM". */
@Composable
internal fun parentHoursLabel(h: ParentHours): String = shortDay(h.day) + ", " + timeRange(h.startMinute, h.endMinute)

@Composable
private fun NotAssignedCard(data: ChildFaculty, sem: SemesterClass, onSeeCurrent: () -> Unit) {
    FacultyCard(Modifier.padding(horizontal = 16.dp)) {
        StateMessage(
            iconRes = R.drawable.ic_people_outline,
            iconTint = BrandBlue,
            iconBackground = SelectedRowBackground,
            title = stringResource(R.string.faculty_not_assigned_title),
            subtitle = stringResource(
                R.string.faculty_not_assigned_body,
                stringResource(R.string.faculty_division_label, data.child.programme, sem.semester, sem.section),
                monthRangeLabel(sem.months),
                DateFormatMonth.previous(sem.months.start),
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onSeeCurrent,
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) { Text(stringResource(R.string.faculty_see_current), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Month names for "usually by the end of November". */
private object DateFormatMonth {
    /** Full name of the month before [monthIndex] (year * 12 + month − 1). */
    fun previous(monthIndex: Int): String = java.text.DateFormatSymbols.getInstance().months[(monthIndex - 1 + 12) % 12]
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherRow(
    data: ChildFaculty,
    sem: SemesterClass,
    load: TeacherLoad,
    tab: TeacherTab,
    showAway: Boolean,
    onClick: () -> Unit,
    onMessage: () -> Unit,
) {
    val teacher = data.teacher(load.teacherId) ?: return
    val isClassTeacher = sem.classTeacherId == teacher.id
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.align(Alignment.Top)) { InitialsAvatar(teacher.initials, background = SelectedRowBackground, content = BrandBlue) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(teacher.name, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
            Text(
                if (isClassTeacher) stringResource(R.string.faculty_designation_ct, teacher.designation) else teacher.designation,
                color = TextSecondary,
                fontSize = AppTextSize.BodyLarge,
            )
            sem.subjectsFor(load, tab).forEach { s ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                    KindTag(s.kind)
                    Text(pluralStringResource(R.plurals.faculty_periods_per_week, s.periodsPerWeek, s.periodsPerWeek), color = TextSecondary, fontSize = AppTextSize.Body)
                }
            }
            if (showAway && load.isAwayToday) {
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = Tiles.amberBg) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_calendar), null, tint = OnboardingAmberCard, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.faculty_away_today), color = OnboardingAmberCard, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(awaySummary(load.awayToday), color = OnboardingAmberCard, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        MessageButton(stringResource(R.string.faculty_message_name, teacher.name), onMessage)
    }
}

/** "P3 and P5 are free periods · self-study" or "P4 covered by Dr. Kavitha M.". */
@Composable
private fun awaySummary(away: List<AwayPeriod>): String {
    val free = away.filter { it.substitute == null }.map { it.period }
    val covered = away.filter { it.substitute != null }
    val parts = buildList {
        if (free.isNotEmpty()) add(pluralStringResource(R.plurals.faculty_free_self_study, free.size, periodList(free)))
        covered.forEach { add(stringResource(R.string.faculty_covered_by, it.period, it.substitute!!)) }
    }
    return parts.joinToString(" · ")
}

// ---- Teacher profile (parents) ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyTeacherScreen(
    onBack: () -> Unit,
    onOpenTimetable: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyTeacherViewModel = viewModel(),
) {
    MessageEffects(viewModel)
    val data = viewModel.data
    val sem = data?.semester(viewModel.semester)
    val teacher = data?.teacher(viewModel.teacherId)
    val load = sem?.loads?.firstOrNull { it.teacherId == viewModel.teacherId }
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.faculty_profile_title),
            subtitle = data?.let { stringResource(R.string.faculty_child_sem, it.child.name, viewModel.semester) },
            onBack = onBack,
        )
        if (data == null || sem == null || teacher == null || load == null) {
            StateMessage(
                iconRes = R.drawable.ic_people_outline,
                iconTint = BrandBlue,
                iconBackground = SelectedRowBackground,
                title = stringResource(R.string.faculty_not_found),
                subtitle = stringResource(R.string.faculty_not_found_body),
                modifier = Modifier.padding(24.dp),
            )
            return@Column
        }
        val child = data.child
        val status = data.status(sem.semester)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FacultyCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(teacher.initials, size = 84.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(teacher.name, color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                            Text(stringResource(R.string.faculty_designation_dept, teacher.designation, teacher.department), color = TextPrimary, fontSize = AppTextSize.BodyLarge)
                            if (sem.classTeacherId == teacher.id) ClassTeacherBadge()
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = FieldBorder)
                    DetailRow(stringResource(R.string.faculty_department), teacher.department)
                    HorizontalDivider(color = FieldBorder)
                    DetailRow(stringResource(R.string.faculty_qualification), teacher.qualification)
                    HorizontalDivider(color = FieldBorder)
                    DetailRow(stringResource(R.string.faculty_experience), pluralStringResource(R.plurals.faculty_years, teacher.experienceYears, teacher.experienceYears))
                    HorizontalDivider(color = FieldBorder)
                    val also = data.alsoTaught(teacher.id, except = sem.semester)
                    DetailRow(
                        stringResource(R.string.faculty_also_taught, child.firstName),
                        if (also.isEmpty()) stringResource(R.string.faculty_only_this_sem) else also.map { stringResource(R.string.faculty_sem_short, it) }.joinToString(", "),
                    )
                }
            }
            if (status == SemesterStatus.CURRENT && load.isAwayToday) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Tiles.amberBg, border = BorderStroke(1.dp, Tiles.amberBorder)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_calendar), null, tint = OnboardingAmberCard, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.faculty_on_leave_today, SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date())),
                                color = OnboardingAmberCard,
                                fontSize = AppTextSize.H6,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        load.awayToday.forEach { a ->
                            Text(
                                if (a.substitute != null) stringResource(R.string.faculty_away_substitute, a.period, a.subject, a.substitute)
                                else stringResource(R.string.faculty_away_free, a.period, a.subject),
                                color = OnboardingAmberCard,
                                fontSize = AppTextSize.BodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Row(Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenTimetable), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.faculty_see_timetable),
                                color = OnboardingAmberCard,
                                fontSize = AppTextSize.BodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                textDecoration = TextDecoration.Underline,
                            )
                            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = OnboardingAmberCard, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            FacultyCard {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        stringResource(if (status == SemesterStatus.COMPLETED) R.string.faculty_taught_child else R.string.faculty_teaches_child, child.firstName, sem.semester),
                        color = TextPrimary,
                        fontSize = AppTextSize.H6,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    load.subjects.forEach { s ->
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = FieldBorder)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                            KindTag(s.kind)
                        }
                        Text(pluralStringResource(R.plurals.faculty_periods_per_week, s.periodsPerWeek, s.periodsPerWeek), color = TextPrimary, fontSize = AppTextSize.BodyLarge)
                        if (s.slots.isNotEmpty()) {
                            Text(stringResource(R.string.faculty_when, slotsLabel(s.slots)), color = TextSecondary, fontSize = AppTextSize.Body)
                        }
                    }
                }
            }
            FacultyCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.faculty_meet_teacher), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                    IconText(R.drawable.ic_clock, stringResource(R.string.faculty_parent_hours, parentHoursLabel(teacher.parentHours)), color = TextPrimary)
                    IconText(R.drawable.ic_location_on, teacher.cabin, color = TextPrimary)
                    Text(stringResource(R.string.faculty_meet_note), color = TextSecondary, fontSize = AppTextSize.Body)
                }
            }
        }
        Button(
            onClick = { viewModel.onMessage(teacher.id) },
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
        ) {
            Icon(painterResource(R.drawable.ic_chat_bubble_outline), null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.faculty_message_name, teacher.name), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
    MessageSheetHost(viewModel, data)
}

// ---- Messaging ----

@Composable
private fun MessageEffects(viewModel: TeacherMessagingViewModel) {
    val context = LocalContext.current
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val text = when (notice) {
        MessageNotice.SENT -> stringResource(R.string.faculty_message_sent)
        MessageNotice.QUEUED -> stringResource(R.string.faculty_message_queued)
        MessageNotice.FLUSHED -> stringResource(R.string.faculty_message_flushed)
        null -> null
    }
    LaunchedEffect(text) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.onNoticeShown()
        }
    }
}

@Composable
private fun MessageSheetHost(viewModel: TeacherMessagingViewModel, data: ChildFaculty?) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val d = draft ?: return
    val teacher = data?.teacher(d.teacherId) ?: return
    val online by NetworkMonitor.isOnline.collectAsStateWithLifecycle()
    val child = data.child
    FacultySheet(viewModel::onDismissMessage) {
        SheetHeader(
            stringResource(R.string.faculty_message_name, teacher.name),
            stringResource(R.string.faculty_message_about, child.name, stringResource(R.string.faculty_child_class, child.programme, child.currentSemester, child.section)),
            viewModel::onDismissMessage,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = d.text,
            onValueChange = viewModel::onDraftChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
            placeholder = { Text(stringResource(R.string.faculty_message_hint), color = TextSecondary) },
            shape = RoundedCornerShape(16.dp),
            colors = appTextFieldColors(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(if (online) R.string.faculty_message_note else R.string.faculty_message_offline_note),
            color = TextSecondary,
            fontSize = AppTextSize.Body,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::onSend,
            enabled = d.text.isNotBlank() && !d.sending,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
        ) {
            if (d.sending) {
                CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.5.dp)
            } else {
                Icon(painterResource(R.drawable.ic_send), null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(if (online) R.string.faculty_send else R.string.faculty_send_later), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---- Students ----

/** A student's teachers: name, role and subject, and the cabin to find them in. */
@Composable
fun StudentFacultyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val entries = FacultyRepository.studentFaculty.filter {
        query.isBlank() || it.name.contains(query.trim(), true) || it.subject.contains(query.trim(), true) || it.designation.contains(query.trim(), true)
    }
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(title = stringResource(R.string.faculty_title), onBack = onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                SearchBar(query, { query = it }, stringResource(R.string.faculty_student_search), containerColor = White)
            }
            FacultyCard {
                if (entries.isEmpty()) {
                    StateMessage(
                        iconRes = R.drawable.ic_people_outline,
                        iconTint = BrandBlue,
                        iconBackground = SelectedRowBackground,
                        title = stringResource(R.string.faculty_student_none),
                        subtitle = stringResource(R.string.faculty_student_none_body),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                    )
                } else {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        entries.forEachIndexed { i, e ->
                            if (i > 0) HorizontalDivider(color = FieldBorder)
                            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(SelectedRowBackground), contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.ic_people_outline), null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                                    Text(listOfNotNull(e.designation, e.subject, e.note).joinToString(" · "), color = TextSecondary, fontSize = AppTextSize.Body)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(e.cabin, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
    }
}
