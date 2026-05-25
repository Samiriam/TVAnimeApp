package com.tvanime.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tvanime.app.data.local.dao.ContentDao
import com.tvanime.app.data.local.dao.FavoriteDao
import com.tvanime.app.data.local.dao.HistoryDao
import com.tvanime.app.data.local.entity.ContentEntity
import com.tvanime.app.data.local.entity.FavoriteEntity
import com.tvanime.app.data.local.entity.HistoryEntity

@Database(
    entities = [ContentEntity::class, HistoryEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TVAnimeDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "tv_anime.db"
    }
}
