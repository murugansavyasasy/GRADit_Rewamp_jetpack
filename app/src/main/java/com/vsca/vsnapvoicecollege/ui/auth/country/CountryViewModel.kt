package com.vsca.vsnapvoicecollege.ui.auth.country

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the state for the country selection screen.
 *
 * The country list and detected location are stubbed here; move them to a data
 * layer (repository + device locale / geo-IP) when that work lands.
 */
class CountryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CountryUiState())
    val uiState: StateFlow<CountryUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                countries = SUPPORTED_COUNTRIES,
                detectedIso = "IN",
                // No country is pre-selected — the user must choose one explicitly.
                selectedIso = null,
                detectedLocation = "Chennai, Tamil Nadu",
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onCountrySelected(iso: String) {
        _uiState.update { it.copy(selectedIso = iso) }
    }

    private companion object {
        // TODO: replace with the full list from a data source.
        val SUPPORTED_COUNTRIES = listOf(
            Country("IN", "India", "+91", "INR ₹"),
            Country("AE", "United Arab Emirates", "+971", "AED", popular = true),
            Country("SA", "Saudi Arabia", "+966", "SAR", popular = true),
            Country("SG", "Singapore", "+65", "SGD", popular = true),
            Country("LK", "Sri Lanka", "+94", "LKR", popular = true),
            Country("AU", "Australia", "+61", "AUD"),
            Country("BH", "Bahrain", "+973", "BHD"),
            Country("CA", "Canada", "+1", "CAD"),
            Country("DE", "Germany", "+49", "EUR"),
            Country("KW", "Kuwait", "+965", "KWD"),
            Country("MY", "Malaysia", "+60", "MYR"),
            Country("NP", "Nepal", "+977", "NPR"),
            Country("OM", "Oman", "+968", "OMR"),
            Country("QA", "Qatar", "+974", "QAR"),
            Country("GB", "United Kingdom", "+44", "GBP"),
            Country("US", "United States", "+1", "USD"),
        )
    }
}
