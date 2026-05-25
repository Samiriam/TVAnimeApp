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
