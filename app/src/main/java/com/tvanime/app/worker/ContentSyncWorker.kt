package com.tvanime.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvanime.app.data.local.dao.ContentDao
import com.tvanime.app.data.local.entity.ContentEntity
import com.tvanime.app.data.parser.M3uPlaylistParser
import com.tvanime.app.domain.model.MediaType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class ContentSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val contentDao: ContentDao,
    private val m3uParser: M3uPlaylistParser
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val source = inputData.getString(KEY_SOURCE) ?: return@withContext Result.failure()
        val m3uUrl = inputData.getString(KEY_M3U_URL)
        val assetName = inputData.getString(KEY_ASSET_NAME)

        try {
            val items = when {
                !m3uUrl.isNullOrBlank() -> {
                    m3uParser.parseFromUrl(m3uUrl)
                }
                else -> {
                    val raw = applicationContext.assets.open(assetName ?: "playlist_demo.m3u").bufferedReader().use { it.readText() }
                    m3uParser.parseAsset(raw)
                }
            }
            items.forEach { item ->
                contentDao.insert(ContentEntity(
                    id = item.url,
                    title = item.title,
                    description = "",
                    posterUrl = item.logoUrl,
                    backdropUrl = "",
                    mediaType = MediaType.OTHER.name,
                    genres = item.group,
                    year = 0,
                    communityRating = 0f,
                    videoUrl = item.url,
                    subtitleUrl = null,
                    sourceName = item.group.ifBlank { "M3U" },
                    syncedAt = System.currentTimeMillis()
                ))
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_SOURCE = "source"
        const val KEY_M3U_URL = "m3u_url"
        const val KEY_ASSET_NAME = "asset_name"
    }
}