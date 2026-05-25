package com.tvanime.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvanime.app.data.parser.M3uPlaylistParser
import com.tvanime.app.data.repository.ContentsRepository
import com.tvanime.app.data.remote.dto.RemoteContentItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker de WorkManager que descarga y sincroniza el catálogo
 * desde una URL M3U autorizada cada cierto intervalo.
 *
 * Configuración en el hilt de la app:
 *   PeriodicWorkRequestBuilder<ContentSyncWorker>(4, TimeUnit.HOURS)
 */
@HiltWorker
class ContentSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val parser: M3uPlaylistParser,
    private val repo: ContentsRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_M3U_URL = "key_m3u_url"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val m3uUrl = inputData.getString(KEY_M3U_URL).orEmpty()
        if (m3uUrl.isBlank()) return@withContext Result.failure()

        return@withContext runCatching {
            val items = parser.parseFromUrl(m3uUrl)
            val remoteItems = items.map { playlistItem ->
                RemoteContentItem(
                    id = playlistItem.url,           // URL como id único
                    title = playlistItem.title,
                    description = "Fuente: ${playlistItem.group.ifEmpty { playlistItem.url }}",
                    posterUrl = playlistItem.logoUrl,
                    backdropUrl = playlistItem.logoUrl,
                    mediaType = "OTHER",
                    genres = listOf(playlistItem.group).filter { it.isNotBlank() },
                    year = 0,
                    communityRating = 0f,
                    videoUrl = playlistItem.url,
                    subtitleUrl = null,
                    sourceName = "M3U Sync"
                )
            }
            repo.syncCatalog(remoteItems)
            Result.success()
        }.getOrElse { e ->
            e.printStackTrace()
            Result.retry()
        }
    }
}
