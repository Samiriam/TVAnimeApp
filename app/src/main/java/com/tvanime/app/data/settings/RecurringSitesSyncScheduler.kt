package com.tvanime.app.data.settings

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tvanime.app.worker.RecurringSitesSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringSitesSyncScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun schedulePeriodicSync() {
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<RecurringSitesSyncWorker>(6, TimeUnit.HOURS).build()
        )
    }

    fun requestImmediateSync() {
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RecurringSitesSyncWorker>().build()
        )
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "recurring_sites_periodic_sync"
        private const val IMMEDIATE_WORK_NAME = "recurring_sites_immediate_sync"
    }
}
