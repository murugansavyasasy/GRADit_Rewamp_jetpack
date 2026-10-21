package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.teacher

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AppTextField
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.FilterDropdown
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.Segment
import com.vsca.vsnapvoicecollege.ui.components.SegmentedControl
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.mediumDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportStudent
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultySheet
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.timeOfDay
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** "Mark attendance" for a class period: topic, P / A per student, on duty / on leave, then send. */
@Composable
fun MarkAttendanceScreen(onBack: () -> Unit, onSent: () -> Unit, modifier: Modifier = Modifier, viewModel: MarkAttendanceViewModel = viewModel()) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sentText = stringResource(R.string.att_sent_toast, s.session?.subject.orEmpty(), s.count(StudentStatus.PRESENT) + s.count(StudentStatus.ON_DUTY))
    val failedText = stringResource(R.string.att_send_failed)
    LaunchedEffect(s.send) {
        when (s.send) {
            SendState.SENT -> {
                Toast.makeText(context, sentText, Toast.LENGTH_SHORT).show()
                onSent()
            }
            SendState.FAILED -> {
                Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
                viewModel.onFailureShown()
            }
            else -> Unit
        }
    }
    val session = s.session
    val c = s.reportClass
    Column(modifier.fillMaxSize().background(ScreenBackground).imePadding()) {
        TitledTopBar(title = stringResource(R.string.att_mark_title), subtitle = mediumDate(s.day), onBack = onBack)
        if (session == null || c == null) {
            Text(stringResource(R.string.att_not_found), Modifier.padding(32.dp), color = TextSecondary)
            return@Column
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            item(key = "class") {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(SelectedRowBackground), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_book), null, tint = BrandBlue, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(session.subject, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                            Text(classLongLabel(c.programme, c.division.semester, c.division.section), color = TextSecondary, fontSize = AppTextSize.Body)
                            Text(stringResource(R.string.att_room_students, session.room, c.students.size), color = TextSecondary, fontSize = AppTextSize.Label)
                        }
                    }
                }
            }
            item(key = "period") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.att_marking_for), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    val labels = s.periods.map { periodLabel(it) }
                    DropdownField(periodLabel(session), labels, { picked -> s.periods.getOrNull(labels.indexOf(picked))?.let(viewModel::onPeriod) })
                    Spacer(Modifier.height(12.dp))
                    SegmentedControl(
                        options = SessionKind.entries.map { Segment(stringResource(it.label)) },
                        selectedIndex = s.kind.ordinal,
                        onSelect = { viewModel.onKind(SessionKind.entries[it]) },
                        height = 50.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = s.topic,
                        onValueChange = viewModel::onTopic,
                        label = stringResource(R.string.att_topic_covered),
                        placeholder = stringResource(R.string.att_topic_hint),
                    )
                }
            }
            item(key = "search") {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchBar(
                        query = s.query,
                        onQueryChange = viewModel::onQuery,
                        hint = stringResource(R.string.att_search_student),
                        containerColor = White,
                        modifier = Modifier.weight(1f).border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
                    )
                    val name = stringResource(R.string.att_sort_name)
                    val reg = stringResource(R.string.att_sort_register)
                    FilterDropdown(
                        label = null,
                        value = if (s.sort == StudentSort.NAME) name else reg,
                        options = listOf(name to { viewModel.onSort(StudentSort.NAME) }, reg to { viewModel.onSort(StudentSort.REGISTER) }),
                        selected = if (s.sort == StudentSort.NAME) name else reg,
                    )
                }
            }
            item(key = "status-chips") { StatusChips(s, viewModel::onFilter) }
            item(key = "hint") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.att_tap_circle), Modifier.weight(1f), color = TextSecondary, fontSize = AppTextSize.Label)
                    Text(
                        stringResource(R.string.att_mark_all_present),
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = viewModel::onMarkAllPresent).padding(8.dp),
                        color = BrandBlue,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (s.shown.isEmpty()) {
                item(key = "none") {
                    Text(stringResource(R.string.att_no_students_match), Modifier.fillMaxWidth().padding(24.dp), color = TextSecondary, fontSize = AppTextSize.Body, textAlign = TextAlign.Center)
                }
            }
            items(s.shown, key = { it.id }) { student ->
                StudentCard(
                    student = student,
                    status = s.statuses[student.id] ?: StudentStatus.PRESENT,
                    classId = c.id,
                    onToggle = { viewModel.onToggle(student.id) },
                    onDuty = { viewModel.onOnDuty(student.id, it) },
                    onLeave = { viewModel.onOnLeave(student.id, it) },
                )
            }
        }
        Surface(Modifier.fillMaxWidth(), color = White, shadowElevation = 8.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(0.45f).height(56.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, FieldBorder)) {
                    Text(stringResource(R.string.att_cancel), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = viewModel::openConfirm,
                    modifier = Modifier.weight(0.55f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_send), null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (s.isEdit) R.string.att_update_attendance else R.string.att_send_attendance), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
    if (s.confirming && session != null && c != null) {
        ConfirmSheet(s, session, classLongLabel(c.programme, c.division.semester, c.division.section), viewModel)
    }
}

/** "B.E. CSE · Year 3 · Sem 5 · B". */
@Composable
private fun classLongLabel(programme: String, semester: Int, section: String): String =
    stringResource(R.string.att_class_long, programme, (semester + 1) / 2, semester, section)

/** "Period 3 · 11:30 AM – 01:10 PM". */
@Composable
private fun periodLabel(session: TeacherSession): String =
    stringResource(R.string.att_period_range, session.period, timeOfDay(session.startMinute), timeOfDay(session.endMinute))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChips(s: MarkUiState, onFilter: (StudentStatus?) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatusChip(stringResource(R.string.att_filter_all), s.students.size, null, s.filter == null) { onFilter(null) }
        StatusChip(stringResource(R.string.att_status_present), s.count(StudentStatus.PRESENT), AttGreen, s.filter == StudentStatus.PRESENT) { onFilter(StudentStatus.PRESENT) }
        StatusChip(stringResource(R.string.att_status_absent), s.count(StudentStatus.ABSENT), BadgeRed, s.filter == StudentStatus.ABSENT) { onFilter(StudentStatus.ABSENT) }
        StatusChip(stringResource(R.string.att_on_duty), s.count(StudentStatus.ON_DUTY), BrandBlue, s.filter == StudentStatus.ON_DUTY) { onFilter(StudentStatus.ON_DUTY) }
        StatusChip(stringResource(R.string.att_status_on_leave), s.count(StudentStatus.ON_LEAVE), OnboardingAmberCard, s.filter == StudentStatus.ON_LEAVE) { onFilter(StudentStatus.ON_LEAVE) }
    }
}

@Composable
private fun StatusChip(label: String, count: Int, dot: Color?, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = if (selected) BrandBlue else White, border = if (selected) null else BorderStroke(1.dp, FieldBorder)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            dot?.let {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (selected) White else it))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, color = if (selected) White else TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), color = if (selected) White else TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun StudentCard(
    student: ReportStudent,
    status: StudentStatus,
    classId: String,
    onToggle: () -> Unit,
    onDuty: (Boolean) -> Unit,
    onLeave: (Boolean) -> Unit,
) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(student.name, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.att_register_no, student.registerNo), color = TextSecondary, fontSize = AppTextSize.Body)
                    Text(stringResource(R.string.att_admission_no, admissionNo(student, classId)), color = TextSecondary, fontSize = AppTextSize.Body)
                }
                val (bg, letter) = when (status) {
                    StudentStatus.PRESENT -> AttGreen to stringResource(R.string.att_short_present)
                    StudentStatus.ABSENT -> BadgeRed to stringResource(R.string.att_short_absent)
                    StudentStatus.ON_DUTY -> BrandBlue to stringResource(R.string.att_short_on_duty)
                    StudentStatus.ON_LEAVE -> OnboardingAmberCard to stringResource(R.string.att_short_leave)
                }
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(bg).clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, color = White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FieldBorder)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.att_on_duty_title), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                Switch(status == StudentStatus.ON_DUTY, onDuty, colors = switchColors())
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.att_on_leave_title), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                Switch(status == StudentStatus.ON_LEAVE, onLeave, colors = switchColors())
            }
        }
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(checkedTrackColor = BrandBlue, uncheckedTrackColor = BadgeNeutralBackground, uncheckedBorderColor = FieldBorder)

/** "UCE24CS101" from the register number and class. */
private fun admissionNo(student: ReportStudent, classId: String): String =
    "UCE" + student.registerNo.take(4).takeLast(2) + classId.substringBefore('-').take(2).uppercase() + student.registerNo.takeLast(3)

/** "Send attendance?": class, period and topic, the counts, who's absent, then Confirm & send. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmSheet(s: MarkUiState, session: TeacherSession, classLabel: String, vm: MarkAttendanceViewModel) {
    FacultySheet(vm::closeConfirm) {
        Text(stringResource(R.string.att_send_title), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = ScreenBackground) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SummaryLine(stringResource(R.string.att_class_label), classLabel)
                HorizontalDivider(color = FieldBorder)
                SummaryLine(stringResource(R.string.att_period_label), stringResource(R.string.att_period_kind, session.period, stringResource(s.kind.label)))
                if (s.topic.isNotBlank()) {
                    HorizontalDivider(color = FieldBorder)
                    SummaryLine(stringResource(R.string.att_topic_label), s.topic)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountTile(s.count(StudentStatus.PRESENT), stringResource(R.string.att_status_present), OnboardingGreenDisc, AttGreen, Modifier.weight(1f))
            CountTile(s.count(StudentStatus.ABSENT), stringResource(R.string.att_status_absent), AlertBackground, BadgeRed, Modifier.weight(1f))
            CountTile(s.count(StudentStatus.ON_DUTY), stringResource(R.string.att_on_duty), SelectedRowBackground, BrandBlue, Modifier.weight(1f))
            CountTile(s.count(StudentStatus.ON_LEAVE), stringResource(R.string.att_status_on_leave), AmberChipBackground, OnboardingAmberCard, Modifier.weight(1f))
        }
        if (s.absentees.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.att_absent_students), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.absentees.forEach {
                    Surface(shape = RoundedCornerShape(50), color = AlertBackground) {
                        Text(it.name, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = SelectedRowBackground) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(painterResource(R.drawable.ic_info_outline), null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.att_send_note, timeOfDay(EDIT_UNTIL_MINUTE)), color = BrandBlue, fontSize = AppTextSize.Body, lineHeight = 21.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = vm::send,
            enabled = s.send != SendState.SENDING,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
        ) {
            if (s.send == SendState.SENDING) {
                CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.att_confirm_send), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            stringResource(R.string.att_review_again),
            Modifier.fillMaxWidth().clickable(onClick = vm::closeConfirm).padding(vertical = 16.dp),
            color = BrandBlue,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 12.dp)) {
        Text(label, Modifier.width(96.dp), color = TextSecondary, fontSize = AppTextSize.Body)
        Text(value, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CountTile(value: Int, label: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = background) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = content, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
            Text(label, color = content, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
