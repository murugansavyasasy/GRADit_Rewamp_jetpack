package com.vsca.vsnapvoicecollege.ui.dashboard.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.AppCard
import com.vsca.vsnapvoicecollege.ui.components.NumberBadge
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

private data class TermsSection(@param:StringRes val title: Int, @param:StringRes val body: Int)

private val termsSections = listOf(
    TermsSection(R.string.terms_s1_title, R.string.terms_s1_body),
    TermsSection(R.string.terms_s2_title, R.string.terms_s2_body),
    TermsSection(R.string.terms_s3_title, R.string.terms_s3_body),
    TermsSection(R.string.terms_s4_title, R.string.terms_s4_body),
    TermsSection(R.string.terms_s5_title, R.string.terms_s5_body),
    TermsSection(R.string.terms_s6_title, R.string.terms_s6_body),
)

@Composable
fun TermsScreen(
    onBack: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var agreed by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        LegalTopBar(title = stringResource(R.string.terms_title), onBack = onBack, showDownload = true)

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = SelectedRowBackground) {
                Text(
                    text = stringResource(R.string.terms_effective),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = BrandBlue,
                    fontSize = AppTextSize.Caption,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(R.string.terms_read_time),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
            )
        }

        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            AppCard {
                Column {
                    termsSections.forEachIndexed { index, section ->
                        if (index > 0) HorizontalDivider(color = FieldBorder)
                        TermsItem(
                            number = index + 1,
                            title = stringResource(section.title),
                            body = stringResource(section.body),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        HorizontalDivider(color = FieldBorder)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = agreementText(),
                color = TextPrimary,
                fontSize = AppTextSize.Label,
                lineHeight = 20.sp,
            )
        }

        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = stringResource(R.string.terms_accept),
            onClick = onAccept,
            enabled = agreed,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun agreementText(): androidx.compose.ui.text.AnnotatedString {
    val terms = stringResource(R.string.terms_agree_terms)
    val privacy = stringResource(R.string.terms_agree_privacy)
    val full = stringResource(R.string.terms_agree_prefix) + terms +
        stringResource(R.string.terms_agree_middle) + privacy
    return buildAnnotatedString {
        append(full)
        listOf(terms, privacy).forEach { token ->
            val start = full.indexOf(token)
            if (start >= 0) {
                addStyle(SpanStyle(color = BrandBlue, fontWeight = FontWeight.Bold), start, start + token.length)
            }
        }
    }
}

@Composable
private fun TermsItem(number: Int, title: String, body: String) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
        NumberBadge(number)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = TextSecondary, fontSize = AppTextSize.Caption, lineHeight = 20.sp)
        }
    }
}
