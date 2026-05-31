package com.tvanime.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tvanime.app.data.crawl.CrawlService
import com.tvanime.app.data.local.dao.ContentDao
import com.tvanime.app.data.local.dao.CrawlCategoryDao
import com.tvanime.app.domain.model.CategoryConfig
import com.tvanime.app.domain.model.SiteConfig
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class CrawlWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val crawlService = CrawlService(context)
    private val crawlCategoryDao: CrawlCategoryDao = com.tvanime.app.data.local.TVAnimeDatabase.getInstance(context).crawlCategoryDao()
    private val contentDao: ContentDao = com.tvanime.app.data.local.TVAnimeDatabase.getInstance(context).contentDao()

    override suspend fun doWork(): Result {
        return try {
            val categories = crawlCategoryDao.observeEnabled().first()
            if (categories.isEmpty()) return Result.success()

            for (catEntity in categories) {
                val categoryConfig = CategoryConfig.DEFAULT.find { it.category == catEntity.category }
                    ?: continue

                val sites = getSitesForCategory(catEntity.category)
                if (sites.isEmpty()) continue

                val result = crawlService.crawlCategory(catEntity.category, sites)

                if (result.items.isNotEmpty()) {
                    val existingIds = contentDao.observeAll().first().associateBy { it.id }
                    val entities = result.items.mapNotNull { item ->
                        val stableId = stableId(item.title, item.source)
                        if (existingIds.containsKey(stableId)) null
                        else com.tvanime.app.data.local.entity.ContentEntity(
                            id = stableId,
                            title = item.title,
                            description = "",
                            posterUrl = item.thumbnail,
                            backdropUrl = "",
                            mediaType = categoryConfig.mediaType,
                            genres = "[]",
                            year = item.year.toIntOrNull() ?: 0,
                            communityRating = item.rating,
                            videoUrl = "",
                            subtitleUrl = null,
                            sourceName = item.source,
                            syncedAt = System.currentTimeMillis()
                        )
                    }
                    if (entities.isNotEmpty()) {
                        contentDao.insertAll(entities)
                    }
                }

                crawlCategoryDao.updateLastCrawled(catEntity.category, System.currentTimeMillis())
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun getSitesForCategory(category: String): List<SiteConfig> {
        return when (category) {
            "anime" -> listOf(
                SiteConfig("AnimeFLV", "https://animeflv.net", "anime"),
                SiteConfig("Jkanime", "https://jkanime.net", "anime")
            )
            "movies" -> listOf(
                SiteConfig("Repelis", "https://repelis.live", "movies")
            )
            "series" -> listOf(
                SiteConfig("Repelis", "https://repelis.live", "series")
            )
            "documentaries" -> listOf(
                SiteConfig("Repelis", "https://repelis.live", "documentaries")
            )
            else -> emptyList()
        }
    }

    private fun stableId(title: String, source: String): String {
        val input = "${title.lowercase().trim()}_${source.lowercase().trim()}"
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val WORK_NAME = "crawl_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<CrawlWorker>(
                6, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}