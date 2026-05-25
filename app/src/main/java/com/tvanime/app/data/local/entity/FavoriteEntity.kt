package com.tvanime.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val contentId: String,
    val addedAt: Long = System.currentTimeMillis()
)
