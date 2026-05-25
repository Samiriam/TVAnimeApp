package com.tvanime.app.data.repository

import com.tvanime.app.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeAll(): Flow<List<FavoriteEntity>>
    fun isFavorite(id: String): Flow<Boolean>
    suspend fun add(id: String)
    suspend fun remove(id: String)
}
