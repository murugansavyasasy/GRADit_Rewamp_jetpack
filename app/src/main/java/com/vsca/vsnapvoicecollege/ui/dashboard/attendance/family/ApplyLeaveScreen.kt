package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.family

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.FileAttachment
import com.vsca.vsnapvoicecollege.ui.components.IMAGE_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.PDF_MIME_TYPES
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.dashedBorder
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.pickDate
import com.vsca.vsnapvoicecollege.ui.components.readAttachment
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveRequest
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave.LeaveType
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelBackground
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelColor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportClass
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportStudent
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.InitialsAvatar
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ApplyState { IDLE, SENDING, SENT, FAILED }

data class ApplyLeaveUiState(
    val student: ReportStudent? = null,
    val reportClass: ReportClass? = null,
    val percent: Double = 0.0,
    val type: LeaveType = LeaveType.CASUAL,
    val from: Day = AcademicCalendar.workingDays(Day.today().plusDays(1), Day.today().plusDays(10)).firstOrNull() ?: Day.today(),
    val to: Day = from,
    val reason: String = "",
    val attachment: FileAttachment? = null,
    val showErrors: Boolean = false,
    val state: ApplyState = ApplyState.IDLE,
) {
    val workingDays: List<Day> get() = AcademicCalendar.workingDays(from, to)
    val reasonMissing: Boolean get() = reason.trim().length < MIN_REASON
    val noWorkingDays: Boolean get() = workingDays.isEmpty()
    val canSubmit: Boolean get() = !reasonMissing && !noWorkingDays

    companion object {
        const val MAX_REASON = 250
        const val MIN_REASON = 5
    }
}

/** "Apply leave" for a student, sent to the class advisor. */
class ApplyLeaveViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _ui = MutableStateFlow(ApplyLeaveUiState())
    val uiState: StateFlow<ApplyLeaveUiState> = _ui.asStateFlow()

    init {
        val data = ReportRepository.data()
        val student = data.student(savedStateHandle.get<String>(ARG_STUDENT_ID).orEmpty())
        val reportClass = student?.let { data.classById(it.classId) }
        if (student != null && reportClass != null) {
            val today = Day.today()
            val child = FamilyChild(student, reportClass)
            val marks = AcademicCalendar.workingDays(AcademicCalendar.semesterStart(today), today).map { dayKind(child, it, today) }
            _ui.update {
                it.copy(
                    student = student,
                    reportClass = reportClass,
                    percent = percentOf(marks.count { k -> k == DayKind.PRESENT || k == DayKind.PARTLY }, marks.size),
                )
            }
        }
    }

    fun onType(type: LeaveType) = _ui.update { it.copy(type = type) }

    /** Picking a From after To moves To along with it. */
    fun onFrom(day: Day) = _ui.update { it.copy(from = day, to = if (it.to < day) day else it.to) }

    fun onTo(day: Day) = _ui.update { it.copy(to = if (day < it.from) it.from else day) }

    fun onReason(text: String) = _ui.update { it.copy(reason = text.take(ApplyLeaveUiState.MAX_REASON)) }

    fun onAttachment(file: FileAttachment?) = _ui.update { it.copy(attachment = file) }

    fun submit() {
        val s = _ui.value
        val student = s.student ?: return
        val c = s.reportClass ?: return
        if (!s.canSubmit) {
            _ui.update { it.copy(showErrors = true) }
            return
        }
        if (s.state == ApplyState.SENDING) return
        _ui.update { it.copy(state = ApplyState.SENDING) }
        viewModelScope.launch {
            val result = try {
                LeaveRepository.submit(
                    LeaveRequest(
                        id = LeaveRepository.newId(),
                        studentId = student.id,
                        studentName = student.name,
                        initials = student.initials,
                        classLabel = "${c.programme} · Sem ${c.division.semester} ${c.division.section}",
                        classLongLabel = "${c.programme} · Year ${(c.division.semester + 1) / 2} · Sem ${c.division.semester} ${c.division.section}",
                        classId = c.id,
                        registerNo = student.registerNo,
                        type = s.type,
                        from = s.from,
                        to = s.to,
                        appliedAt = System.currentTimeMillis(),
                        reason = s.reason.trim(),
                        attachment = s.attachment,
                    ),
                )
                ApplyState.SENT
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ApplyState.FAILED
            }
            _ui.update { it.copy(state = result) }
        }
    }

    fun onFailureShown() = _ui.update { it.copy(state = ApplyState.IDLE) }

    companion object {
        const val ARG_STUDENT_ID = "studentId"
    }
}

@Composable
fun ApplyLeaveScreen(onBack: () -> Unit, onSent: () -> Unit, modifier: Modifier = Modifier, viewModel: ApplyLeaveViewModel = viewModel()) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val advisor = s.reportClass?.mentor
    val sentText = advisor?.let { stringResource(R.string.att_leave_sent_to, it) } ?: stringResource(R.string.att_leave_sent)
    val failedText = stringResource(R.string.att_send_failed)
    LaunchedEffect(s.state) {
        when (s.state) {
            ApplyState.SENT -> {
                Toast.makeText(context, sentText, Toast.LENGTH_SHORT).show()
                onSent()
            }
            ApplyState.FAILED -> {
                Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
                viewModel.onFailureShown()
            }
            else -> Unit
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readAttachment(context, it) }?.let(viewModel::onAttachment)
    }
    val student = s.student
    val c = s.reportClass
    Column(modifier.fillMaxSize().background(ScreenBackground).imePadding()) {
        TitledTopBar(title = stringResource(R.string.att_apply_leave), onBack = onBack)
        if (student == null || c == null) {
            Text(stringResource(R.string.att_not_found), Modifier.padding(32.dp), color = TextSecondary)
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(2.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(student.initials, size = 48.dp, background = SelectedRowBackground, content = BrandBlue)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(student.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(R.string.att_class_reg, "${c.programme} · Sem ${c.division.semester} ${c.division.section}", student.registerNo), color = TextSecondary, fontSize = AppTextSize.Label)
                    }
                    val level = levelOf(s.percent)
                    Surface(shape = RoundedCornerShape(50), color = levelBackground(level)) {
                        Text(percentLabel(s.percent), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = levelColor(level), fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            FieldLabel(stringResource(R.string.att_leave_type))
            val typeLabels = LeaveType.entries.map { stringResource(it.label) }
            DropdownField(stringResource(s.type.label), typeLabels, { picked -> viewModel.onType(LeaveType.entries[typeLabels.indexOf(picked)]) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(stringResource(R.string.att_from), s.from, Modifier.weight(1f)) { pickDate(context) { viewModel.onFrom(Day.of(it)) } }
                DateField(stringResource(R.string.att_to), s.to, Modifier.weight(1f)) { pickDate(context) { viewModel.onTo(Day.of(it)) } }
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = if (s.noWorkingDays) AlertBackground else SelectedRowBackground) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_info_outline), null, tint = if (s.noWorkingDays) BadgeRed else BrandBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.att_no_of_days_n, s.workingDays.size), color = if (s.noWorkingDays) BadgeRed else BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (s.noWorkingDays) {
                                stringResource(R.string.att_no_working_days)
                            } else {
                                stringResource(R.string.att_days_note, daysSummary(s.workingDays))
                            },
                            color = if (s.noWorkingDays) BadgeRed else BrandBlue,
                            fontSize = AppTextSize.Body,
                        )
                    }
                }
            }
            FieldLabel(stringResource(R.string.att_reason))
            OutlinedTextField(
                value = s.reason,
                onValueChange = viewModel::onReason,
                modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                placeholder = { Text(stringResource(R.string.att_reason_hint), color = TextSecondary) },
                isError = s.showErrors && s.reasonMissing,
                shape = RoundedCornerShape(16.dp),
                colors = appTextFieldColors(),
                supportingText = {
                    Row(Modifier.fillMaxWidth()) {
                        if (s.showErrors && s.reasonMissing) Text(stringResource(R.string.att_reason_required), Modifier.weight(1f), color = BadgeRed)
                        Spacer(Modifier.weight(1f))
                        Text("${s.reason.length} / ${ApplyLeaveUiState.MAX_REASON}", color = TextSecondary, textAlign = TextAlign.End)
                    }
                },
            )
            Row {
                FieldLabel(stringResource(R.string.att_attach_document))
                Text(" " + stringResource(R.string.att_optional), color = TextSecondary, fontSize = AppTextSize.BodyLarge)
            }
            val file = s.attachment
            if (file == null) {
                Surface(
                    onClick = { picker.launch(IMAGE_MIME_TYPES + PDF_MIME_TYPES) },
                    modifier = Modifier.fillMaxWidth().dashedBorder(BrandBlue.copy(alpha = 0.5f), 18.dp, 1.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = White,
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(painterResource(R.drawable.ic_upload), null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.att_attach_hint), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                    Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_file), null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(file.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                            Text(fileSizeLabel(file.sizeBytes), color = TextSecondary, fontSize = AppTextSize.Label)
                        }
                        IconButton(onClick = { viewModel.onAttachment(null) }) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.att_remove_document), tint = BadgeRed)
                        }
                    }
                }
            }
            Text(
                advisor?.let { stringResource(R.string.att_advisor_note, it) } ?: stringResource(R.string.att_advisor_note_generic),
                color = TextSecondary,
                fontSize = AppTextSize.Body,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(8.dp))
        }
        Surface(Modifier.fillMaxWidth(), color = White, shadowElevation = 8.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, FieldBorder)) {
                    Text(stringResource(R.string.att_cancel), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = viewModel::submit,
                    enabled = s.state != ApplyState.SENDING,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                ) {
                    if (s.state == ApplyState.SENDING) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.att_submit_request), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun DateField(label: String, day: Day, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier) {
        FieldLabel(label)
        Spacer(Modifier.height(8.dp))
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(day.millis)), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge)
                Icon(painterResource(R.drawable.ic_calendar), null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** "Friday", "Mon – Wed" or "12 Oct – 16 Oct". */
private fun daysSummary(days: List<Day>): String {
    val weekday = SimpleDateFormat("EEEE", Locale.getDefault())
    val short = SimpleDateFormat("EEE dd MMM", Locale.getDefault())
    return when (days.size) {
        0 -> ""
        1 -> weekday.format(Date(days[0].millis))
        else -> short.format(Date(days.first().millis)) + " – " + short.format(Date(days.last().millis))
    }
}

