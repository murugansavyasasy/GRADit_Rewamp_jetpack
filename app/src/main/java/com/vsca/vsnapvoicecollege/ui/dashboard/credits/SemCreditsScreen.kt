package com.vsca.vsnapvoicecollege.ui.dashboard.credits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.FilterDropdown
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.Segment
import com.vsca.vsnapvoicecollege.ui.components.SegmentedControl
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberText
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberRing
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Deep blue of the summary card; the stat tiles use the brand blue on top of it. */
private val HeroBlue = Color(0xFF1E42B4)

/** Darker green for "obtained" figures, matching the design. */
private val ObtainedGreen = Color(0xFF17733F)

/**
 * Sem Credit Points for parents and students: credits obtained against the regulation's
 * total, arrears still pending, and a breakdown by category or by semester.
 */
@Composable
fun SemCreditsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SemCreditsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SemCreditsContent(
        uiState = uiState,
        onBack = onBack,
        onChild = viewModel::onChildSelected,
        onView = viewModel::onViewSelected,
        onCategory = viewModel::onCategorySelected,
        onToggle = viewModel::onToggle,
        onToggleAll = viewModel::onToggleAll,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun SemCreditsContent(
    uiState: SemCreditsUiState,
    onBack: () -> Unit,
    onChild: (CreditRecord) -> Unit,
    onView: (CreditView) -> Unit,
    onCategory: (CreditCategory?) -> Unit,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val record = uiState.record
    val selected = uiState.selected
    LazyColumn(
        modifier = modifier.fillMaxSize().background(ScreenBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            TitledTopBar(
                title = stringResource(R.string.credit_title),
                subtitle = selected?.let {
                    stringResource(R.string.credit_subtitle, it.name, stringResource(R.string.credit_child_class, it.programme, it.currentSemester, it.section))
                },
                onBack = onBack,
            )
        }
        if (!uiState.isOnline && uiState.load == CreditLoad.READY && uiState.lastSyncedAt > 0) {
            item(key = "offline") {
                OfflineBanner(
                    stringResource(R.string.credit_offline, savedAtLabel(uiState.lastSyncedAt)),
                    Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        if (uiState.showChildSwitcher) {
            item(key = "children") { ChildSwitcher(uiState.children, selected, onChild) }
        }
        when (uiState.load) {
            CreditLoad.LOADING -> item(key = "loading") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.fillMaxWidth().height(290.dp).clip(RoundedCornerShape(26.dp)).background(FieldBorder))
                    SkeletonCards(2)
                    PoweredByRow(Modifier.fillMaxWidth().wrapContentWidth().padding(vertical = 12.dp), textColor = TextSecondary)
                }
            }
            CreditLoad.ERROR -> item(key = "error") {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
                        StateMessage(
                            iconRes = R.drawable.ic_wifi,
                            iconTint = BadgeRed,
                            iconBackground = AlertBackground,
                            title = stringResource(R.string.credit_error_title),
                            subtitle = stringResource(R.string.credit_error_body),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
                        ) {
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
                            ) {
                                Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.credit_try_again), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    PoweredByRow(Modifier.fillMaxWidth().wrapContentWidth().padding(vertical = 24.dp), textColor = TextSecondary)
                }
            }
            CreditLoad.READY -> if (record != null) {
                item(key = "summary") { SummaryCard(record, Modifier.padding(horizontal = 16.dp)) }
                item(key = "view") {
                    SegmentedControl(
                        options = listOf(Segment(stringResource(R.string.credit_by_category)), Segment(stringResource(R.string.credit_by_semester))),
                        selectedIndex = uiState.view.ordinal,
                        onSelect = { onView(CreditView.entries[it]) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        height = 56.dp,
                    )
                }
                item(key = "filters") {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val all = stringResource(R.string.credit_filter_all)
                        FilterDropdown(
                            label = stringResource(R.string.credit_filter_category),
                            value = uiState.category?.code ?: all,
                            selected = uiState.category?.let { stringResource(it.label) } ?: all,
                            options = listOf<Pair<String, () -> Unit>>(all to { onCategory(null) }) +
                                CreditCategory.entries.map { c -> stringResource(c.label) to { onCategory(c) } },
                            highlighted = uiState.category != null,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onToggleAll) {
                            Text(
                                stringResource(if (uiState.allExpanded) R.string.credit_collapse_all else R.string.credit_expand_all),
                                color = BrandBlue,
                                fontSize = AppTextSize.BodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (uiState.view == CreditView.BY_CATEGORY) {
                    items(uiState.categories, key = { "c-" + it.code }) { category ->
                        CategoryCard(
                            record = record,
                            category = category,
                            expanded = category.code in uiState.expanded,
                            onToggle = { onToggle(category.code) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    items(uiState.semesters, key = { "s-$it" }) { semester ->
                        SemesterCard(
                            record = record,
                            semester = semester,
                            category = uiState.category,
                            expanded = semesterKey(semester) in uiState.expanded,
                            onToggle = { onToggle(semesterKey(semester)) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                item(key = "powered") { PoweredByRow(Modifier.fillMaxWidth().wrapContentWidth().padding(vertical = 12.dp), textColor = TextSecondary) }
            }
        }
    }
}

/** "Wed 07 Oct, 08:15 AM". */
@Composable
private fun savedAtLabel(millis: Long): String =
    "${SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date(millis))}, ${clockLabel(millis)}"

@Composable
private fun ChildSwitcher(children: List<CreditRecord>, selected: CreditRecord?, onSelect: (CreditRecord) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        children.forEach { child ->
            val isSelected = child.studentId == selected?.studentId
            Surface(
                onClick = { onSelect(child) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = if (isSelected) SelectedRowBackground else White,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandBlue else FieldBorder),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(if (isSelected) BrandBlue else BadgeNeutralBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(child.initials, color = if (isSelected) White else TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(child.name, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            stringResource(R.string.credit_child_class, child.programme, child.currentSemester, child.section),
                            color = TextSecondary,
                            fontSize = AppTextSize.Label,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(record: CreditRecord, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = HeroBlue) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.credit_obtained_regulation, record.regulation),
                        color = White.copy(alpha = 0.9f),
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)) { append(record.obtainedCredits.toString()) }
                            withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = White.copy(alpha = 0.85f))) {
                                append(" / ${record.totalCredits}")
                            }
                        },
                        color = White,
                    )
                }
                ProgressRing(record.percent, record.obtainedCredits.toFloat() / record.totalCredits.coerceAtLeast(1))
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(record.toBeObtained.toString(), stringResource(R.string.credit_to_be_obtained), Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.credit_sem_of, record.completedSemesters, record.totalSemesters),
                    stringResource(R.string.credit_semesters_completed),
                    Modifier.weight(1f),
                )
                StatTile(record.creditsThisSemester.toString(), stringResource(R.string.credit_this_semester), Modifier.weight(1f))
            }
            record.pendingSemesters.forEach { pending ->
                Spacer(Modifier.height(14.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AmberChipBackground) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(painterResource(R.drawable.ic_info_outline), contentDescription = null, tint = OnboardingAmberCard, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(
                                R.string.credit_pending_banner,
                                pending.semester,
                                pending.credits,
                                joinCodes(pending.courses.map { it.code }),
                                pending.courses.firstNotNullOfOrNull { it.arrearExam }.orEmpty(),
                            ),
                            color = OnboardingAmberCard,
                            fontSize = AppTextSize.Body,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }
        }
    }
}

/** "MA3354 and CS3351" / "A, B and C". */
@Composable
private fun joinCodes(codes: List<String>): String = when (codes.size) {
    0 -> ""
    1 -> codes[0]
    else -> stringResource(R.string.credit_and, codes.dropLast(1).joinToString(", "), codes.last())
}

@Composable
private fun ProgressRing(percent: Int, fraction: Float) {
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(104.dp)) {
            val stroke = 14.dp.toPx()
            drawArc(BrandBlue, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(stroke))
            drawArc(White, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, style = Stroke(stroke, cap = StrokeCap.Butt))
        }
        Text(stringResource(R.string.credit_percent, percent), color = White, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.heightIn(min = 100.dp), shape = RoundedCornerShape(18.dp), color = BrandBlue) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(value, color = White, fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(label, color = White.copy(alpha = 0.9f), fontSize = AppTextSize.Body, lineHeight = 19.sp)
        }
    }
}

// ---- Cards ----

@Composable
private fun CategoryCard(
    record: CreditRecord,
    category: CreditCategory,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totals = record.categoryTotals(category)
    ExpandableCard(
        badge = category.code,
        badgeBackground = category.background,
        badgeContent = category.content,
        title = stringResource(category.label),
        subtitle = stringResource(R.string.credit_complete, totals.percent),
        subtitleColor = TextPrimary,
        totals = totals,
        barColor = BrandBlue,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
    ) {
        val rows = (1..record.totalSemesters).mapNotNull { sem ->
            record.semesterTotals(sem, category).takeIf { it.total > 0 }?.let { sem to it }
        }
        BreakdownTable(
            firstColumn = stringResource(R.string.credit_col_semester),
            rows = rows.map { (sem, t) ->
                BreakdownRow(
                    label = stringResource(R.string.credit_sem_short, sem),
                    note = semesterNote(record, sem),
                    totals = t,
                )
            },
        )
    }
}

@Composable
private fun SemesterCard(
    record: CreditRecord,
    semester: Int,
    category: CreditCategory?,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totals = record.semesterTotals(semester, category)
    val state = record.semesterState(semester)
    val (badgeBg, badgeFg) = when (state) {
        SemesterState.COMPLETED -> OnboardingGreenDisc to BrandGreen
        SemesterState.PENDING -> AmberChipBackground to OnboardingAmberCard
        SemesterState.IN_PROGRESS -> SelectedRowBackground to BrandBlue
        SemesterState.UPCOMING -> BadgeNeutralBackground to TextSecondary
    }
    val month = record.resultMonths[semester].orEmpty()
    ExpandableCard(
        badge = stringResource(R.string.credit_sem_badge, semester),
        badgeBackground = badgeBg,
        badgeContent = badgeFg,
        title = stringResource(R.string.credit_semester, semester),
        subtitle = when (state) {
            SemesterState.COMPLETED -> stringResource(R.string.credit_state_completed, month)
            SemesterState.PENDING -> stringResource(R.string.credit_state_pending, totals.remaining)
            SemesterState.IN_PROGRESS -> stringResource(R.string.credit_state_in_progress)
            SemesterState.UPCOMING -> stringResource(R.string.credit_state_upcoming)
        },
        subtitleColor = when (state) {
            SemesterState.COMPLETED -> ObtainedGreen
            SemesterState.PENDING -> OnboardingAmberCard
            SemesterState.IN_PROGRESS -> BrandBlue
            SemesterState.UPCOMING -> TextSecondary
        },
        totals = totals,
        barColor = if (state == SemesterState.COMPLETED) ObtainedGreen else BrandBlue,
        border = if (state == SemesterState.PENDING) OnboardingAmberRing else FieldBorder,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
    ) {
        val courses = record.courses.filter { it.semester == semester && (category == null || it.category == category) }
        CourseList(courses)
    }
}

@Composable
private fun ExpandableCard(
    badge: String,
    badgeBackground: Color,
    badgeContent: Color,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    totals: Totals,
    barColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    border: Color = FieldBorder,
    details: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = White,
        border = BorderStroke(if (border == FieldBorder) 1.dp else 1.5.dp, border),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 56.dp, height = 44.dp).clip(RoundedCornerShape(12.dp)).background(badgeBackground), contentAlignment = Alignment.Center) {
                    Text(badge, color = badgeContent, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
                    Text(subtitle, color = subtitleColor, fontSize = AppTextSize.Body, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = AppTextSize.H4, fontWeight = FontWeight.ExtraBold, color = TextPrimary)) { append(totals.obtained.toString()) }
                            withStyle(SpanStyle(fontSize = AppTextSize.BodyLarge, color = TextSecondary)) { append(" / ${totals.total}") }
                        },
                    )
                    Text(stringResource(R.string.credit_credits), color = TextSecondary, fontSize = AppTextSize.Body)
                }
                Icon(
                    painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                    contentDescription = stringResource(if (expanded) R.string.credit_collapse_desc else R.string.credit_expand_desc, title),
                    tint = TextPrimary,
                    modifier = Modifier.padding(start = 8.dp).size(26.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { totals.fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = barColor,
                trackColor = FieldBorder,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Text(
                    stringResource(R.string.credit_obtained_count, totals.obtained),
                    Modifier.weight(1f),
                    color = ObtainedGreen,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.credit_to_be_obtained_count, totals.remaining),
                    color = if (totals.remaining > 0) AmberText else TextSecondary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(16.dp))
                details()
            }
        }
    }
}

private data class BreakdownRow(val label: String, val note: String?, val totals: Totals)

/** "Upcoming", "In progress" or "Arrear" under a semester row, when it isn't simply completed. */
@Composable
private fun semesterNote(record: CreditRecord, semester: Int): String? = when (record.semesterState(semester)) {
    SemesterState.COMPLETED -> null
    SemesterState.PENDING -> stringResource(R.string.credit_note_arrear)
    SemesterState.IN_PROGRESS -> stringResource(R.string.credit_note_in_progress)
    SemesterState.UPCOMING -> stringResource(R.string.credit_note_upcoming)
}

@Composable
private fun BreakdownTable(firstColumn: String, rows: List<BreakdownRow>) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column {
            Row(Modifier.fillMaxWidth().background(ScreenBackground).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderCell(firstColumn, Modifier.weight(1.3f), TextAlign.Start)
                HeaderCell(stringResource(R.string.credit_col_obtained), Modifier.weight(1f), TextAlign.End)
                HeaderCell(stringResource(R.string.credit_col_to_be_obtained), Modifier.weight(1.1f), TextAlign.End)
                HeaderCell(stringResource(R.string.credit_col_total), Modifier.weight(0.8f), TextAlign.End)
            }
            rows.forEach { row ->
                HorizontalDivider(color = FieldBorder)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1.3f)) {
                        Text(row.label, color = TextPrimary, fontSize = AppTextSize.BodyLarge)
                        row.note?.let { Text(it, color = TextSecondary, fontSize = AppTextSize.Label) }
                    }
                    NumberCell(row.totals.obtained, ObtainedGreen, Modifier.weight(1f))
                    NumberCell(row.totals.remaining, if (row.totals.remaining > 0) AmberText else TextSecondary, Modifier.weight(1.1f))
                    NumberCell(row.totals.total, TextPrimary, Modifier.weight(0.8f), bold = false)
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(text, modifier, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, textAlign = align, lineHeight = 18.sp)
}

@Composable
private fun NumberCell(value: Int, color: Color, modifier: Modifier, bold: Boolean = true) {
    Text(
        value.toString(),
        modifier,
        color = color,
        fontSize = AppTextSize.BodyLarge,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.End,
    )
}

/** The courses behind a semester's credits, with their category and status. */
@Composable
private fun CourseList(courses: List<Course>) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column {
            courses.forEachIndexed { index, course ->
                if (index > 0) HorizontalDivider(color = FieldBorder)
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = course.category.background) {
                        Text(
                            course.category.code,
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = course.category.content,
                            fontSize = AppTextSize.Caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(course.name, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(course.code, statusLabel(course)).joinToString(" · "),
                            color = if (course.status == CourseStatus.ARREAR) OnboardingAmberCard else TextSecondary,
                            fontSize = AppTextSize.Label,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.credit_course_credits, course.credits),
                        color = if (course.status.isObtained) ObtainedGreen else TextMuted,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun statusLabel(course: Course): String? = when (course.status) {
    CourseStatus.PASSED -> stringResource(R.string.credit_status_passed)
    CourseStatus.ARREAR -> course.arrearExam?.let { stringResource(R.string.credit_status_arrear_exam, it) }
        ?: stringResource(R.string.credit_status_arrear)
    CourseStatus.ONGOING -> stringResource(R.string.credit_status_ongoing)
    CourseStatus.UPCOMING -> null
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun SemCreditsPreview() {
    GRADit_RewampTheme {
        val children = CreditRepository.children
        SemCreditsContent(
            uiState = SemCreditsUiState(load = CreditLoad.READY, children = children, selected = children[0], record = children[0], expanded = setOf("ES")),
            onBack = {}, onChild = {}, onView = {}, onCategory = {}, onToggle = {}, onToggleAll = {}, onRetry = {},
        )
    }
}
