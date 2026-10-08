package com.vsca.vsnapvoicecollege.ui.dashboard.faq

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.CategoryChip
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.theme.AppDimens
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

@Composable
fun FaqScreen(
    onBack: () -> Unit,
    onRaiseConcern: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val faqs = listOf(
        stringResource(R.string.faq_q1) to stringResource(R.string.faq_a1),
        stringResource(R.string.faq_q2) to stringResource(R.string.faq_a2),
        stringResource(R.string.faq_q3) to stringResource(R.string.faq_a3),
        stringResource(R.string.faq_q4) to stringResource(R.string.faq_a4),
        stringResource(R.string.faq_q5) to stringResource(R.string.faq_a5),
        stringResource(R.string.faq_q6) to stringResource(R.string.faq_a6),
    )
    val categories = listOf(
        stringResource(R.string.faq_cat_all),
        stringResource(R.string.faq_cat_account),
        stringResource(R.string.faq_cat_fees),
        stringResource(R.string.faq_cat_attendance),
        stringResource(R.string.faq_cat_more),
    )

    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(0) }
    var expandedQuestion by rememberSaveable { mutableStateOf(faqs.first().first) }

    val visibleFaqs = faqs.filter { it.first.contains(query, ignoreCase = true) }

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
                text = stringResource(R.string.faq_title),
                color = TextPrimary,
                fontSize = AppTextSize.H4,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(Modifier.height(16.dp))
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            hint = stringResource(R.string.faq_search_hint),
        )

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            categories.forEachIndexed { index, category ->
                CategoryChip(
                    text = category,
                    selected = index == selectedCategory,
                    onClick = { selectedCategory = index },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        visibleFaqs.forEachIndexed { index, (question, answer) ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            FaqCard(
                question = question,
                answer = answer,
                expanded = expandedQuestion == question,
                onToggle = {
                    expandedQuestion = if (expandedQuestion == question) "" else question
                },
            )
        }

        Spacer(Modifier.height(20.dp))
        HelpCard(onRaiseConcern = onRaiseConcern)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FaqCard(
    question: String,
    answer: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (expanded) SelectedRowBackground else White,
        border = BorderStroke(
            width = if (expanded) 1.5.dp else 1.dp,
            color = if (expanded) BrandBlue else FieldBorder,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = question,
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(12.dp))
                ToggleCircle(expanded)
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = answer,
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun ToggleCircle(expanded: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (expanded) BrandBlue else BadgeNeutralBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (expanded) R.drawable.ic_remove else R.drawable.ic_add),
            contentDescription = stringResource(
                if (expanded) R.string.collapse_desc else R.string.expand_desc,
            ),
            tint = if (expanded) White else TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun HelpCard(onRaiseConcern: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = SelectedRowBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.faq_help_title),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.faq_help_body),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.faq_raise_concern),
                    onClick = onRaiseConcern,
                    modifier = Modifier.weight(1f),
                )
                CallOfficeButton(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CallOfficeButton(modifier: Modifier = Modifier) {
    Surface(
        onClick = { /* TODO: call office */ },
        modifier = modifier.height(AppDimens.ButtonHeight),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = White,
        border = BorderStroke(1.dp, BrandBlue),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_phone),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.faq_call_office),
                color = BrandBlue,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
