package com.tvanime.app.domain.model

data class DetectedMedia(
    val url: String,
    val mediaType: String,
    val format: String,
    val sourceName: String,
    val referer: String,
    val label: String = ""
)

data class ExtractionResult(
    val pageUrl: String,
    val sourceHost: String,
    val title: String,
    val candidates: List<DetectedMedia>
)
