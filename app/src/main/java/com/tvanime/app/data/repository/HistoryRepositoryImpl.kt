package com.tvanime.app.data.repository

import com.tvanime.app.data.local.dao.HistoryDao
import com.tvanime.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de historial de reproducción — implementación local (Room).
 */
class HistoryRepositoryImpl(
    private val dao: HistoryDao
) : HistoryRepository {

    override fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()

    override suspend fun saveProgress(contentId: String, positionMs: Long, durationMs: Long) {
        dao.save(HistoryEntity(contentId = contentId, positionMs = positionMs, durationMs = durationMs))
    }

    override suspend fun remove(entry: HistoryEntity) = dao.remove(entry)
    override suspend fun clearAll() = dao.clearAll()
}
