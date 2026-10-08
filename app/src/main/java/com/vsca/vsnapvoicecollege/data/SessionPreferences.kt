package com.vsca.vsnapvoicecollege.data

import android.content.Context
import androidx.core.content.edit

/**
 * Persists whether the user has an active signed-in session, so the app can
 * auto-login (skip the sign-in flow) on the next launch until they sign out.
 */
object SessionPreferences {
    private const val PREFS_NAME = "gradit_prefs"
    private const val KEY_LOGGED_IN = "logged_in"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(context: Context, loggedIn: Boolean) {
        prefs(context).edit { putBoolean(KEY_LOGGED_IN, loggedIn) }
    }
}
