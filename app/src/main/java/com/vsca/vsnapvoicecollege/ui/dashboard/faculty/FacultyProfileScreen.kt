package com.vsca.vsnapvoicecollege.ui.dashboard.faculty

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.ConfirmDialog
import com.vsca.vsnapvoicecollege.ui.components.DialogButtons
import com.vsca.vsnapvoicecollege.ui.components.DialogCard
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** Square swatches beside each subject, cycling through these. */
private val subjectSwatches = listOf(
    Color(0xFFDDE5FC) to BrandBlue,
    PresentBackground to PresentGreen,
    Tiles.amberBg to OnboardingAmberCard,
)

@Composable
fun FacultyProfileScreen(
    onBack: () -> Unit,
    onOpenFreePeriods: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacultyProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val d = uiState.directory
    val f = uiState.faculty

    val noticeText = when (uiState.notice) {
        ProfileNotice.CLASS_TEACHER_ASSIGNED -> stringResource(R.string.faculty_ct_assigned)
        ProfileNotice.HOD_CHANGED -> stringResource(R.string.faculty_hod_changed)
        null -> null
    }
    LaunchedEffect(noticeText) {
        if (noticeText != null) {
            Toast.makeText(context, noticeText, Toast.LENGTH_SHORT).show()
            viewModel.onNoticeShown()
        }
    }

    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        val dept = d.department(f?.departmentId)
        TitledTopBar(
            title = stringResource(R.string.faculty_profile_title),
            subtitle = dept?.let { stringResource(R.string.faculty_dept_in_stream, it.name, d.stream(it.streamId)?.name.orEmpty()) },
            onBack = onBack,
        )
        if (f == null || dept == null) {
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
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IdentityCard(f, d, onCall = {
                try {
                    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${f.phone.replace(" ", "")}".toUri()))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.faculty_export_no_app, Toast.LENGTH_SHORT).show()
                }
            })
            TodayCard(f, d, onOpenFreePeriods)
            SubjectsCard(f, d)
            if (viewModel.canManage) {
                FacultyCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.faculty_manage), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                        Button(
                            onClick = viewModel::onAssignClassTeacher,
                            enabled = viewModel.teachableDivisions(d).isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                        ) {
                            Icon(painterResource(R.drawable.ic_star), null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.faculty_assign_ct), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                        }
                        if (!f.isHod) {
                            OutlinedButton(
                                onClick = viewModel::onMakeHod,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, FieldBorder),
                            ) {
                                Icon(painterResource(R.drawable.ic_crown), null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.faculty_make_hod), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    when (val dialog = uiState.dialog) {
        is ProfileDialog.AssignClassTeacher -> if (f != null) AssignDialog(f, d, viewModel.teachableDivisions(d), dialog.divisionId, viewModel)
        ProfileDialog.MakeHod -> if (f != null) {
            val dept = d.department(f.departmentId)
            val current = d.hodOf(f.departmentId)
            ConfirmDialog(
                title = stringResource(R.string.faculty_hod_confirm_title, f.name),
                body = if (current != null) {
                    stringResource(R.string.faculty_hod_confirm_body, f.name, dept?.name.orEmpty(), d.stream(dept?.streamId)?.name.orEmpty(), current.name)
                } else {
                    stringResource(R.string.faculty_hod_confirm_body_new, f.name, dept?.name.orEmpty(), d.stream(dept?.streamId)?.name.orEmpty())
                },
                confirm = stringResource(R.string.faculty_confirm),
                destructive = false,
                onDismiss = viewModel::onDismissDialog,
                onConfirm = viewModel::onConfirmHod,
            )
        }
        null -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityCard(f: Faculty, d: FacultyDirectory, onCall: () -> Unit) {
    val dept = d.department(f.departmentId)
    FacultyCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(f.initials, size = 84.dp)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(f.name, color = TextPrimary, fontSize = AppTextSize.H3, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                    Text(f.designation, color = TextPrimary, fontSize = AppTextSize.BodyLarge)
                    val ct = d.classTeacherOf(f.id).isNotEmpty()
                    if (f.isHod || ct) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (f.isHod) HodBadge()
                            if (ct) ClassTeacherBadge()
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = FieldBorder)
            DetailRow(stringResource(R.string.faculty_employee_id), f.employeeId)
            HorizontalDivider(color = FieldBorder)
            val also = f.alsoDepartmentIds.mapNotNull(d::department).joinToString(", ") { it.name }
            DetailRow(
                stringResource(R.string.faculty_department),
                if (also.isEmpty()) dept?.name.orEmpty() else stringResource(R.string.faculty_department_also, dept?.name.orEmpty(), also),
            )
            HorizontalDivider(color = FieldBorder)
            DetailRow(stringResource(R.string.faculty_stream), d.stream(dept?.streamId)?.name.orEmpty())
            HorizontalDivider(color = FieldBorder)
            DetailRow(stringResource(R.string.faculty_qualification), f.qualification)
            HorizontalDivider(color = FieldBorder)
            DetailRow(stringResource(R.string.faculty_experience), pluralStringResource(R.plurals.faculty_years, f.experienceYears, f.experienceYears))
            HorizontalDivider(color = FieldBorder)
            Row(Modifier.fillMaxWidth().clickable(onClick = onCall).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.faculty_phone), Modifier.weight(0.42f), color = TextSecondary, fontSize = AppTextSize.BodyLarge)
                Row(Modifier.weight(0.58f), verticalAlignment = Alignment.CenterVertically) {
                    Text(f.phone, Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Icon(painterResource(R.drawable.ic_phone), stringResource(R.string.faculty_call, f.name), tint = BrandBlue, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun TodayCard(f: Faculty, d: FacultyDirectory, onOpenFreePeriods: () -> Unit) {
    val p = f.presence
    FacultyCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(todayHeading(), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                PresencePill(p)
            }
            IconText(
                R.drawable.ic_fingerprint,
                when (p.status) {
                    PresenceStatus.PRESENT -> stringResource(R.string.faculty_punched_in, timeOfDay(p.punchMinute ?: 0), p.punchMethod ?: stringResource(R.string.faculty_punch_default))
                    PresenceStatus.ON_LEAVE -> stringResource(R.string.faculty_punch_not_needed)
                    PresenceStatus.ABSENT -> stringResource(R.string.faculty_punch_missing, timeOfDay(9 * 60 + 15))
                    PresenceStatus.NOT_IN -> stringResource(R.string.faculty_punch_waiting)
                },
            )
            val created = d.freePeriodsCreatedBy(f.id)
            if (p.status.isAway) {
                Text(
                    if (p.status == PresenceStatus.ON_LEAVE) {
                        pluralStringResource(R.plurals.faculty_leave_today, created.size, p.leaveReason ?: stringResource(R.string.faculty_leave_default), created.size)
                    } else {
                        pluralStringResource(R.plurals.faculty_absent_today_body, created.size, created.size)
                    },
                    color = TextSecondary,
                    fontSize = AppTextSize.BodyLarge,
                )
            }
            if (created.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(18.dp), color = Tiles.amberBg, border = BorderStroke(1.dp, Tiles.amberBorder)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            pluralStringResource(R.plurals.faculty_created_free, created.size, created.size),
                            color = OnboardingAmberCard,
                            fontSize = AppTextSize.BodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        created.forEach { fp ->
                            val division = d.division(fp.divisionId)
                            Row(verticalAlignment = Alignment.Top) {
                                Text(stringResource(R.string.faculty_period_short, fp.period), Modifier.width(40.dp), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    division?.let { stringResource(R.string.faculty_class_label, it.programme, it.semester, it.section) }.orEmpty(),
                                    Modifier.weight(1f),
                                    color = TextPrimary,
                                    fontSize = AppTextSize.Body,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    d.faculty(fp.allocatedToId)?.let { stringResource(R.string.faculty_allocated_to, it.name) } ?: stringResource(R.string.faculty_unallocated),
                                    Modifier.weight(1.2f),
                                    color = TextPrimary,
                                    fontSize = AppTextSize.Body,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                )
                            }
                        }
                        Row(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenFreePeriods), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.faculty_open_free_periods),
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
        }
    }
}

/** "Today · Wed, 07 Oct". */
@Composable
internal fun todayHeading(): String = stringResource(
    R.string.faculty_today_heading,
    java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale.getDefault()).format(java.util.Date()),
)

@Composable
private fun SubjectsCard(f: Faculty, d: FacultyDirectory) {
    if (f.teaching.isEmpty()) return
    FacultyCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.faculty_divisions_subjects), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                Text(pluralStringResource(R.plurals.faculty_periods_week, f.periodsPerWeek, f.periodsPerWeek), color = BrandBlue, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            }
            f.teaching.forEachIndexed { i, t ->
                val division = d.division(t.divisionId)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = FieldBorder)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (bg, fg) = subjectSwatches[i % subjectSwatches.size]
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(bg).border(1.5.dp, fg, RoundedCornerShape(5.dp)))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(t.subject, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.Bold)
                        division?.let { Text(stringResource(R.string.faculty_division_label, it.programme, it.semester, it.section), color = TextSecondary, fontSize = AppTextSize.Body) }
                        if (division?.classTeacherId == f.id) ClassTeacherBadge()
                    }
                    Text(stringResource(R.string.faculty_per_week_short, t.periodsPerWeek), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AssignDialog(f: Faculty, d: FacultyDirectory, divisions: List<Division>, selected: String?, vm: FacultyProfileViewModel) {
    DialogCard(onDismiss = vm::onDismissDialog) {
        Text(stringResource(R.string.faculty_assign_ct_title), color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.faculty_assign_ct_body, f.name), color = TextSecondary, fontSize = AppTextSize.BodyLarge)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            divisions.forEach { division ->
                val current = division.classTeacherId == f.id
                val currentName = d.faculty(division.classTeacherId)?.name
                RadioCard(
                    title = stringResource(R.string.faculty_division_label, division.programme, division.semester, division.section),
                    subtitle = when {
                        current -> stringResource(R.string.faculty_already_ct)
                        currentName != null -> stringResource(R.string.faculty_current_ct, currentName)
                        else -> stringResource(R.string.faculty_no_ct)
                    },
                    selected = division.id == selected,
                    enabled = !current,
                    onClick = { vm.onPickDivision(division.id) },
                    trailing = if (current) {
                        { Surface(shape = RoundedCornerShape(50), color = PresentBackground) { Text(stringResource(R.string.faculty_current), Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = PresentGreen, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold) } }
                    } else {
                        null
                    },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        DialogButtons(
            confirm = stringResource(R.string.faculty_assign),
            confirmEnabled = selected != null,
            onDismiss = vm::onDismissDialog,
            onConfirm = vm::onConfirmAssign,
        )
    }
}
