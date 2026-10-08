package com.vsca.vsnapvoicecollege.ui.components

/**
 * A selectable app language.
 *
 * @param code BCP-47 language code (e.g. "en", "hi").
 * @param nativeName Name in its own script (shown as-is, never translated).
 * @param englishName English name, used for the selector label / Apply button.
 */
data class Language(
    val code: String,
    val nativeName: String,
    val englishName: String,
)

/**
 * Languages the app ships with.
 *
 * TODO: move to a data/config source if this needs to vary per build or region.
 */
val SupportedLanguages: List<Language> = listOf(
    Language("en", "English", "English"),
    Language("hi", "हिन्दी", "Hindi"),
    Language("ta", "தமிழ்", "Tamil"),
    Language("te", "తెలుగు", "Telugu"),
    Language("kn", "ಕನ್ನಡ", "Kannada"),
    Language("ml", "മലയാളം", "Malayalam"),
    Language("mr", "मराठी", "Marathi"),
    Language("bn", "বাংলা", "Bengali"),
)
