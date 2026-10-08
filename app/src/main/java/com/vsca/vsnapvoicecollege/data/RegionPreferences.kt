package com.vsca.vsnapvoicecollege.data

import android.content.Context
import androidx.core.content.edit

/**
 * Persists whether the user has confirmed their region/country (by tapping
 * "Continue" on the country screen). Until they do, the app reopens the country
 * screen on the next launch instead of proceeding to sign in.
 */
object RegionPreferences {
    private const val PREFS_NAME = "gradit_prefs"
    private const val KEY_REGION_SELECTED = "region_selected"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isSelected(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REGION_SELECTED, false)

    fun setSelected(context: Context, selected: Boolean) {
        prefs(context).edit { putBoolean(KEY_REGION_SELECTED, selected) }
    }
}
