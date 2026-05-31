package com.tvanime.app.domain.model

data class DetectedMedia(
    val url: String,
    val mediaType: String,
    val format: String,
    val sourceName: String,
    val referer: String,
    val label: String = "",
    val server: String = "directo",
    val quality: String? = null,
    val variant: String? = null,
    val language: String? = null,
    val isDirect: Boolean = true,
    val requiresResolver: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val priority: Int = 100,
    val diagnostics: List<String> = emptyList(),
    val compatibilityScore: Int = 0
)

data class ExtractionResult(
    val pageUrl: String,
    val sourceHost: String,
    val title: String,
    val candidates: List<DetectedMedia>
)

data class RecurringSite(
    val url: String,
    val category: String = "Recurrente",
    val enabled: Boolean = true
)
