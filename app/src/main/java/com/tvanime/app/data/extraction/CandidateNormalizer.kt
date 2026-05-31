package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import java.net.URI
import java.net.URLDecoder
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
        val cleaned = extractNestedPlayableUrl(cleanUrl(rawUrl)) ?: cleanUrl(rawUrl)
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
            .substringBefore("#")
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .removeSurrounding("`")
            .replace("\\u0026", "&")
            .replace("\\x26", "&", ignoreCase = true)
            .replace("\\u003d", "=", ignoreCase = true)
            .replace("\\u003f", "?", ignoreCase = true)
            .replace("\\u003a", ":", ignoreCase = true)
            .replace("\\u002f", "/", ignoreCase = true)
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .replace("%3A", ":", ignoreCase = true)
            .replace("%2F", "/", ignoreCase = true)
            .replace("%3F", "?", ignoreCase = true)
            .replace("%3D", "=", ignoreCase = true)
            .replace("%26", "&", ignoreCase = true)

        return decodeBase64Url(trimmed) ?: decodeUrlEncoded(trimmed) ?: trimmed
    }

    private fun extractNestedPlayableUrl(value: String): String? {
        val decoded = decodeUrlEncoded(value) ?: value
        val innerDecoded = decoded.lastNestedHttpUrl()
        val innerRaw = value.lastNestedHttpUrl()
        return innerDecoded?.let { DIRECT_URL_PATTERN.find(it)?.value }
            ?: innerRaw?.let { DIRECT_URL_PATTERN.find(it)?.value }
            ?: DIRECT_URL_PATTERN.findAll(decoded).lastOrNull()?.value
            ?: DIRECT_URL_PATTERN.findAll(value).lastOrNull()?.value
            ?: decodeBase64Url(value)?.let { DIRECT_URL_PATTERN.find(it)?.value ?: it }
    }

    private fun String.lastNestedHttpUrl(): String? {
        val lastHttps = lastIndexOf("https://")
        val lastHttp = lastIndexOf("http://")
        val start = maxOf(lastHttps, lastHttp)
        if (start <= 0) return null
        return substring(start)
    }

    private fun decodeUrlEncoded(value: String): String? {
        if (!value.contains("%")) return null
        return runCatching {
            URLDecoder.decode(value, Charsets.UTF_8.name()).trim()
        }.getOrNull()?.takeIf { it != value }
    }

    private fun decodeBase64Url(value: String): String? {
        if (!BASE64_PATTERN.matches(value) || value.length < 12) return null

        return runCatching {
            val normalized = value.replace('-', '+').replace('_', '/')
            val padded = normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, '=')
            val decoded = String(Base64.getDecoder().decode(padded), Charsets.UTF_8).trim()
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
        private val DIRECT_URL_PATTERN = Regex(
            "https?://[^\\s\\\"'<>]+\\.(?:m3u8|mp4|webm|mkv|mp3|aac|m4a|ogg)[^\\s\\\"'<>]*",
            RegexOption.IGNORE_CASE
        )
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
