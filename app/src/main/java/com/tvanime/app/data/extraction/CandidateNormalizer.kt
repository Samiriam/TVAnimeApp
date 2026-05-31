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
        
        if (server.id == "directo" && server.format == "file" && !hasPlayableExtension(absoluteUrl)) {
            if (!isEmbedUrl(absoluteUrl)) return null
        }
        
        val quality = detectQuality(absoluteUrl, label)
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
            quality = quality,
            isDirect = server.isDirect,
            requiresResolver = server.requiresResolver,
            headers = mapOf("Referer" to pageUrl.toString()),
            priority = server.priority,
            diagnostics = diagnostics + "server=${server.id}"
        )
    }
    
    private fun isEmbedUrl(url: String): Boolean {
        val lower = url.lowercase()
        return EMBED_SERVER_TOKENS.any { lower.contains(it) } ||
            lower.contains("/embed") || 
            lower.contains("/player") ||
            lower.contains("/e/") ||
            lower.contains("/v/") ||
            lower.contains("/watch")
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
        
        if (lower.startsWith("blob:") || lower.startsWith("javascript:") || lower.startsWith("data:")) return true
        
        if (EMBED_SERVER_TOKENS.any { lower.contains(it) }) return false
        
        if (lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".webm") || 
            lower.contains(".mkv") || lower.contains(".mp3") || lower.contains(".aac")) return false
        
        if (lower.contains("/embed") || lower.contains("/player") || lower.contains("/video") ||
            lower.contains("/watch") || lower.contains("/stream") || lower.contains("/play")) return false
        
        return NOISE_TOKENS.any { lower.contains(it) } ||
            ASSET_EXTENSIONS.any { lower.substringBefore('?').endsWith(it) }
    }

    private fun hasPlayableExtension(url: String): Boolean {
        val lower = url.lowercase()
        return listOf(".m3u8", ".mp4", ".webm", ".mkv", ".mp3", ".aac", ".m4a", ".ogg").any { lower.contains(it) }
    }

    private fun detectQuality(url: String, label: String): String? {
        val value = "$url $label"
        return QUALITY_PATTERN.find(value)?.value?.lowercase()
    }

    companion object {
        private val BASE64_PATTERN = Regex("^[A-Za-z0-9+/=_-]+$")
        private val DIRECT_URL_PATTERN = Regex(
            "https?://[^\\s\\\"'<>]+\\.(?:m3u8|mp4|webm|mkv|mp3|aac|m4a|ogg)[^\\s\\\"'<>]*",
            RegexOption.IGNORE_CASE
        )
        private val QUALITY_PATTERN = Regex("(?:2160p|1440p|1080p|720p|480p|360p|4k|fhd|hd|sd)", RegexOption.IGNORE_CASE)
        
        private val EMBED_SERVER_TOKENS = listOf(
            "streamtape", "streamwish", "voe", "mixdrop", "doodstream", "ok.ru", "yourupload",
            "fembed", "streamsb", "vidmoly", "uqload", "sendvid", "mediafire", "mp4upload",
            "jwplayer", "dailymotion", "blogger", "archive.org", "filemoon", "streamlare",
            "vidlox", "upstream", "wolfstream", "vidcloud", "streamhub", "streamsss",
            "vidsrc", "2embed", "multiembed", "embedsu", "vidplay", "filelions",
            "vidsrc.to", "vidsrc.me", "vidsrc.cc", "embed.su", "multiup", "rapidvid",
            "vidbox", "streamvid", "streamplay", "playhydrax", "hydrax", "vidsrc.xyz"
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
            "placeholder",
            "favicon",
            "apple-touch-icon",
            "manifest.json",
            "browserconfig",
            "mstile",
            "sprite",
            "logo",
            "banner",
            "doubleclick",
            "ampproject",
            "gstatic",
            "linkrit",
            "bigbuckbunny",
            "test-videos",
            "sample-video",
            "wp-content/plugins",
            "wp-includes",
            "recaptcha",
            "hcaptcha",
            "adsbygoogle",
            "adservice",
            "tracking",
            "telemetry",
            "sentry.io",
            "hotjar",
            "mixpanel",
            "segment.io",
            "intercom",
            "crisp.chat",
            "tawk.to",
            "zendesk",
            "livechat",
            "chatbot"
        )
        private val ASSET_EXTENSIONS = listOf(
            ".ico", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp", ".avif",
            ".css", ".js", ".json", ".xml", ".woff", ".woff2", ".ttf", ".map",
            ".eot", ".otf", ".pdf", ".zip", ".rar", ".7z", ".tar", ".gz",
            ".exe", ".dmg", ".apk", ".deb", ".rpm", ".msi"
        )
    }
}
