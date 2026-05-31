package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import java.net.URI
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CandidateNormalizer @Inject constructor(
    private val serverClassifier: ServerClassifier
) {

    fun normalize(
        rawUrl: String,
        pageUrl: URI,
        sourceName: String,
        explicitFormat: String? = null,
        label: String = "",
        diagnostics: List<String> = emptyList()
    ): DetectedMedia? {
        val cleaned = cleanUrl(rawUrl)
        if (cleaned.isBlank() || isNoise(cleaned)) return null

        val absoluteUrl = runCatching { pageUrl.resolve(cleaned).toString() }.getOrNull() ?: return null
        if (isNoise(absoluteUrl)) return null

        val server = serverClassifier.classify(absoluteUrl, explicitFormat)
        if (server.id == "directo" && server.format == "file" && !hasPlayableExtension(absoluteUrl)) return null
        val mediaType = when (server.format) {
            "audio" -> "audio"
            else -> "video"
        }

        return DetectedMedia(
            url = absoluteUrl,
            mediaType = mediaType,
            format = server.format,
            sourceName = sourceName,
            referer = pageUrl.toString(),
            label = label.ifBlank { server.label },
            server = server.id,
            isDirect = server.isDirect,
            requiresResolver = server.requiresResolver,
            headers = mapOf("Referer" to pageUrl.toString()),
            priority = server.priority,
            diagnostics = diagnostics + "server=${server.id}"
        )
    }

    fun cleanUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .replace("%3A", ":", ignoreCase = true)
            .replace("%2F", "/", ignoreCase = true)
            .replace("%3F", "?", ignoreCase = true)
            .replace("%3D", "=", ignoreCase = true)

        return decodeBase64Url(trimmed) ?: trimmed
    }

    private fun decodeBase64Url(value: String): String? {
        if (!BASE64_PATTERN.matches(value) || value.length < 12) return null

        return runCatching {
            val decoded = String(Base64.getDecoder().decode(value), Charsets.UTF_8).trim()
            decoded.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        }.getOrNull()
    }

    private fun isNoise(url: String): Boolean {
        val lower = url.lowercase()
        return NOISE_TOKENS.any { lower.contains(it) } || lower.startsWith("blob:") || lower.startsWith("javascript:")
    }

    private fun hasPlayableExtension(url: String): Boolean {
        val lower = url.lowercase()
        return listOf(".m3u8", ".mp4", ".webm", ".mkv", ".mp3", ".aac", ".m4a", ".ogg").any { lower.contains(it) }
    }

    companion object {
        private val BASE64_PATTERN = Regex("^[A-Za-z0-9+/=_-]+$")
        private val NOISE_TOKENS = listOf(
            "cloudflareinsights",
            "google-analytics",
            "googletagmanager",
            "facebook.net",
            "beacon.min.js",
            ".js?",
            "analytics",
            "pixel",
            "placeholder"
        )
    }
}
