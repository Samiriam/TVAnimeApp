package com.tvanime.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentItem(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val mediaType: MediaType,
    val genres: List<String> = emptyList(),
    val year: Int = 0,
    val communityRating: Float = 0f,
    val videoUrl: String = "",
    val subtitleUrl: String? = null,
    val sourceName: String = "",
    val syncedAt: Long = 0L
) : Parcelable
