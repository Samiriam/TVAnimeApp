package com.tvanime.app.data.settings

import android.content.Context
import com.tvanime.app.domain.model.RecurringSite
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringSitesStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSites(): List<RecurringSite> = preferences.getString(KEY_URLS, "").orEmpty()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { line ->
            val parts = line.split("|", limit = 2)
            RecurringSite(
                url = parts[0].trim(),
                category = parts.getOrNull(1)?.trim().orEmpty().ifBlank { "Recurrente" }
            )
        }
        .toList()

    fun getRawText(): String = preferences.getString(KEY_URLS, "").orEmpty()

    fun saveRawText(value: String) {
        preferences.edit().putString(KEY_URLS, value.trim()).apply()
    }

    companion object {
        private const val PREFS_NAME = "recurring_sites"
        private const val KEY_URLS = "urls"
    }
}
