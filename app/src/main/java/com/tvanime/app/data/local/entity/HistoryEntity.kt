package com.tvanime.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val contentId: String,
    val positionMs: Long,
    val durationMs: Long,
    val playedAt: Long = System.currentTimeMillis()
)
