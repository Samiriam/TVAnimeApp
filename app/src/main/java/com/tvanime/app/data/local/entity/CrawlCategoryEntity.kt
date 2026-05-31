package com.tvanime.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crawl_categories")
data class CrawlCategoryEntity(
    @PrimaryKey val category: String,
    val enabled: Boolean = true,
    val sites: String = "",           // JSON array ["site1", "site2"]
    val lastCrawledAt: Long = 0L,
    val crawlIntervalHours: Int = 6,
    val createdAt: Long = System.currentTimeMillis()
)