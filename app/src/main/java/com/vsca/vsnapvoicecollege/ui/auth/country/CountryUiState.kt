package com.vsca.vsnapvoicecollege.ui.auth.country

/**
 * Immutable UI state for the country selection screen.
 */
data class CountryUiState(
    val query: String = "",
    val countries: List<Country> = emptyList(),
    val selectedIso: String? = null,
    val detectedIso: String? = null,
    /** Human-readable location shown on the detected card, e.g. "Chennai, Tamil Nadu". */
    val detectedLocation: String = "",
) {
    val isSearching: Boolean get() = query.isNotBlank()

    /** Countries filtered by the current [query] (matches name, dial code or currency). */
    val visibleCountries: List<Country>
        get() = if (query.isBlank()) {
            countries
        } else {
            countries.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.dialCode.contains(query) ||
                    it.currency.contains(query, ignoreCase = true)
            }
        }

    /** All countries sorted A–Z and grouped by their first letter. */
    val groupedCountries: List<Pair<Char, List<Country>>>
        get() = visibleCountries
            .sortedBy { it.name }
            .groupBy { it.name.first().uppercaseChar() }
            .toList()

    val popularCountries: List<Country>
        get() = countries.filter { it.popular }

    val selectedCountry: Country?
        get() = countries.firstOrNull { it.iso == selectedIso }

    val detectedCountry: Country?
        get() = countries.firstOrNull { it.iso == detectedIso }
}
