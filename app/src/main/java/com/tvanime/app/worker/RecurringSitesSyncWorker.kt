package com.tvanime.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvanime.app.data.remote.dto.RemoteContentItem
import com.tvanime.app.data.repository.ContentsRepository
import com.tvanime.app.data.repository.ExtractionRepository
import com.tvanime.app.data.settings.RecurringSitesStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RecurringSitesSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringSitesStore: RecurringSitesStore,
    private val extractionRepository: ExtractionRepository,
    private val contentsRepository: ContentsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sites = recurringSitesStore.getSites().filter { it.enabled }
        if (sites.isEmpty()) return@withContext Result.success()

        val remoteItems = mutableListOf<RemoteContentItem>()

        for (site in sites) {
            runCatching {
                val result = extractionRepository.extractFromPage(site.url)
                result.candidates.filter { it.isDirect }.forEachIndexed { index, candidate ->
                    remoteItems += RemoteContentItem(
                        id = candidate.url,
                        title = "${result.title} #${index + 1}",
                        description = "${site.category} · ${candidate.server} · ${candidate.format}",
                        posterUrl = "",
                        backdropUrl = "",
                        mediaType = "OTHER",
                        genres = listOf(site.category, candidate.server).filter { it.isNotBlank() },
                        year = 0,
                        communityRating = 0f,
                        videoUrl = candidate.url,
                        subtitleUrl = null,
                        sourceName = "Auto sitio recurrente"
                    )
                }
            }
        }

        if (remoteItems.isNotEmpty()) {
            contentsRepository.syncCatalog(remoteItems.distinctBy { it.id })
        }

        Result.success()
    }
}
