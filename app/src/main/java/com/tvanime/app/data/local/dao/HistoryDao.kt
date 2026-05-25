package com.tvanime.app.data.local.dao

import androidx.room.*
import com.tvanime.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY playedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE contentId = :id LIMIT 1")
    suspend fun getById(id: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: HistoryEntity)

    @Delete
    suspend fun remove(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
