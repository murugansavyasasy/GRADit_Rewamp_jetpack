package com.vsca.vsnapvoicecollege.ui.dashboard.help

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AppTextField
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.OnboardingGreenDisc
import com.vsca.vsnapvoicecollege.ui.theme.PositiveGreen
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

private const val CATEGORY_COLUMNS = 3

/**
 * "Raise a concern" support form, reached from Settings → Help & support.
 */
@Composable
fun RaiseConcernScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RaiseConcernViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RaiseConcernContent(
        uiState = uiState,
        onBack = onBack,
        onCategorySelected = viewModel::onCategorySelected,
        onSubjectChange = viewModel::onSubjectChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSubmit = {
            viewModel.onSubmit()
            onSubmitted()
        },
        modifier = modifier,
    )
}

@Composable
private fun RaiseConcernContent(
    uiState: RaiseConcernUiState,
    onBack: () -> Unit,
    onCategorySelected: (ConcernCategory) -> Unit,
    onSubjectChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.concern_title),
                color = TextPrimary,
                fontSize = AppTextSize.H4,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(20.dp))

            FieldLabel(stringResource(R.string.concern_category))
            Spacer(Modifier.height(10.dp))
            CategoryGrid(
                selected = uiState.selectedCategory,
                onSelect = onCategorySelected,
            )

            Spacer(Modifier.height(20.dp))

            AppTextField(
                value = uiState.subject,
                onValueChange = onSubjectChange,
                label = stringResource(R.string.concern_subject_label),
                placeholder = stringResource(R.string.concern_subject_hint),
            )

            Spacer(Modifier.height(20.dp))

            AppTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                label = stringResource(R.string.concern_describe_label),
                placeholder = stringResource(R.string.concern_describe_hint),
                singleLine = false,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.concern_char_count,
                    uiState.description.length,
                    RaiseConcernUiState.MAX_DESCRIPTION_LENGTH,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                color = TextMuted,
                fontSize = AppTextSize.Caption,
                textAlign = TextAlign.End,
            )

            Spacer(Modifier.height(16.dp))
            AttachFileButton()

            Spacer(Modifier.height(16.dp))
            ResponseTimeChip()

            Spacer(Modifier.height(16.dp))
        }

        PrimaryButton(
            text = stringResource(R.string.concern_submit),
            onClick = onSubmit,
            enabled = uiState.canSubmit,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = TextSecondary,
        fontSize = AppTextSize.Label,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun CategoryGrid(
    selected: ConcernCategory,
    onSelect: (ConcernCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ConcernCategory.entries.chunked(CATEGORY_COLUMNS).forEach { rowCategories ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowCategories.forEach { category ->
                    CategoryCard(
                        category = category,
                        selected = category == selected,
                        onClick = { onSelect(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: ConcernCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (selected) BrandBlue else TextPrimary
    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) SelectedRowBackground else White,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) BrandBlue else FieldBorder,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(category.icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(category.label),
                color = accent,
                fontSize = AppTextSize.Label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AttachFileButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SelectedRowBackground)
            .dashedBorder(color = BrandBlue.copy(alpha = 0.4f), cornerRadius = 14.dp)
            .clickable { /* TODO: open the file picker. */ },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.concern_attach_desc),
                tint = BrandBlue,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.concern_attach),
                color = BrandBlue,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ResponseTimeChip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OnboardingGreenDisc,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = stringResource(R.string.concern_response_desc),
                tint = PositiveGreen,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.concern_response_time),
                color = PositiveGreen,
                fontSize = AppTextSize.Caption,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Draws a dashed rounded-rectangle outline behind the content. */
private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
        ),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RaiseConcernPreview() {
    GRADit_RewampTheme {
        RaiseConcernContent(
            uiState = RaiseConcernUiState(
                selectedCategory = ConcernCategory.ATTENDANCE,
                subject = "Absent marked incorrectly on 22 Sep",
                description = "Aarav attended the Networks lab that day; the faculty register shows present. Please correct the record.",
            ),
            onBack = {},
            onCategorySelected = {},
            onSubjectChange = {},
            onDescriptionChange = {},
            onSubmit = {},
        )
    }
}
