package com.vsca.vsnapvoicecollege.ui.dashboard.placement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.data.SessionPreferences
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/** One placement event: when and where, actions, who in the family can attend, companies and criteria. */
@Composable
fun PlacementEventDetailScreen(eventId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val events by PlacementRepository.events.collectAsStateWithLifecycle()
    val event = events.firstOrNull { it.id == eventId }
    val context = LocalContext.current
    val role = remember { SessionPreferences.getRole(context) }
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        TitledTopBar(
            title = stringResource(R.string.placement_detail_title),
            subtitle = stringResource(R.string.placement_subtitle),
            onBack = onBack,
        )
        if (event == null) {
            Text(stringResource(R.string.placement_not_found), Modifier.padding(32.dp), color = TextSecondary)
        } else {
            PlacementEventDetailContent(event, role, PlacementRepository.familyFor(role))
        }
    }
}

@Composable
private fun PlacementEventDetailContent(event: PlacementEvent, role: Role, family: List<PlacementChild>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        SummaryCard(event)
        if (family.isNotEmpty()) FamilyEligibilityCard(event, role, family)
        DetailCard(
            if (event.companies.size == 1) {
                stringResource(R.string.placement_company)
            } else {
                stringResource(R.string.placement_participating_companies, event.companies.size)
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { event.companies.forEach { CompanyRow(it) } }
        }
        DetailCard(stringResource(R.string.placement_eligibility)) {
            FieldCaption(stringResource(R.string.placement_eligible_courses))
            Spacer(Modifier.height(8.dp))
            CourseChips(event.eligibleCourses)
            if (event.criteria.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                FieldCaption(stringResource(R.string.placement_other_criteria))
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    event.criteria.forEach { criterion ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(TextPrimary))
                            Spacer(Modifier.width(10.dp))
                            Text(criterion, color = TextPrimary, fontSize = AppTextSize.Body, lineHeight = 21.sp)
                        }
                    }
                }
            }
        }
        PlacementFooter(Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
    }
}

/** Pills, title, the when / where box and the actions (none once the event has ended). */
@Composable
private fun SummaryCard(event: PlacementEvent) {
    val context = LocalContext.current
    val time = timeRangeLabel(event)
    val ended = event.hasEnded()
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(18.dp)) {
            EventPills(event)
            Spacer(Modifier.height(12.dp))
            Text(event.title, color = TextPrimary, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(event.subtitle, color = TextSecondary, fontSize = AppTextSize.Body, lineHeight = 22.sp)
            Spacer(Modifier.height(14.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = ScreenBackground) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // "Sun, 18 Oct 2026 · In 11 days"; just the date once it has ended.
                    val relative = if (ended) null else relativeDayLabel(event.startAt)
                    EventInfoLine(R.drawable.ic_calendar, relative.orEmpty(), bold = fullDate(event.startAt))
                    EventInfoLine(R.drawable.ic_clock, time)
                    EventInfoLine(if (event.mode.isInPerson) R.drawable.ic_place_outline else R.drawable.ic_videocam_outline, event.venue)
                }
            }
            if (!ended) {
                Spacer(Modifier.height(14.dp))
                if (event.mode.isInPerson) {
                    PrimaryOutlinedAction(R.drawable.ic_map_outline, stringResource(R.string.placement_open_maps)) { openInMaps(context, event) }
                } else if (event.joinUrl != null) {
                    PrimaryOutlinedAction(
                        R.drawable.ic_videocam_outline,
                        stringResource(if (event.isJoinOpen()) R.string.placement_join_now else R.string.placement_join_later),
                    ) { openJoinLink(context, event) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryAction(R.drawable.ic_event_add, stringResource(R.string.placement_add_calendar), Modifier.weight(1f)) { addToCalendar(context, event) }
                    SecondaryAction(R.drawable.ic_share, stringResource(R.string.placement_share), Modifier.weight(1f)) { shareEvent(context, event, time) }
                }
            }
        }
    }
}

@Composable
internal fun PrimaryOutlinedAction(iconRes: Int, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        // Long labels ("Join on Google Meet (opens 10 minutes before)") wrap to a second line.
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BrandBlue),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = BrandBlue, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun SecondaryAction(iconRes: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, FieldBorder),
        // Two buttons share a row, so keep the padding tight enough for "Add to calendar".
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "For your children" — green when at least one child's course is on the list. */
@Composable
private fun FamilyEligibilityCard(event: PlacementEvent, role: Role, family: List<PlacementChild>) {
    val anyEligible = family.any { event.isEligible(it.course) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (anyEligible) EligibleCardBackground else White,
        border = BorderStroke(1.dp, if (anyEligible) BrandGreen.copy(alpha = 0.7f) else FieldBorder),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                stringResource(if (role == Role.STUDENT) R.string.placement_for_you else R.string.placement_for_children),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                family.forEach { child -> ChildEligibilityRow(child, event.isEligible(child.course), onWhite = anyEligible) }
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.placement_eligibility_note), color = TextSecondary, fontSize = AppTextSize.Caption, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ChildEligibilityRow(child: PlacementChild, eligible: Boolean, onWhite: Boolean) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (onWhite) White else Color.Transparent) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsBadge(child.initials, size = 36.dp, background = BadgeNeutralBackground, content = TextPrimary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.placement_child_course, child.firstName, child.course),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(if (eligible) R.drawable.ic_check else R.drawable.ic_remove_circle_outline),
                        contentDescription = null,
                        tint = if (eligible) BrandGreen else TextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(if (eligible) R.string.placement_course_eligible else R.string.placement_course_not_eligible),
                        color = if (eligible) BrandGreen else TextSecondary,
                        fontSize = AppTextSize.Caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** White card with a bold title ("Company", "Eligibility"). */
@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/** Very light green behind "For your children" when a child can attend. */
private val EligibleCardBackground = OnboardingGreenDisc.copy(alpha = 0.8f)

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun PlacementEventDetailPreview() {
    GRADit_RewampTheme {
        Column(Modifier.background(ScreenBackground)) {
            PlacementEventDetailContent(
                PlacementRepository.events.value.first { it.id == "infosys-drive" },
                Role.PARENT,
                PlacementRepository.familyFor(Role.PARENT),
            )
        }
    }
}
