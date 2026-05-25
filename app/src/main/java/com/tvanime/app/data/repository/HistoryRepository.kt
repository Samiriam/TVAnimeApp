package com.tvanime.app.data.repository

import com.tvanime.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeAll(): Flow<List<HistoryEntity>>
    suspend fun saveProgress(contentId: String, positionMs: Long, durationMs: Long)
    suspend fun remove(entry: HistoryEntity)
    suspend fun clearAll()
}
