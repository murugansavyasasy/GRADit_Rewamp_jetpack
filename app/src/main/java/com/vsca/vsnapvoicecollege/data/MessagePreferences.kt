package com.vsca.vsnapvoicecollege.data

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the user's recent message searches (newest first) so they can be re-run
 * from the search screen.
 */
object MessagePreferences {
    private const val PREFS_NAME = "gradit_prefs"
    private const val KEY_RECENT_SEARCHES = "message_recent_searches"
    private const val SEPARATOR = "\u001F"
    private const val MAX_RECENT = 5

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recentSearches(context: Context): List<String> =
        prefs(context).getString(KEY_RECENT_SEARCHES, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()

    /** Moves [query] to the top of the list, keeping at most [MAX_RECENT]. */
    fun addRecentSearch(context: Context, query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return recentSearches(context)
        val updated = (listOf(trimmed) + recentSearches(context).filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_RECENT)
        save(context, updated)
        return updated
    }

    fun removeRecentSearch(context: Context, query: String): List<String> {
        val updated = recentSearches(context) - query
        save(context, updated)
        return updated
    }

    fun clearRecentSearches(context: Context) {
        prefs(context).edit { remove(KEY_RECENT_SEARCHES) }
    }

    private fun save(context: Context, searches: List<String>) {
        prefs(context).edit { putString(KEY_RECENT_SEARCHES, searches.joinToString(SEPARATOR)) }
    }
}
