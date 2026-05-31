package com.tvanime.app.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfig(): PlaylistSyncConfig {
        val source = preferences.getString(KEY_SOURCE, PlaylistSource.DEMO.name)
            ?.let { runCatching { PlaylistSource.valueOf(it) }.getOrNull() }
            ?: PlaylistSource.DEMO

        return PlaylistSyncConfig(
            source = source,
            remoteUrl = preferences.getString(KEY_REMOTE_URL, "").orEmpty(),
            assetName = DEMO_ASSET_NAME
        )
    }

    fun saveConfig(config: PlaylistSyncConfig) {
        preferences.edit()
            .putString(KEY_SOURCE, config.source.name)
            .putString(KEY_REMOTE_URL, config.remoteUrl.trim())
            .apply()
    }

    companion object {
        const val DEMO_ASSET_NAME = "playlist_demo.m3u"

        private const val PREFS_NAME = "playlist_settings"
        private const val KEY_SOURCE = "source"
        private const val KEY_REMOTE_URL = "remote_url"
    }
}

data class PlaylistSyncConfig(
    val source: PlaylistSource,
    val remoteUrl: String = "",
    val assetName: String = PlaylistSettingsStore.DEMO_ASSET_NAME
)

enum class PlaylistSource {
    DEMO,
    REMOTE_URL
}
