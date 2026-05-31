package com.tvanime.app.data.settings

import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tvanime.app.worker.ContentSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistSyncScheduler @Inject constructor(
    private val workManager: WorkManager
) {

    fun schedulePeriodicSync(config: PlaylistSyncConfig) {
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ContentSyncWorker>(4, TimeUnit.HOURS)
                .setInputData(config.toWorkerData())
                .build()
        )
    }

    fun requestImmediateSync(config: PlaylistSyncConfig, keepExisting: Boolean = false) {
        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            if (keepExisting) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ContentSyncWorker>()
                .setInputData(config.toWorkerData())
                .build()
        )
    }

    private fun PlaylistSyncConfig.toWorkerData(): Data = Data.Builder()
        .putString(ContentSyncWorker.KEY_SOURCE, source.name)
        .putString(ContentSyncWorker.KEY_M3U_URL, remoteUrl)
        .putString(ContentSyncWorker.KEY_ASSET_NAME, assetName)
        .build()

    companion object {
        private const val PERIODIC_SYNC_WORK_NAME = "playlist_periodic_sync"
        private const val IMMEDIATE_SYNC_WORK_NAME = "playlist_immediate_sync"
    }
}
