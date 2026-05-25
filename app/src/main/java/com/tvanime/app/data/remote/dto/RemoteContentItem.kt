package com.tvanime.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RemoteContentItem(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val mediaType: String,
    val genres: List<String>,
    val year: Int,
    val communityRating: Float,
    val videoUrl: String,
    val subtitleUrl: String?,
    val sourceName: String
)
