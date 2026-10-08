package com.vsca.vsnapvoicecollege.ui.auth.country

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SectionLabel
import com.vsca.vsnapvoicecollege.ui.components.SelectableCard
import com.vsca.vsnapvoicecollege.ui.components.SelectionRadio
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.SearchFieldBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextMuted
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Country selection screen shown after onboarding. The user confirms the detected
 * country or picks another from the popular shortcuts / full A–Z list.
 */
@Composable
fun CountrySelectionScreen(
    onBack: () -> Unit,
    onContinue: (Country) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CountryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SystemBarIcons(darkIcons = true)

    CountrySelectionContent(
        uiState = uiState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onCountrySelected = viewModel::onCountrySelected,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun CountrySelectionContent(
    uiState: CountryUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onContinue: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.country_step),
                color = TextSecondary,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.country_title),
                color = TextPrimary,
                fontSize = AppTextSize.H2,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.country_subtitle),
                color = TextSecondary,
                fontSize = AppTextSize.Body,
                lineHeight = 21.sp,
            )

            uiState.detectedCountry?.takeIf { !uiState.isSearching }?.let { detected ->
                Spacer(Modifier.height(20.dp))
                DetectedCard(
                    country = detected,
                    location = uiState.detectedLocation,
                    selected = detected.iso == uiState.selectedIso,
                    onClick = { onCountrySelected(detected.iso) },
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.country_choose_another),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            SearchBar(
                query = uiState.query,
                onQueryChange = onQueryChange,
                hint = stringResource(R.string.country_search_hint),
            )

            if (uiState.isSearching) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(stringResource(R.string.country_results))
                    Text(
                        text = stringResource(R.string.country_matches, uiState.visibleCountries.size),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (uiState.visibleCountries.isEmpty()) {
                    NoMatchCard(query = uiState.query, onClearSearch = { onQueryChange("") })
                    Spacer(Modifier.height(16.dp))
                    PrivacyNote()
                } else {
                    uiState.visibleCountries.forEach { country ->
                        CountryRow(
                            country = country,
                            selected = country.iso == uiState.selectedIso,
                            onClick = { onCountrySelected(country.iso) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            } else {
                Spacer(Modifier.height(20.dp))
                SectionLabel(stringResource(R.string.country_popular))
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    uiState.popularCountries.forEach { country ->
                        PopularChip(
                            country = country,
                            selected = country.iso == uiState.selectedIso,
                            onClick = { onCountrySelected(country.iso) },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(stringResource(R.string.country_all))
                    Text(
                        text = stringResource(R.string.country_count, uiState.countries.size),
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                }
                Spacer(Modifier.height(12.dp))
                uiState.groupedCountries.forEach { (letter, group) ->
                    Text(
                        text = letter.toString(),
                        color = BrandBlue,
                        fontSize = AppTextSize.Label,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    group.forEach { country ->
                        CountryRow(
                            country = country,
                            selected = country.iso == uiState.selectedIso,
                            onClick = { onCountrySelected(country.iso) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        uiState.selectedCountry?.let { selected ->
            SelectedSummary(country = selected)
            Spacer(Modifier.height(12.dp))
        }

        PrimaryButton(
            text = stringResource(R.string.country_continue),
            onClick = { uiState.selectedCountry?.let(onContinue) },
            enabled = uiState.selectedCountry != null,
            trailingIconRes = R.drawable.ic_arrow_forward,
        )

        Spacer(Modifier.height(16.dp))
        PoweredByRow(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textColor = TextSecondary,
            logoBackground = Color.Transparent,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun NoMatchCard(query: String, onClearSearch: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SearchFieldBackground,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_language),
                    contentDescription = stringResource(R.string.country_globe_desc),
                    tint = TextSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.country_no_match, query),
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.country_no_match_hint),
                color = TextSecondary,
                fontSize = AppTextSize.Caption,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                onClick = onClearSearch,
                shape = RoundedCornerShape(12.dp),
                color = White,
                border = BorderStroke(1.dp, FieldBorder),
            ) {
                Text(
                    text = stringResource(R.string.country_clear_search),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = BrandBlue,
                    fontSize = AppTextSize.Label,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.country_privacy_note),
            color = TextMuted,
            fontSize = AppTextSize.Caption,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun DetectedCard(
    country: Country,
    location: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // Highlighted only once the user actually selects it — not pre-selected.
        color = if (selected) SelectedRowBackground else White,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) BrandBlue else FieldBorder,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_location_on),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.country_detected_label),
                    color = BrandBlue,
                    fontSize = AppTextSize.CaptionSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountryBadge(iso = country.iso, filled = true, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = country.name,
                        color = TextPrimary,
                        fontSize = AppTextSize.H6,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (location.isNotBlank()) "${country.subtitle} · $location" else country.subtitle,
                        color = TextSecondary,
                        fontSize = AppTextSize.Caption,
                    )
                }
                Spacer(Modifier.width(12.dp))
                SelectionRadio(selected = selected)
            }
        }
    }
}

@Composable
private fun PopularChip(country: Country, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) SelectedRowBackground else White,
        border = BorderStroke(1.dp, if (selected) BrandBlue else FieldBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CountryBadge(iso = country.iso, filled = selected, size = 28.dp, circle = true)
            Text(
                text = country.name,
                color = TextPrimary,
                fontSize = AppTextSize.Label,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CountryRow(country: Country, selected: Boolean, onClick: () -> Unit) {
    SelectableCard(selected = selected, onClick = onClick) {
        CountryBadge(iso = country.iso, filled = selected, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = country.name,
                color = TextPrimary,
                fontSize = AppTextSize.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = country.subtitle, color = TextSecondary, fontSize = AppTextSize.Caption)
        }
        Spacer(Modifier.width(12.dp))
        SelectionRadio(selected = selected)
    }
}

@Composable
private fun SelectedSummary(country: Country) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountryBadge(iso = country.iso, filled = true, size = 36.dp, cornerRadius = 10.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${country.name} · ${country.subtitle}",
            color = TextPrimary,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CountryBadge(
    iso: String,
    filled: Boolean,
    size: Dp,
    cornerRadius: Dp = 12.dp,
    circle: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(if (circle) CircleShape else RoundedCornerShape(cornerRadius))
            .background(if (filled) BrandBlue else BadgeNeutralBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iso,
            color = if (filled) White else TextPrimary,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CountrySelectionPreview() {
    GRADit_RewampTheme {
        CountrySelectionContent(
            uiState = CountryUiState(
                countries = listOf(
                    Country("IN", "India", "+91", "INR ₹"),
                    Country("AE", "United Arab Emirates", "+971", "AED", popular = true),
                    Country("SA", "Saudi Arabia", "+966", "SAR", popular = true),
                    Country("AU", "Australia", "+61", "AUD"),
                    Country("BH", "Bahrain", "+973", "BHD"),
                ),
                selectedIso = "IN",
                detectedIso = "IN",
                detectedLocation = "Chennai, Tamil Nadu",
            ),
            onBack = {},
            onQueryChange = {},
            onCountrySelected = {},
            onContinue = {},
        )
    }
}
