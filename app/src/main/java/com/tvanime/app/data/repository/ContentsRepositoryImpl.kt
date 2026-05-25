package com.tvanime.app.data.repository

import com.tvanime.app.data.local.dao.ContentDao
import com.tvanime.app.data.remote.api.SourceApi
import com.tvanime.app.data.remote.dto.RemoteContentItem
import com.tvanime.app.domain.model.ContentItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementacion de ContentsRepository.
 * Combina fuente remota + base de datos local Room.
 */
class ContentsRepositoryImpl(
    private val api: SourceApi,
    private val contentDao: ContentDao
) : ContentsRepository {

    override fun observeCatalog(): Flow<List<ContentItem>> =
        contentDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getDetail(id: String): ContentItem? =
        contentDao.getById(id)?.toDomain()

    override suspend fun syncCatalog(remoteItems: List<RemoteContentItem>) {
        val entities = remoteItems.map { it.toEntity() }
        contentDao.insertAll(entities)
    }

    override fun observeByType(type: String): Flow<List<ContentItem>> =
        contentDao.getByType(type).map { list -> list.map { it.toDomain() } }
}
