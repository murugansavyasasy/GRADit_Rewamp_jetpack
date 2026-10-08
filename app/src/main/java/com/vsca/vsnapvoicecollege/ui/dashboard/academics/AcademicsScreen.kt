package com.vsca.vsnapvoicecollege.ui.dashboard.academics

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.BrandGreen
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import com.vsca.vsnapvoicecollege.ui.theme.WhiteMuted

private data class SemesterScore(
    @param:StringRes val label: Int,
    val sgpa: Float,
    val highlighted: Boolean = false,
)

private data class SubjectStrength(
    @param:StringRes val name: Int,
    val percent: Int,
    val highlighted: Boolean = false,
)

private val semesterScores = listOf(
    SemesterScore(R.string.acad_sem1, 7.9f),
    SemesterScore(R.string.acad_sem2, 8.2f),
    SemesterScore(R.string.acad_sem3, 8.5f),
    SemesterScore(R.string.acad_sem4, 8.9f, highlighted = true),
)

private val subjectStrengths = listOf(
    SubjectStrength(R.string.acad_subj_dbms, 88, highlighted = true),
    SubjectStrength(R.string.acad_subj_networks, 74),
    SubjectStrength(R.string.acad_subj_os, 82),
    SubjectStrength(R.string.acad_subj_toc, 78),
)

// SGPA chart y-axis bounds (the "scale 6 – 10" label).
private const val SGPA_MIN = 6f
private const val SGPA_MAX = 10f

/**
 * Academics tab — academic performance overview. Hardcoded content for now.
 */
@Composable
fun AcademicsScreen(
    onBack: () -> Unit,
    onViewMarks: () -> Unit = {},
    onOpenAttendance: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.acad_title),
                color = TextPrimary,
                fontSize = AppTextSize.H4,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(Modifier.height(20.dp))
        SummaryCards()

        Spacer(Modifier.height(20.dp))
        SgpaChartCard()

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.acad_subject_title),
            color = TextPrimary,
            fontSize = AppTextSize.H6,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(16.dp))
        SubjectStrengthList()

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                text = stringResource(R.string.acad_view_marks),
                onClick = onViewMarks,
                modifier = Modifier.weight(1f),
                containerColor = SelectedRowBackground,
                contentColor = BrandBlue,
            )
            PrimaryButton(
                text = stringResource(R.string.acad_attendance),
                onClick = onOpenAttendance,
                modifier = Modifier.weight(1f),
                containerColor = OnboardingGreenDisc,
                contentColor = BrandGreen,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryCards() {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            label = stringResource(R.string.acad_cgpa_label),
            value = stringResource(R.string.acad_cgpa_value),
            sub = stringResource(R.string.acad_cgpa_sub),
            containerColor = BrandBlue,
            contentColor = White,
            subColor = WhiteMuted,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        StatCard(
            label = stringResource(R.string.acad_trend_label),
            value = stringResource(R.string.acad_trend_value),
            sub = stringResource(R.string.acad_trend_sub),
            containerColor = OnboardingGreenDisc,
            contentColor = BrandGreen,
            subColor = BrandGreen,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    sub: String,
    containerColor: Color,
    contentColor: Color,
    subColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                color = contentColor,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                color = contentColor,
                fontSize = AppTextSize.H1,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = sub, color = subColor, fontSize = AppTextSize.Caption)
        }
    }
}

@Composable
private fun SgpaChartCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.acad_sgpa_title),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.acad_sgpa_scale),
                    color = TextMuted,
                    fontSize = AppTextSize.Caption,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                semesterScores.forEach { score ->
                    SgpaBar(score = score, modifier = Modifier.weight(1f))
                }
            }

            HorizontalDivider(color = FieldBorder)

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                semesterScores.forEach { score ->
                    Text(
                        text = stringResource(score.label),
                        modifier = Modifier.weight(1f),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SgpaBar(score: SemesterScore, modifier: Modifier = Modifier) {
    val color = if (score.highlighted) BrandGreen else BrandBlue
    val fraction = ((score.sgpa - SGPA_MIN) / (SGPA_MAX - SGPA_MIN)).coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = score.sgpa.toString(),
            color = color,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(34.dp)
                .fillMaxHeight(fraction)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color),
        )
    }
}

@Composable
private fun SubjectStrengthList() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        subjectStrengths.forEach { subject ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(subject.name),
                    modifier = Modifier.width(92.dp),
                    color = TextPrimary,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.Bold,
                )
                ProgressBar(
                    fraction = subject.percent / 100f,
                    color = if (subject.highlighted) BrandGreen else BrandBlue,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text(
                    text = stringResource(R.string.acad_percent, subject.percent),
                    modifier = Modifier.width(44.dp),
                    color = TextPrimary,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(BadgeNeutralBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AcademicsPreview() {
    GRADit_RewampTheme {
        AcademicsScreen(onBack = {})
    }
}
