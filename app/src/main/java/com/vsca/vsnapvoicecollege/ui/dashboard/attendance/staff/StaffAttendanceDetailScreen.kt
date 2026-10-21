package com.vsca.vsnapvoicecollege.ui.dashboard.attendance.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.DropdownField
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.dashedBorder
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttGreen
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttStatTile
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.AttendanceFooter
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.Day
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.YearMonth
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthName
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.monthYear
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.percentLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.attendance.weekdayHeaders
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.FacultyRepository
import com.vsca.vsnapvoicecollege.ui.dashboard.faculty.InitialsAvatar
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.DashedRing
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberRing
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.util.Calendar

/** One staff member's month: today's status, counts and a day-by-day calendar. */
@Composable
fun StaffAttendanceDetailScreen(staffId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val directory by FacultyRepository.directory.collectAsStateWithLifecycle()
    val staff = directory.faculty(staffId)
    val today = Day.today()
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.month) }
    val ym = YearMonth(year, month)
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.att_staff_detail_title),
            subtitle = staff?.let { stringResource(R.string.att_name_period, it.name, monthYear(ym)) },
            onBack = onBack,
        )
        if (staff == null) {
            Text(stringResource(R.string.att_staff_not_found), Modifier.padding(32.dp), color = TextSecondary)
            return@Column
        }
        val days = ym.days.filter { it <= today }
        val summary = staffSummary(staff, days, today)
        val group = todayGroupOf(staff)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Profile + today
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(staff.initials, size = 72.dp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(staff.name, color = TextPrimary, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
                            Text(staff.designation, color = TextSecondary, fontSize = AppTextSize.Body)
                            val department = directory.department(staff.departmentId)
                            Text(
                                listOfNotNull(department?.name, directory.stream(department?.streamId)?.name, staff.employeeId).joinToString(" · "),
                                color = TextSecondary,
                                fontSize = AppTextSize.Label,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = ScreenBackground) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.att_today_label), color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            TodayPill(group)
                            Spacer(Modifier.width(10.dp))
                            Text(todayDetail(staff, group), color = TextSecondary, fontSize = AppTextSize.Label, lineHeight = 18.sp)
                        }
                    }
                }
            }
            // Year / month
            val years = listOf(today.year - 1, today.year)
            val months = if (year < today.year) (1..12).toList() else (1..today.month).toList()
            val current = stringResource(R.string.att_month_current, monthName(YearMonth(today.year, today.month)))
            fun label(m: Int) = if (year == today.year && m == today.month) current else monthName(YearMonth(year, m))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(0.42f)) {
                    Text(stringResource(R.string.att_year), color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    DropdownField(year.toString(), years.map { it.toString() }, { picked ->
                        year = picked.toInt()
                        if (year == today.year && month > today.month) month = today.month
                    })
                }
                Column(Modifier.weight(0.58f)) {
                    Text(stringResource(R.string.att_month), color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    DropdownField(label(month), months.map(::label), { picked -> month = months[months.map(::label).indexOf(picked)] })
                }
            }
            // Counts
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AttStatTile(summary.present.toString(), stringResource(R.string.att_status_present), Modifier.weight(1f), OnboardingGreenDisc, AttGreen)
                    AttStatTile(summary.absent.toString(), stringResource(R.string.att_status_absent), Modifier.weight(1f), AlertBackground, BadgeRed)
                    AttStatTile(summary.leave.toString(), stringResource(R.string.att_leave), Modifier.weight(1f), AmberChipBackground, OnboardingAmberCard)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AttStatTile(summary.late.toString(), stringResource(R.string.att_status_late), Modifier.weight(1f))
                    AttStatTile(summary.workingDays.toString(), stringResource(R.string.att_working_days), Modifier.weight(1f))
                    AttStatTile(percentLabel(summary.percent), stringResource(R.string.att_attendance), Modifier.weight(1f), SelectedRowBackground, BrandBlue)
                }
            }
            // Calendar
            Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                Column(Modifier.padding(16.dp)) {
                    Text(monthYear(ym), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        weekdayHeaders(Calendar.SUNDAY, "EEE").forEach {
                            Text(it, Modifier.weight(1f), color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val lead = ym.first.dayOfWeek - Calendar.SUNDAY
                    val cells: List<Day?> = List(lead) { null } + ym.days
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (0 until 7).forEach { i ->
                                val day = week.getOrNull(i)
                                Box(Modifier.weight(1f)) {
                                    if (day != null) StaffDayCell(day, staffDayStatus(staff, day, today))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.att_staff_calendar_legend), color = TextSecondary, fontSize = AppTextSize.Caption, lineHeight = 17.sp)
                }
            }
            AttendanceFooter()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Day number over P / A / L / H / W; late days get an amber outline, later days stay faint. */
@Composable
private fun StaffDayCell(day: Day, status: StaffDayStatus) {
    val (background, content) = when (status) {
        StaffDayStatus.PRESENT, StaffDayStatus.LATE -> OnboardingGreenDisc to AttGreen
        StaffDayStatus.ABSENT -> AlertBackground to BadgeRed
        StaffDayStatus.LEAVE -> AmberChipBackground to OnboardingAmberCard
        StaffDayStatus.HOLIDAY -> SelectedRowBackground to BrandBlue
        StaffDayStatus.SUNDAY, StaffDayStatus.VACATION -> BadgeNeutralBackground to TextSecondary
        StaffDayStatus.NOT_IN -> White to TextSecondary
        StaffDayStatus.UPCOMING -> White to TextMuted
    }
    val letter = when (status) {
        StaffDayStatus.PRESENT, StaffDayStatus.LATE -> stringResource(R.string.att_short_present)
        StaffDayStatus.ABSENT -> stringResource(R.string.att_short_absent)
        StaffDayStatus.LEAVE -> stringResource(R.string.att_short_leave)
        StaffDayStatus.HOLIDAY -> stringResource(R.string.att_short_holiday)
        StaffDayStatus.SUNDAY -> stringResource(R.string.att_short_weekly_off)
        StaffDayStatus.VACATION -> stringResource(R.string.att_short_vacation)
        StaffDayStatus.NOT_IN, StaffDayStatus.UPCOMING -> null
    }
    val shape = RoundedCornerShape(12.dp)
    val border: Modifier = when (status) {
        StaffDayStatus.LATE -> Modifier.background(background, shape).border(2.dp, OnboardingAmberRing, shape)
        StaffDayStatus.SUNDAY -> Modifier.background(background, shape).dashedBorder(DashedRing, 12.dp, 1.dp)
        StaffDayStatus.UPCOMING, StaffDayStatus.NOT_IN -> Modifier.background(background, shape).border(1.dp, FieldBorder, shape)
        else -> Modifier.background(background, shape)
    }
    Column(
        Modifier.fillMaxWidth().aspectRatio(0.9f).then(border),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(day.day.toString(), color = content, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold)
        letter?.let { Text(it, color = content, fontSize = AppTextSize.Body, fontWeight = FontWeight.ExtraBold) }
    }
}
