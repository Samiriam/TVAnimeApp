package com.tvanime.app.data.local.entity

import com.tvanime.app.data.local.entity.ContentEntity
import com.tvanime.app.data.remote.dto.RemoteContentItem
import com.tvanime.app.domain.model.ContentItem
import com.tvanime.app.domain.model.MediaType

fun ContentEntity.toDomain(): ContentItem {
    val type = runCatching { MediaType.valueOf(mediaType) }
        .getOrDefault(MediaType.OTHER)
    return ContentItem(
        id = id,
        title = title,
        description = description,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        mediaType = type,
        genres = genres.split(",").map { it.trim() }.filter { it.isNotBlank() },
        year = year,
        communityRating = communityRating,
        videoUrl = videoUrl,
        subtitleUrl = subtitleUrl,
        sourceName = sourceName,
        syncedAt = syncedAt
    )
}

fun RemoteContentItem.toEntity(): ContentEntity {
    return ContentEntity(
        id = id,
        title = title,
        description = description,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        mediaType = mediaType,
        genres = genres.joinToString(","),
        year = year,
        communityRating = communityRating,
        videoUrl = videoUrl,
        subtitleUrl = subtitleUrl,
        sourceName = sourceName,
        syncedAt = System.currentTimeMillis()
    )
}
