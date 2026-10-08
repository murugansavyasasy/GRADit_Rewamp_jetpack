package com.vsca.vsnapvoicecollege.ui.dashboard.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.vsca.vsnapvoicecollege.ui.components.AppCard
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.NumberBadge
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.SearchFieldBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

private data class PolicySection(@param:StringRes val title: Int, @param:StringRes val body: Int)

private val policySections = listOf(
    PolicySection(R.string.privacy_s1_title, R.string.privacy_s1_body),
    PolicySection(R.string.privacy_s2_title, R.string.privacy_s2_body),
    PolicySection(R.string.privacy_s3_title, R.string.privacy_s3_body),
    PolicySection(R.string.privacy_s4_title, R.string.privacy_s4_body),
    PolicySection(R.string.privacy_s5_title, R.string.privacy_s5_body),
    PolicySection(R.string.privacy_s6_title, R.string.privacy_s6_body),
)

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    onReadTerms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedIndex by rememberSaveable { mutableIntStateOf(1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        LegalTopBar(title = stringResource(R.string.privacy_title), onBack = onBack, showDownload = true)

        Spacer(Modifier.height(16.dp))
        SummaryCard()

        Spacer(Modifier.height(16.dp))
        AppCard {
            Column {
                policySections.forEachIndexed { index, section ->
                    if (index > 0) HorizontalDivider(color = FieldBorder)
                    PolicyAccordion(
                        number = index + 1,
                        title = stringResource(section.title),
                        body = stringResource(section.body),
                        expanded = expandedIndex == index,
                        onToggle = { expandedIndex = if (expandedIndex == index) -1 else index },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        ContactCard()

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.privacy_read_terms),
            color = BrandBlue,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onReadTerms),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryCard() {
    Surface(shape = RoundedCornerShape(20.dp), color = SelectedRowBackground) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield),
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.privacy_summary_title),
                    color = TextPrimary,
                    fontSize = AppTextSize.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.privacy_summary_body),
                    color = TextSecondary,
                    fontSize = AppTextSize.Caption,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.privacy_meta),
                    color = BrandBlue,
                    fontSize = AppTextSize.Caption,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PolicyAccordion(
    number: Int,
    title: String,
    body: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberBadge(number)
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                painter = painterResource(
                    if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
                ),
                contentDescription = stringResource(
                    if (expanded) R.string.collapse_desc else R.string.expand_desc,
                ),
                tint = TextMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        if (expanded) {
            Text(
                text = body,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun ContactCard() {
    Surface(shape = RoundedCornerShape(16.dp), color = SearchFieldBackground) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_email),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.privacy_contact),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
internal fun LegalTopBar(
    title: String,
    onBack: () -> Unit,
    showDownload: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BackButton(onClick = onBack)
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = AppTextSize.H4,
            fontWeight = FontWeight.ExtraBold,
        )
        if (showDownload) {
            Surface(
                onClick = { /* TODO: download PDF */ },
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = White,
                border = BorderStroke(1.dp, FieldBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = stringResource(R.string.privacy_download_desc),
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
