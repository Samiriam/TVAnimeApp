package com.tvanime.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tvanime.app.data.settings.PlaylistSettingsStore
import com.tvanime.app.data.settings.PlaylistSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TVAnimeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var playlistSettingsStore: PlaylistSettingsStore

    @Inject
    lateinit var playlistSyncScheduler: PlaylistSyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        val config = playlistSettingsStore.getConfig()
        playlistSyncScheduler.schedulePeriodicSync(config)
        playlistSyncScheduler.requestImmediateSync(config, keepExisting = true)
    }
}