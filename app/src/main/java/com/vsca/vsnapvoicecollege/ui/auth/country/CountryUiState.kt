package com.vsca.vsnapvoicecollege.ui.auth.country

/**
 * Immutable UI state for the country selection screen.
 */
data class CountryUiState(
    val query: String = "",
    val countries: List<Country> = emptyList(),
    val selectedIso: String? = null,
    val detectedIso: String? = null,
) {
    /** Countries filtered by the current [query] (matches name or dial code). */
    val visibleCountries: List<Country>
        get() = if (query.isBlank()) {
            countries
        } else {
            countries.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.dialCode.contains(query)
            }
        }

    val selectedCountry: Country?
        get() = countries.firstOrNull { it.iso == selectedIso }

    val detectedCountry: Country?
        get() = countries.firstOrNull { it.iso == detectedIso }
}
