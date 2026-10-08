package com.vsca.vsnapvoicecollege.ui.auth.country

/**
 * A selectable country.
 *
 * @param iso ISO 3166-1 alpha-2 code (e.g. "IN") — also shown as the row badge.
 * @param name Display name.
 * @param dialCode International dialing code (e.g. "+91").
 */
data class Country(
    val iso: String,
    val name: String,
    val dialCode: String,
)
