package com.tvanime.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tvanime.app.data.local.entity.CrawlCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrawlCategoryDao {

    @Query("SELECT * FROM crawl_categories")
    fun observeAll(): Flow<List<CrawlCategoryEntity>>

    @Query("SELECT * FROM crawl_categories WHERE enabled = 1")
    fun observeEnabled(): Flow<List<CrawlCategoryEntity>>

    @Query("SELECT * FROM crawl_categories WHERE category = :category LIMIT 1")
    suspend fun getByCategory(category: String): CrawlCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrawlCategoryEntity)

    @Update
    suspend fun update(entity: CrawlCategoryEntity)

    @Query("UPDATE crawl_categories SET enabled = :enabled WHERE category = :category")
    suspend fun setEnabled(category: String, enabled: Boolean)

    @Query("UPDATE crawl_categories SET lastCrawledAt = :timestamp WHERE category = :category")
    suspend fun updateLastCrawled(category: String, timestamp: Long)
}