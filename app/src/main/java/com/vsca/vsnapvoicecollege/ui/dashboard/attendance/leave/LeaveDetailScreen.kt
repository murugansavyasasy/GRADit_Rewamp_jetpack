package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.leave

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.components.openAttachment
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AcademicCalendar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.MINIMUM_PERCENT
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.PercentBar
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelColor
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.levelOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentOf
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.ReportRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.studentMark
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report.totals
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.InitialsAvatar
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One leave request for the class advisor: details, the attendance it would leave, reason, attachment, decide. */
@Composable
fun LeaveDetailScreen(requestId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val requests by LeaveRepository.requests.collectAsStateWithLifecycle()
    val request = requests.firstOrNull { it.id == requestId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var rejecting by rememberSaveable { mutableStateOf(false) }
    val approvedText = request?.let { stringResource(R.string.att_leave_approved_toast, it.studentName) }.orEmpty()
    val rejectedText = request?.let { stringResource(R.string.att_leave_rejected_toast, it.studentName) }.orEmpty()
    val failedText = stringResource(R.string.att_send_failed)

    fun decide(approve: Boolean, note: String?) {
        if (busy || request == null) return
        busy = true
        scope.launch {
            val ok = try {
                LeaveRepository.decide(request.id, approve, note)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            busy = false
            Toast.makeText(context, if (!ok) failedText else if (approve) approvedText else rejectedText, Toast.LENGTH_SHORT).show()
            if (ok) {
                rejecting = false
                onBack()
            }
        }
    }

    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(title = stringResource(R.string.att_leave_detail_title), onBack = onBack)
        if (request == null) {
            Text(stringResource(R.string.att_not_found), Modifier.padding(32.dp), color = TextSecondary)
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(4.dp))
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(request.initials, size = 56.dp, background = OnboardingGreenDisc, content = AttGreen)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(request.studentName, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                        Text(request.classLongLabel, color = TextSecondary, fontSize = AppTextSize.Body)
                        Text(stringResource(R.string.att_reg_no, request.registerNo), color = TextSecondary, fontSize = AppTextSize.Body)
                    }
                    LeaveStatusPill(request.status)
                }
            }
            Spacer(Modifier.height(14.dp))
            Card {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    val full = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                    DetailLine(stringResource(R.string.att_leave_type), stringResource(request.type.label))
                    HorizontalDivider(color = FieldBorder)
                    DetailLine(stringResource(R.string.att_from), full.format(Date(request.from.millis)))
                    HorizontalDivider(color = FieldBorder)
                    DetailLine(stringResource(R.string.att_to), full.format(Date(request.to.millis)))
                    HorizontalDivider(color = FieldBorder)
                    DetailLine(stringResource(R.string.att_no_of_days), stringResource(R.string.att_n_days, request.days))
                    HorizontalDivider(color = FieldBorder)
                    DetailLine(
                        stringResource(R.string.att_applied_on_label),
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(request.appliedAt)) + ", " + clockLabel(request.appliedAt),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            AttendanceImpact(request)
            Spacer(Modifier.height(14.dp))
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.att_reason), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Text(request.reason, color = TextPrimary, fontSize = AppTextSize.BodyLarge, lineHeight = 24.sp)
                    request.decisionNote?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.att_reject_reason_shown, it), color = BadgeRed, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            request.attachment?.let { file ->
                Spacer(Modifier.height(14.dp))
                Card {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = AlertBackground) {
                            Icon(painterResource(R.drawable.ic_file), null, tint = BadgeRed, modifier = Modifier.padding(12.dp).size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(file.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.att_pdf_size, fileSizeLabel(file.sizeBytes)), color = TextSecondary, fontSize = AppTextSize.Label)
                        }
                        Surface(onClick = { openAttachment(context, file) }, shape = RoundedCornerShape(14.dp), color = SelectedRowBackground) {
                            Text(stringResource(R.string.att_view), Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (request.status == LeaveStatus.PENDING) {
            Surface(Modifier.fillMaxWidth(), color = White, shadowElevation = 8.dp) {
                DecisionButtons(busy, onReject = { rejecting = true }, onApprove = { decide(approve = true, note = null) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
    }
    if (rejecting && request != null) {
        RejectLeaveSheet(request, busy, onConfirm = { decide(approve = false, note = it) }, onDismiss = { rejecting = false })
    }
}

/** Current attendance and what it would be after the leave, against the 75% minimum. */
@Composable
private fun AttendanceImpact(request: LeaveRequest) {
    val today = Day.today()
    val student = remember(request.studentId) { ReportRepository.data().student(request.studentId) }
    if (student == null) return
    val totals = remember(student) {
        AcademicCalendar.workingDays(AcademicCalendar.semesterStart(today), today).map { studentMark(student, it, today) }.totals()
    }
    val current = totals.percent
    val after = percentOf(totals.present, totals.marked + request.days)
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.att_current_attendance), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                Text(percentLabel(current), color = levelColor(levelOf(current)), fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
            }
            PercentBar((current / 100).toFloat(), levelColor(levelOf(current)), markerAt = MINIMUM_PERCENT / 100f, height = 10.dp)
            Text(
                stringResource(if (after >= MINIMUM_PERCENT) R.string.att_after_leave_ok else R.string.att_after_leave_low, percentLabel(after)),
                color = if (after >= MINIMUM_PERCENT) TextSecondary else BadgeRed,
                fontSize = AppTextSize.Label,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) { content() }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = AppTextSize.BodyLarge)
        Spacer(Modifier.width(12.dp))
        Text(value, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}
