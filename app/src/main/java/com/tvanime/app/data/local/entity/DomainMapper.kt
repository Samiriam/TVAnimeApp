package com.tvanime.app.domain.model

import com.tvanime.app.data.local.entity.ContentEntity

fun ContentEntity.toDomain(): Content {
    val type = runCatching { MediaType.valueOf(mediaType) }
        .getOrDefault(MediaType.SERIES)
    return Content(
        id = id,
        title = title,
        description = description,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        mediaType = type,
        genres = genres.split(",").filter { it.isNotBlank() },
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
