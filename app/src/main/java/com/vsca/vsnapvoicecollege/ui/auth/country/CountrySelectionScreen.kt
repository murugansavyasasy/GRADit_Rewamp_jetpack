package com.vsca.vsnapvoicecollege.ui.auth.country

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.BackButton
import com.vsca.vsnapvoicecollege.ui.components.PoweredByRow
import com.vsca.vsnapvoicecollege.ui.components.PrimaryButton
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SelectableCard
import com.vsca.vsnapvoicecollege.ui.components.SelectionRadio
import com.vsca.vsnapvoicecollege.ui.components.SystemBarIcons
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeNeutralBackground
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.PositiveGreen
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White

/**
 * Country selection screen shown after onboarding. Lets the user search and pick
 * a country, then continue into the auth flow.
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
        Spacer(Modifier.height(16.dp))

        BackButton(onClick = onBack)

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

        Spacer(Modifier.height(20.dp))

        SearchBar(
            query = uiState.query,
            onQueryChange = onQueryChange,
            hint = stringResource(R.string.country_search_hint),
        )

        uiState.detectedCountry?.let { detected ->
            Spacer(Modifier.height(16.dp))
            DetectedLocationRow(countryName = detected.name)
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.visibleCountries, key = { it.iso }) { country ->
                CountryRow(
                    country = country,
                    selected = country.iso == uiState.selectedIso,
                    onClick = { onCountrySelected(country.iso) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val selected = uiState.selectedCountry
        PrimaryButton(
            text = if (selected != null) {
                stringResource(R.string.country_continue, selected.name)
            } else {
                stringResource(R.string.splash_get_started)
            },
            onClick = { selected?.let(onContinue) },
            enabled = selected != null,
        )

        Spacer(Modifier.height(20.dp))

        PoweredByRow(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textColor = TextSecondary,
            logoBackground = Color.Transparent,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetectedLocationRow(
    countryName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location_on),
            contentDescription = stringResource(R.string.location_content_desc),
            tint = PositiveGreen,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.country_detected, countryName),
            color = PositiveGreen,
            fontSize = AppTextSize.Caption,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CountryRow(
    country: Country,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableCard(selected = selected, onClick = onClick, modifier = modifier) {
        CountryBadge(iso = country.iso, selected = selected)

        Spacer(Modifier.width(14.dp))

        Text(
            text = country.name,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = AppTextSize.BodyLarge,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = country.dialCode,
            color = TextSecondary,
            fontSize = AppTextSize.Label,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.width(12.dp))

        SelectionRadio(selected = selected)
    }
}

@Composable
private fun CountryBadge(
    iso: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BrandBlue else BadgeNeutralBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iso,
            color = if (selected) White else TextPrimary,
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
                    Country("IN", "India", "+91"),
                    Country("AE", "United Arab Emirates", "+971"),
                    Country("US", "United States", "+1"),
                ),
                selectedIso = "IN",
                detectedIso = "IN",
            ),
            onBack = {},
            onQueryChange = {},
            onCountrySelected = {},
            onContinue = {},
        )
    }
}
