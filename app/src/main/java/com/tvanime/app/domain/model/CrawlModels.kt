package com.tvanime.app.domain.model

data class CrawlItem(
    val title: String,
    val thumbnail: String,
    val year: String,
    val rating: Float,
    val detailUrl: String,
    val category: String,
    val source: String,
    val syncedAt: Long = System.currentTimeMillis()
)

data class CrawlResult(
    val category: String,
    val site: String,
    val items: List<CrawlItem>,
    val crawledAt: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

data class CrawlStatus(
    val isRunning: Boolean = false,
    val lastCrawl: Long = 0L,
    val currentCategory: String = "",
    val progress: Float = 0f,
    val itemsFound: Int = 0
)