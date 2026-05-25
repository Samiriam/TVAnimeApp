package com.tvanime.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content")
data class ContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val mediaType: String,     // "anime" | "movie" | "series"
    val genres: String,        // JSON array string
    val year: Int,
    val communityRating: Float,
    val videoUrl: String,      // URL del stream directo
    val subtitleUrl: String?,  // URL de subtítulos .vtt opcional
    val sourceName: String,    // Ej: "FuenteLocal", "PlaylistA"
    val syncedAt: Long
)
