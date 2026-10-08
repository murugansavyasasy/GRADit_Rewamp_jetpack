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
        val detected = "IN"
        _uiState.update {
            it.copy(
                countries = SUPPORTED_COUNTRIES,
                detectedIso = detected,
                selectedIso = detected,
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
            Country(iso = "IN", name = "India", dialCode = "+91"),
            Country(iso = "AE", name = "United Arab Emirates", dialCode = "+971"),
            Country(iso = "SA", name = "Saudi Arabia", dialCode = "+966"),
            Country(iso = "SG", name = "Singapore", dialCode = "+65"),
            Country(iso = "GB", name = "United Kingdom", dialCode = "+44"),
            Country(iso = "US", name = "United States", dialCode = "+1"),
        )
    }
}
