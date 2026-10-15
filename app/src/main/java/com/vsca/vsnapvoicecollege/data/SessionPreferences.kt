package com.vsca.vsnapvoicecollege.data

import android.content.Context
import androidx.core.content.edit
import com.vsca.vsnapvoicecollege.ui.auth.roleselection.Role

/**
 * Persists whether the user has an active signed-in session, so the app can
 * auto-login (skip the sign-in flow) on the next launch until they sign out.
 * Also remembers which [Role] the user continued as, which drives the role-specific
 * parts of the app shell (e.g. the bottom navigation tabs).
 */
object SessionPreferences {
    private const val PREFS_NAME = "gradit_prefs"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_ROLE = "role"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(context: Context, loggedIn: Boolean) {
        prefs(context).edit { putBoolean(KEY_LOGGED_IN, loggedIn) }
    }

    /** The role chosen at sign in; defaults to [Role.STUDENT] for sessions saved before roles were stored. */
    fun getRole(context: Context): Role {
        val name = prefs(context).getString(KEY_ROLE, null)
        return Role.entries.firstOrNull { it.name == name } ?: Role.STUDENT
    }

    fun setRole(context: Context, role: Role) {
        prefs(context).edit { putString(KEY_ROLE, role.name) }
    }
}
