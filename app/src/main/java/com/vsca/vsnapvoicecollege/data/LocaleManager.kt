package com.vsca.vsnapvoicecollege.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.vsca.vsnapvoicecollege.ui.components.Language
import com.vsca.vsnapvoicecollege.ui.components.SupportedLanguages

/**
 * App-wide language switching built on AndroidX per-app locales.
 *
 * [apply] changes the language and recreates the running activities so the whole
 * UI re-reads its string resources. The choice is persisted automatically by
 * AppCompat (via the AppLocalesMetadataHolderService on API < 33, and by the
 * system on API 33+), so it survives app restarts.
 */
object LocaleManager {

    /** Applies [code] (a BCP-47 language tag like "hi") as the app language. */
    fun apply(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }

    /** The currently applied language code, or "en" when none has been set. */
    fun currentCode(): String =
        AppCompatDelegate.getApplicationLocales()[0]?.language ?: SupportedLanguages.first().code

    /** The currently applied [Language], falling back to the first supported one. */
    fun currentLanguage(): Language {
        val code = currentCode()
        return SupportedLanguages.firstOrNull { it.code == code } ?: SupportedLanguages.first()
    }
}
