package com.tvanime.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tvanime.app.data.local.dao.ContentDao
import com.tvanime.app.data.local.dao.CrawlCategoryDao
import com.tvanime.app.data.local.dao.FavoriteDao
import com.tvanime.app.data.local.dao.HistoryDao
import com.tvanime.app.data.local.entity.ContentEntity
import com.tvanime.app.data.local.entity.CrawlCategoryEntity
import com.tvanime.app.data.local.entity.FavoriteEntity
import com.tvanime.app.data.local.entity.HistoryEntity

@Database(
    entities = [ContentEntity::class, HistoryEntity::class, FavoriteEntity::class, CrawlCategoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TVAnimeDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun crawlCategoryDao(): CrawlCategoryDao

    companion object {
        const val DATABASE_NAME = "tv_anime.db"

        @Volatile
        private var INSTANCE: TVAnimeDatabase? = null

        fun getInstance(context: Context): TVAnimeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TVAnimeDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
