package com.tvanime.app.data.repository

import com.tvanime.app.data.local.dao.FavoriteDao
import com.tvanime.app.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de favoritos — implementación local (Room).
 */
class FavoritesRepositoryImpl(
    private val dao: FavoriteDao
) : FavoritesRepository {

    override fun observeAll(): Flow<List<FavoriteEntity>> = dao.observeAll()
    override fun isFavorite(id: String): Flow<Boolean> = dao.isFavorite(id)
    override suspend fun add(id: String) {
        dao.add(FavoriteEntity(id))
    }

    override suspend fun remove(id: String) {
        dao.remove(FavoriteEntity(id))
    }
}
