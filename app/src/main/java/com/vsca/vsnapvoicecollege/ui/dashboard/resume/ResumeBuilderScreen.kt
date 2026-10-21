package com.vsca.vsnapvoicecollege.ui.dashboard.resume

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.DialogButtons
import com.vsca.vsnapvoicecollege.ui.components.DialogCard
import com.vsca.vsnapvoicecollege.ui.components.LabelPill
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SelectionRadio
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.SquareIconButton
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.components.isDaysAgo
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.MessageCard
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.RetryButton
import com.vsca.vsnapvoicecollege.ui.dashboard.placement.syncedLabel
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AmberChipBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingAmberCard
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Resume builder. Students edit their sections and template; parents see their child's
 * resume read-only and can preview or download it.
 */
@Composable
fun ResumeBuilderScreen(
    onBack: () -> Unit,
    onPreview: (studentId: String) -> Unit,
    onEditSection: (studentId: String, section: ResumeSection) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResumeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickingTemplate by rememberSaveable { mutableStateOf(false) }
    var collegeSection by rememberSaveable { mutableStateOf<ResumeSection?>(null) }

    ResumeBuilderContent(
        uiState = uiState,
        onBack = onBack,
        onStudent = viewModel::onStudent,
        onRetry = viewModel::refresh,
        onPreview = { uiState.resume?.let { onPreview(it.studentId) } },
        onDownload = { uiState.resume?.let { ResumePdf.download(context, it) } },
        onChangeTemplate = { pickingTemplate = true },
        onSection = { section ->
            val resume = uiState.resume ?: return@ResumeBuilderContent
            if (section.fromCollege) collegeSection = section else onEditSection(resume.studentId, section)
        },
        modifier = modifier,
    )

    val resume = uiState.resume
    if (pickingTemplate && resume != null) {
        TemplateDialog(
            current = resume.template,
            onDismiss = { pickingTemplate = false },
            onPick = {
                viewModel.onTemplate(it)
                pickingTemplate = false
            },
        )
    }
    collegeSection?.let { section ->
        if (resume != null) CollegeSectionDialog(section, resume.contact.collegeShort, onDismiss = { collegeSection = null })
    }
}

@Composable
private fun ResumeBuilderContent(
    uiState: ResumeUiState,
    onBack: () -> Unit,
    onStudent: (String) -> Unit,
    onRetry: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onChangeTemplate: () -> Unit,
    onSection: (ResumeSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resume = uiState.resume
    val header = resume ?: uiState.resumes.firstOrNull()
    Column(modifier.fillMaxSize().background(ScreenBackground)) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
            item(key = "header") {
                TitledTopBar(
                    title = when {
                        uiState.canEdit -> stringResource(R.string.resume_title)
                        header != null -> stringResource(R.string.resume_title_child, header.firstName)
                        else -> stringResource(R.string.resume_title)
                    },
                    subtitle = header?.let {
                        val updated = updatedLabel(it.updatedAt)
                        if (uiState.canEdit) {
                            stringResource(R.string.resume_saved_at, updated)
                        } else {
                            stringResource(R.string.resume_built_by, it.firstName, updated)
                        }
                    },
                    onBack = onBack,
                    trailing = {
                        SquareIconButton(
                            iconRes = R.drawable.ic_visibility_outline,
                            contentDescription = stringResource(R.string.resume_preview),
                            onClick = { if (resume != null) onPreview() },
                        )
                    },
                )
            }
            if (!uiState.isOnline && uiState.load != ResumeLoad.ERROR) {
                item(key = "offline") {
                    OfflineBanner(
                        stringResource(R.string.resume_offline_banner, syncedLabel(uiState.lastSyncedAt)),
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
            if (uiState.showChildSwitcher) {
                item(key = "children") { ChildSwitcher(uiState.resumes, header?.studentId, onStudent) }
            }
            when {
                uiState.load == ResumeLoad.LOADING -> item(key = "loading") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(26.dp)).background(FieldBorder))
                        SkeletonCards(2)
                    }
                }
                uiState.load == ResumeLoad.ERROR || resume == null -> item(key = "error") {
                    MessageCard {
                        StateMessage(
                            iconRes = R.drawable.ic_wifi,
                            iconTint = BadgeRed,
                            iconBackground = AlertBackground,
                            title = stringResource(R.string.resume_error_title),
                            subtitle = stringResource(R.string.resume_error_body),
                        ) { RetryButton(onRetry) }
                    }
                }
                else -> {
                    item(key = "strength") { StrengthCard(resume, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                    item(key = "note") { SourceNote(resume, uiState.canEdit) }
                    item(key = "template") { TemplateCard(resume.template, uiState.canEdit, onChangeTemplate) }
                    item(key = "sections-title") {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.resume_sections), Modifier.weight(1f), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                            Text(
                                stringResource(R.string.resume_sections_complete, resume.completeCount, ResumeSection.entries.size),
                                color = TextSecondary,
                                fontSize = AppTextSize.Label,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    item(key = "sections") { SectionsCard(resume, uiState.canEdit, onSection) }
                }
            }
        }
        if (resume != null) {
            ActionBar(onPreview, onDownload)
        }
    }
}

/** "today, 9:42 AM", "yesterday, 6:15 PM" or "15 Oct, 6:15 PM". */
@Composable
private fun updatedLabel(millis: Long): String {
    val time = clockLabel(millis)
    return when {
        isDaysAgo(millis, 0) -> stringResource(R.string.resume_time_today, time)
        isDaysAgo(millis, 1) -> stringResource(R.string.resume_time_yesterday, time)
        else -> "${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))}, $time"
    }
}

@Composable
private fun ChildSwitcher(resumes: List<Resume>, selectedId: String?, onSelect: (String) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        resumes.forEach { resume ->
            val isSelected = resume.studentId == selectedId
            Surface(
                onClick = { onSelect(resume.studentId) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                color = if (isSelected) SelectedRowBackground else White,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandBlue else FieldBorder),
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (isSelected) BrandBlue else BadgeNeutralBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(resume.initials, color = if (isSelected) White else TextPrimary, fontSize = AppTextSize.Caption, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(resume.firstName, color = TextPrimary, fontSize = AppTextSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            stringResource(R.string.resume_percent_strength, resume.strength),
                            color = TextSecondary,
                            fontSize = AppTextSize.Caption,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Ring with the %, "RESUME STRENGTH", the level and what's left to add. */
@Composable
private fun StrengthCard(resume: Resume, modifier: Modifier = Modifier) {
    val percent = resume.strength
    val missing = resume.missing
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = ResumeHeroBlue) {
        Box {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 60.dp, y = (-50).dp).size(220.dp).clip(CircleShape)
                    .background(White.copy(alpha = 0.07f)),
            )
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                StrengthRing(percent)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.resume_strength).uppercase(Locale.getDefault()),
                        color = White.copy(alpha = 0.85f),
                        fontSize = AppTextSize.Label,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(StrengthLevel.of(percent).label), color = White, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (missing.isEmpty()) {
                            stringResource(R.string.resume_strength_ready)
                        } else {
                            stringResource(R.string.resume_strength_hint, joinSections(missing.map { stringResource(it.label).lowercase(Locale.getDefault()) }))
                        },
                        color = White.copy(alpha = 0.9f),
                        fontSize = AppTextSize.Body,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthRing(percent: Int) {
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            val stroke = 10.dp.toPx()
            drawArc(White.copy(alpha = 0.25f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(stroke))
            drawArc(White, startAngle = -90f, sweepAngle = 360f * percent / 100f, useCenter = false, style = Stroke(stroke, cap = StrokeCap.Butt))
        }
        Text(stringResource(R.string.resume_percent, percent), color = White, fontSize = AppTextSize.H5, fontWeight = FontWeight.ExtraBold)
    }
}

/** "A", "A and B", "A, B and C" in the app language. */
@Composable
private fun joinSections(labels: List<String>): String = when (labels.size) {
    0 -> ""
    1 -> labels[0]
    else -> stringResource(R.string.resume_list_and, labels.dropLast(1).joinToString(", "), labels.last())
}

/** Green note: where the data comes from (student) or who builds it (parent). */
@Composable
private fun SourceNote(resume: Resume, canEdit: Boolean) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp), color = OnboardingGreenDisc) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_shield_check), contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                if (canEdit) {
                    stringResource(R.string.resume_note_student, resume.contact.collegeShort)
                } else {
                    stringResource(R.string.resume_note_parent, resume.firstName)
                },
                color = BrandGreen,
                fontSize = AppTextSize.Body,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun TemplateCard(template: ResumeTemplate, canEdit: Boolean, onChange: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TemplateThumbnail(template)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.resume_template).uppercase(Locale.getDefault()), color = TextSecondary, fontSize = AppTextSize.Label, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp)
                Text(stringResource(template.label), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(template.description), color = TextSecondary, fontSize = AppTextSize.Label)
            }
            if (canEdit) {
                Spacer(Modifier.width(8.dp))
                Surface(onClick = onChange, shape = RoundedCornerShape(14.dp), color = SelectedRowBackground) {
                    Text(
                        stringResource(R.string.resume_change),
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = BrandBlue,
                        fontSize = AppTextSize.Body,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** A tiny page with the template's accent bars. */
@Composable
private fun TemplateThumbnail(template: ResumeTemplate, modifier: Modifier = Modifier) {
    val accent = template.style().accent
    Surface(modifier.size(width = 56.dp, height = 74.dp), shape = RoundedCornerShape(8.dp), color = White, border = BorderStroke(1.dp, FieldBorder)) {
        Column(Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.width(26.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Box(Modifier.width(18.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(FieldBorder))
            Box(Modifier.fillMaxWidth().height(2.dp).background(accent))
            repeat(if (template == ResumeTemplate.COMPACT) 4 else 3) {
                Box(Modifier.fillMaxWidth(if (it % 2 == 0) 1f else 0.7f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(FieldBorder))
            }
        }
    }
}

@Composable
private fun SectionsCard(resume: Resume, canEdit: Boolean, onSection: (ResumeSection) -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(Modifier.padding(horizontal = 14.dp)) {
            ResumeSection.entries.forEachIndexed { index, section ->
                if (index > 0) HorizontalDivider(color = FieldBorder)
                SectionRow(resume, section, canEdit, onClick = { onSection(section) })
            }
        }
    }
}

@Composable
private fun SectionRow(resume: Resume, section: ResumeSection, canEdit: Boolean, onClick: () -> Unit) {
    val rowModifier = if (canEdit) Modifier.clickable(onClick = onClick) else Modifier
    Row(Modifier.fillMaxWidth().then(rowModifier).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SelectedRowBackground), contentAlignment = Alignment.Center) {
            Icon(painterResource(section.icon), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(section.label), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            Text(sectionSummary(resume, section, canEdit), color = TextSecondary, fontSize = AppTextSize.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        when {
            !resume.isComplete(section) -> LabelPill(
                stringResource(if (canEdit) R.string.resume_chip_add else R.string.resume_chip_missing),
                AmberChipBackground,
                OnboardingAmberCard,
            )
            section.fromCollege -> LabelPill(stringResource(R.string.resume_chip_auto), OnboardingGreenDisc, BrandGreen)
            else -> LabelPill(stringResource(R.string.resume_chip_done), SelectedRowBackground, BrandBlue)
        }
    }
}

/** "Name, email, phone, Chennai", "6 added · Python, SQL, React", "Not added yet". */
@Composable
private fun sectionSummary(resume: Resume, section: ResumeSection, canEdit: Boolean): String {
    if (section == ResumeSection.PERSONAL) return stringResource(R.string.resume_personal_summary, resume.contact.city)
    val entries = resume.entries(section)
    return when {
        entries.isEmpty() -> stringResource(if (canEdit) R.string.resume_tap_to_add else R.string.resume_not_added)
        section == ResumeSection.EDUCATION -> entries.joinToString(" · ") { it.short ?: it.title }
        else -> {
            val count = stringResource(R.string.resume_added_count, entries.size)
            val titles = entries.take(section.previewTitles).joinToString(", ") { it.title }
            if (titles.isEmpty()) count else "$count · $titles"
        }
    }
}

/** Preview (outlined) and Download PDF (blue), pinned above the bottom bar. */
@Composable
internal fun ActionBar(onPreview: () -> Unit, onDownload: () -> Unit, previewIcon: Int = R.drawable.ic_visibility_outline, previewLabel: String = stringResource(R.string.resume_preview)) {
    Surface(Modifier.fillMaxWidth(), color = White, shadowElevation = 8.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.weight(0.37f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FieldBorder),
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Icon(painterResource(previewIcon), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(previewLabel, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onDownload,
                modifier = Modifier.weight(0.63f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = White),
            ) {
                Icon(painterResource(R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.resume_download_pdf), fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TemplateDialog(current: ResumeTemplate, onDismiss: () -> Unit, onPick: (ResumeTemplate) -> Unit) {
    var choice by rememberSaveable { mutableStateOf(current) }
    DialogCard(onDismiss) {
        Text(stringResource(R.string.resume_template_pick), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ResumeTemplate.entries.forEach { template ->
                val selected = template == choice
                Surface(
                    onClick = { choice = template },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) SelectedRowBackground else White,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BrandBlue else FieldBorder),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TemplateThumbnail(template, Modifier.size(width = 40.dp, height = 54.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(template.label), color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(template.description), color = TextSecondary, fontSize = AppTextSize.Label)
                        }
                        SelectionRadio(selected)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        DialogButtons(
            confirm = stringResource(R.string.resume_template_use),
            confirmEnabled = true,
            onDismiss = onDismiss,
            onConfirm = { onPick(choice) },
        )
    }
}

/** Sections from college records can't be edited here. */
@Composable
private fun CollegeSectionDialog(section: ResumeSection, college: String, onDismiss: () -> Unit) {
    DialogCard(onDismiss) {
        Text(stringResource(R.string.resume_college_title, stringResource(section.label)), color = TextPrimary, fontSize = AppTextSize.H6, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.resume_college_body, college), color = TextSecondary, fontSize = AppTextSize.Body, lineHeight = 21.sp)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = stringResource(R.string.resume_college_ok), onClick = onDismiss)
    }
}

/** Deep blue of the strength card, shared with the credits and fee summaries. */
internal val ResumeHeroBlue = Color(0xFF1E42B4)

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun ResumeBuilderPreview() {
    GRADit_RewampTheme {
        val resumes = ResumeRepository.resumes.value
        ResumeBuilderContent(
            uiState = ResumeUiState(load = ResumeLoad.READY, canEdit = true, resumes = resumes.take(1), resume = resumes[0]),
            onBack = {}, onStudent = {}, onRetry = {}, onPreview = {}, onDownload = {}, onChangeTemplate = {}, onSection = {},
        )
    }
}
