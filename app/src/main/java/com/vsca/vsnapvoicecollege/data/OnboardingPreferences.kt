package com.vsca.vsnapvoicecollege.data

import android.content.Context
import androidx.core.content.edit

/**
 * Persists whether the one-time onboarding intro has been completed, so it is
 * shown only on a fresh install. Backed by [android.content.SharedPreferences].
 */
object OnboardingPreferences {
    private const val PREFS_NAME = "gradit_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setCompleted(context: Context) {
        prefs(context).edit { putBoolean(KEY_ONBOARDING_COMPLETED, true) }
    }
}
