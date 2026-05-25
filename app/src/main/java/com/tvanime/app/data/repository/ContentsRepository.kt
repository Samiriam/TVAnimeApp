package com.tvanime.app.data.repository

import com.tvanime.app.data.local.entity.FavoriteEntity
import com.tvanime.app.data.local.entity.HistoryEntity
import com.tvanime.app.data.remote.dto.RemoteContentItem
import com.tvanime.app.domain.model.ContentItem
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de contenido y catalogo.
 */
interface ContentsRepository {
    fun observeCatalog(): Flow<List<ContentItem>>
    suspend fun getDetail(id: String): ContentItem?
    suspend fun syncCatalog(remoteItems: List<RemoteContentItem>)

    fun observeByType(type: String): Flow<List<ContentItem>>
    fun observeHistory(): Flow<List<HistoryEntity>>
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    suspend fun addFavorite(id: String)
    suspend fun removeFavorite(id: String)
    fun isFavorite(id: String): Flow<Boolean>
}
