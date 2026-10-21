package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.report

import android.app.Application
import android.content.Context
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.NetworkMonitor
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.appTextFieldColors
import com.vsca.vsnapvoicecollege.ui.components.fileSizeLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceExport
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceTable
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.fileDate
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.mediumDate
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportFormat
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.ExportedFile
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyExport
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Parts the exported report can include. */
enum class ExportPart { SUMMARY, TREND, GROUPS, PEOPLE }

enum class ExportSendState { IDLE, WORKING, DONE, FAILED }

data class ExportShareUiState(
    val filter: ReportFilter = ReportFilter(),
    val span: DateSpan = DateSpan(Day.today(), Day.today()),
    val groupCount: Int = 0,
    val peopleCount: Int = 0,
    val departmentName: String? = null,
    val format: ExportFormat = ExportFormat.PDF,
    val parts: Set<ExportPart> = setOf(ExportPart.SUMMARY, ExportPart.TREND, ExportPart.GROUPS),
    val recipients: List<String> = emptyList(),
    val draft: String = "",
    val draftInvalid: Boolean = false,
    val state: ExportSendState = ExportSendState.IDLE,
)

/** "Export & share": format, what to include and who gets it by email. */
class ExportShareViewModel(application: Application) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(initial())
    val uiState: StateFlow<ExportShareUiState> = _ui.asStateFlow()

    private fun initial(): ExportShareUiState {
        val filter = ReportExportSession.filter
        val data = ReportRepository.data()
        val classes = data.classes.filter { c ->
            (filter.departmentId == null || c.division.departmentId == filter.departmentId) &&
                (filter.semester == null || c.division.semester == filter.semester) &&
                (filter.section == null || c.division.section == filter.section)
        }
        val groups = data.staffGroups.filter { filter.departmentId == null || it.departmentId == filter.departmentId }
        return ExportShareUiState(
            filter = filter,
            span = spanFor(filter, Day.today()),
            groupCount = if (filter.audience == Audience.STUDENTS) classes.size else groups.size,
            peopleCount = if (filter.audience == Audience.STUDENTS) classes.sumOf { it.students.size } else groups.sumOf { it.members.size },
            departmentName = data.directory.department(filter.departmentId)?.name,
            recipients = ReportRepository.defaultRecipients(filter.departmentId),
        )
    }

    fun onFormat(format: ExportFormat) = _ui.update { it.copy(format = format) }

    fun onTogglePart(part: ExportPart) = _ui.update { s -> s.copy(parts = if (part in s.parts) s.parts - part else s.parts + part) }

    fun onDraft(text: String) = _ui.update { it.copy(draft = text.trim(), draftInvalid = false) }

    fun onAddRecipient() = _ui.update { s ->
        val email = s.draft.lowercase(Locale.ROOT)
        when {
            email.isEmpty() -> s
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> s.copy(draftInvalid = true)
            email in s.recipients -> s.copy(draft = "")
            else -> s.copy(recipients = s.recipients + email, draft = "")
        }
    }

    fun onRemoveRecipient(email: String) = _ui.update { s -> s.copy(recipients = s.recipients - email) }

    fun onFailureShown() = _ui.update { it.copy(state = ExportSendState.IDLE) }

    /**
     * Saves the report to Downloads and, when [email] is set, sends it to the recipients.
     * TODO: send through the college's email API; for now the send is simulated.
     */
    fun export(email: Boolean) {
        val s = _ui.value
        if (s.state == ExportSendState.WORKING || s.parts.isEmpty()) return
        _ui.update { it.copy(state = ExportSendState.WORKING) }
        viewModelScope.launch {
            val ok = try {
                val context = getApplication<Application>()
                val table = withContext(Dispatchers.Default) { buildTable(context, s) }
                val file = withContext(Dispatchers.IO) { AttendanceExport.export(context, fileBase(s), s.format, table) }
                if (email) {
                    delay(SEND_DELAY_MS)
                    if (!NetworkMonitor.isOnline.value) throw java.io.IOException("offline")
                }
                ReportExportSession.result = ReportExportResult(
                    fileName = file.name,
                    sizeBytes = sizeOf(context, file),
                    pages = if (s.format == ExportFormat.PDF) AttendanceExport.pdfPages(table.rows.size) else 1,
                    uri = file.uri,
                    mimeType = s.format.mimeType,
                    recipients = if (email) s.recipients else emptyList(),
                )
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _ui.update { it.copy(state = if (ok) ExportSendState.DONE else ExportSendState.FAILED) }
        }
    }

    private fun sizeOf(context: Context, file: ExportedFile): Long =
        runCatching { context.contentResolver.openFileDescriptor(file.uri, "r")?.use { it.statSize } }.getOrNull() ?: 0L

    /** "Attendance_CSE_Sem5_6Oct2026". */
    private fun fileBase(s: ExportShareUiState): String = buildString {
        append("Attendance")
        if (s.filter.audience == Audience.STAFF) append("_Staff")
        s.departmentName?.let { append("_").append(it.replace(' ', '_')) }
        s.filter.semester?.let { append("_Sem").append(it) }
        s.filter.section?.let { append("_").append(it) }
        append("_").append(fileDate(s.span.from).replace("-", ""))
        if (!s.span.isSingleDay) append("-").append(fileDate(s.span.to).replace("-", ""))
    }

    private fun buildTable(context: Context, s: ExportShareUiState): AttendanceTable {
        val data = ReportRepository.data()
        val today = Day.today()
        val days = s.span.workingDays.filter { it <= today }
        val f = s.filter
        val rows = mutableListOf<List<String>>()
        fun pct(t: Totals) = String.format(Locale.US, "%.1f%%", t.percent)
        fun section(title: String) = rows.add(listOf(title.uppercase(Locale.getDefault()), "", "", ""))
        if (f.audience == Audience.STUDENTS) {
            val classes = data.classes.filter { c ->
                (f.departmentId == null || c.division.departmentId == f.departmentId) &&
                    (f.semester == null || c.division.semester == f.semester) &&
                    (f.section == null || c.division.section == f.section)
            }
            val perClass = classes.map { c -> c to days.fold(Totals()) { acc, d -> acc + c.totalsOn(d, today) } }
            if (ExportPart.SUMMARY in s.parts) {
                section(context.getString(R.string.att_part_summary))
                val total = perClass.fold(Totals()) { a, (_, t) -> a + t }
                rows += listOf(context.getString(R.string.att_overall_attendance), total.present.toString(), total.absent.toString(), pct(total))
            }
            if (ExportPart.TREND in s.parts) {
                section(context.getString(R.string.att_part_trend))
                days.forEach { d ->
                    val t = classes.fold(Totals()) { a, c -> a + c.totalsOn(d, today) }
                    rows += listOf(mediumDate(d), t.present.toString(), t.absent.toString(), pct(t))
                }
            }
            if (ExportPart.GROUPS in s.parts) {
                section(context.getString(R.string.att_part_class_list))
                perClass.forEach { (c, t) -> rows += listOf(c.shortName, t.present.toString(), t.absent.toString(), pct(t)) }
            }
            if (ExportPart.PEOPLE in s.parts) {
                section(context.getString(R.string.att_part_student_list))
                classes.forEach { c ->
                    c.students.forEach { st ->
                        val t = days.map { studentMark(st, it, today) }.totals()
                        rows += listOf("${st.name} · ${st.registerNo} · ${c.shortName}", t.present.toString(), t.absent.toString(), pct(t))
                    }
                }
            }
        } else {
            val groups = data.staffGroups.filter { f.departmentId == null || it.departmentId == f.departmentId }
            val perGroup = groups.map { g -> g to days.fold(Totals()) { acc, d -> acc + g.totalsOn(d, today) } }
            if (ExportPart.SUMMARY in s.parts) {
                section(context.getString(R.string.att_part_summary))
                val total = perGroup.fold(Totals()) { a, (_, t) -> a + t }
                rows += listOf(context.getString(R.string.att_overall_attendance), total.present.toString(), total.absent.toString(), pct(total))
            }
            if (ExportPart.TREND in s.parts) {
                section(context.getString(R.string.att_part_trend))
                days.forEach { d ->
                    val t = groups.fold(Totals()) { a, g -> a + g.totalsOn(d, today) }
                    rows += listOf(mediumDate(d), t.present.toString(), t.absent.toString(), pct(t))
                }
            }
            if (ExportPart.GROUPS in s.parts) {
                section(context.getString(R.string.att_part_group_list))
                perGroup.forEach { (g, t) -> rows += listOf(g.name, t.present.toString(), t.absent.toString(), pct(t)) }
            }
            if (ExportPart.PEOPLE in s.parts) {
                section(context.getString(R.string.att_part_staff_list))
                groups.forEach { g ->
                    g.members.forEach { m ->
                        val t = days.map { staffMark(m, it, today) }.totals()
                        rows += listOf("${m.name} · ${m.employeeId} · ${g.name}", t.present.toString(), t.absent.toString(), pct(t))
                    }
                }
            }
        }
        return AttendanceTable(
            title = reportTitle(context, s),
            subtitle = if (s.span.isSingleDay) mediumDate(s.span.from) else "${mediumDate(s.span.from)} – ${mediumDate(s.span.to)}",
            header = listOf(
                context.getString(R.string.att_col_item), context.getString(R.string.att_col_present),
                context.getString(R.string.att_col_absent), context.getString(R.string.att_col_percent),
            ),
            rows = rows,
            weights = listOf(3.2f, 1f, 1f, 1f),
        )
    }

    companion object {
        private const val SEND_DELAY_MS = 900L
    }
}

/** "Students · CSE · Semester 5". */
private fun reportTitle(context: Context, s: ExportShareUiState): String = listOfNotNull(
    context.getString(if (s.filter.audience == Audience.STUDENTS) R.string.att_students else R.string.att_staff),
    s.departmentName,
    s.filter.semester?.let { context.getString(R.string.att_semester_n, it) },
    s.filter.section?.let { context.getString(R.string.att_section_n, it) },
).joinToString(" · ")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportShareScreen(onBack: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier, viewModel: ExportShareViewModel = viewModel()) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(s.state) {
        when (s.state) {
            ExportSendState.DONE -> onDone()
            ExportSendState.FAILED -> {
                android.widget.Toast.makeText(context, R.string.att_export_send_failed, android.widget.Toast.LENGTH_LONG).show()
                viewModel.onFailureShown()
            }
            else -> Unit
        }
    }
    val students = s.filter.audience == Audience.STUDENTS
    Column(modifier.fillMaxSize().background(ScreenBackground).imePadding()) {
        TitledTopBar(title = stringResource(R.string.att_export_share_title), onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = SelectedRowBackground) {
                Column(Modifier.padding(18.dp)) {
                    Text(stringResource(R.string.att_report_label).uppercase(Locale.getDefault()), color = BrandBlue, fontSize = AppTextSize.Label, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                    Text(reportTitle(context, s), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                    Text(
                        stringResource(
                            R.string.att_report_scope,
                            spanLabel(s.span),
                            pluralStringResource(if (students) R.plurals.att_classes else R.plurals.att_groups, s.groupCount, s.groupCount),
                            pluralStringResource(if (students) R.plurals.att_n_students else R.plurals.att_staff_members, s.peopleCount, s.peopleCount),
                        ),
                        color = TextSecondary,
                        fontSize = AppTextSize.Body,
                    )
                }
            }
            Label(stringResource(R.string.att_format))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormatCard("PDF", stringResource(R.string.att_export_pdf), stringResource(R.string.att_format_pdf_sub), s.format == ExportFormat.PDF, Modifier.weight(1f)) { viewModel.onFormat(ExportFormat.PDF) }
                FormatCard("XLS", stringResource(R.string.att_format_excel), stringResource(R.string.att_format_excel_sub), s.format == ExportFormat.EXCEL, Modifier.weight(1f)) { viewModel.onFormat(ExportFormat.EXCEL) }
            }
            Label(stringResource(R.string.att_include))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    val parts = listOf(
                        ExportPart.SUMMARY to stringResource(R.string.att_part_summary),
                        ExportPart.TREND to stringResource(R.string.att_part_trend),
                        ExportPart.GROUPS to stringResource(if (students) R.string.att_part_class_list else R.string.att_part_group_list),
                        ExportPart.PEOPLE to stringResource(if (students) R.string.att_part_student_rows else R.string.att_part_staff_rows, s.peopleCount),
                    )
                    parts.forEachIndexed { index, (part, label) ->
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 8.dp), color = FieldBorder)
                        Row(Modifier.fillMaxWidth().clickable { viewModel.onTogglePart(part) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = part in s.parts,
                                onCheckedChange = { viewModel.onTogglePart(part) },
                                colors = CheckboxDefaults.colors(checkedColor = BrandBlue, uncheckedColor = TextSecondary, checkmarkColor = White),
                            )
                            Text(label, color = TextPrimary, fontSize = AppTextSize.BodyLarge)
                        }
                    }
                }
            }
            Label(stringResource(R.string.att_share_by_email))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.recipients.forEach { email ->
                    Surface(onClick = { viewModel.onRemoveRecipient(email) }, shape = RoundedCornerShape(50), color = SelectedRowBackground) {
                        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(email, color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.att_remove_recipient, email), tint = BrandBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            OutlinedTextField(
                value = s.draft,
                onValueChange = viewModel::onDraft,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.att_add_email), color = TextSecondary) },
                singleLine = true,
                isError = s.draftInvalid,
                supportingText = if (s.draftInvalid) ({ Text(stringResource(R.string.att_email_invalid), color = BadgeRed) }) else null,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.onAddRecipient() }),
                trailingIcon = if (s.draft.isNotEmpty()) ({
                    Text(stringResource(R.string.att_add), Modifier.clip(RoundedCornerShape(8.dp)).clickable { viewModel.onAddRecipient() }.padding(10.dp), color = BrandBlue, fontWeight = FontWeight.Bold)
                }) else null,
                colors = appTextFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
        }
        val working = s.state == ExportSendState.WORKING
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { viewModel.export(email = true) },
                enabled = !working && s.recipients.isNotEmpty() && s.parts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                if (working) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = White, strokeWidth = 2.5.dp)
                } else {
                    Icon(painterResource(R.drawable.ic_mail_outline), null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.att_export_and_email), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick = { viewModel.export(email = false) },
                enabled = !working && s.parts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrandBlue),
            ) {
                Icon(painterResource(R.drawable.ic_download), null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.att_download_only), color = BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun FormatCard(badge: String, title: String, subtitle: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) SelectedRowBackground else White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BrandBlue else FieldBorder),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) BrandBlue else BadgeNeutralBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, color = if (selected) White else TextPrimary, fontSize = AppTextSize.Label, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = TextSecondary, fontSize = AppTextSize.Label)
            }
        }
    }
}

/** "Report exported": the file, who it went to, and Open file / Back to report. */
@Composable
fun ExportCompleteScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val result = ReportExportSession.result
    val context = LocalContext.current
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(title = stringResource(R.string.att_export_complete), onBack = onBack)
        if (result == null) {
            Text(stringResource(R.string.att_not_found), Modifier.padding(32.dp), color = TextSecondary)
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Box(Modifier.size(96.dp).clip(CircleShape).background(OnboardingGreenDisc), contentAlignment = Alignment.Center) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(AttGreen), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = White, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.att_report_exported), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (result.recipients.isEmpty()) {
                    stringResource(R.string.att_report_saved_only)
                } else {
                    pluralStringResource(R.plurals.att_report_saved_emailed, result.recipients.size, result.recipients.size)
                },
                color = TextSecondary,
                fontSize = AppTextSize.BodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val pdf = result.mimeType.endsWith("pdf")
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(if (pdf) AlertBackground else OnboardingGreenDisc), contentAlignment = Alignment.Center) {
                        Text(if (pdf) "PDF" else "XLS", color = if (pdf) BadgeRed else AttGreen, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(result.fileName, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.att_file_size_pages, fileSizeLabel(result.sizeBytes), pluralStringResource(R.plurals.att_pages, result.pages, result.pages)),
                            color = TextSecondary,
                            fontSize = AppTextSize.Label,
                        )
                    }
                }
            }
            if (result.recipients.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        result.recipients.forEachIndexed { index, email ->
                            if (index > 0) HorizontalDivider(color = FieldBorder)
                            Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_check), null, tint = AttGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(email, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.Body)
                                Text(stringResource(R.string.att_sent), color = AttGreen, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { FacultyExport.open(context, ExportedFile(result.fileName, result.uri, if (result.mimeType.endsWith("pdf")) ExportFormat.PDF else ExportFormat.EXCEL, 0, 0L)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Text(stringResource(R.string.att_open_file), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BrandBlue)) {
                Text(stringResource(R.string.att_back_to_report), color = BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
