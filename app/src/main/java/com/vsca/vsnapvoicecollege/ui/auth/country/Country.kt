package com.vsca.vsnapvoicecollege.ui.auth.country

/**
 * A selectable country.
 *
 * @param iso ISO 3166-1 alpha-2 code (e.g. "IN") — also shown as the row badge.
 * @param name Display name.
 * @param dialCode International dialing code (e.g. "+91").
 * @param currency Short currency label shown in the sub-line (e.g. "INR ₹", "AUD").
 * @param popular Whether to feature the country in the "popular" shortcuts row.
 */
data class Country(
    val iso: String,
    val name: String,
    val dialCode: String,
    val currency: String,
    val popular: Boolean = false,
) {
    /** The sub-line shown under the name, e.g. "+91 · INR ₹". */
    val subtitle: String get() = "$dialCode · $currency"
}
