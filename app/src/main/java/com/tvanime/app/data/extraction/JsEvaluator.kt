package com.tvanime.app.data.extraction

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsEvaluator @Inject constructor() {

    fun evaluateJsVariables(html: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        results.addAll(extractVarAssignments(html, pageUrl))
        results.addAll(extractJsonObjects(html, pageUrl))
        results.addAll(extractSvelteData(html, pageUrl))
        results.addAll(extractPlayerSetup(html, pageUrl))
        results.addAll(extractSourcesArray(html, pageUrl))
        results.addAll(extractScriptUrls(html, pageUrl))
        
        return results.distinctBy { it.url }
    }

    private fun extractVarAssignments(html: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        for (pattern in VAR_PATTERNS) {
            pattern.findAll(html).forEach { match ->
                val value = match.groupValues.getOrNull(1).orEmpty().trim()
                val urls = extractUrlsFromValue(value, pageUrl)
                results.addAll(urls)
            }
        }
        
        return results
    }

    private fun extractJsonObjects(html: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        for (pattern in JSON_PATTERNS) {
            pattern.findAll(html).forEach { match ->
                val jsonStr = match.groupValues.getOrNull(1).orEmpty()
                val urls = extractUrlsFromJson(jsonStr, pageUrl)
                results.addAll(urls)
            }
        }
        
        return results
    }

    private fun extractSvelteData(html: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        for (pattern in SVENGER_PATTERNS) {
            pattern.findAll(html).forEach { match ->
                val data = match.groupValues.getOrNull(1).orEmpty()
                val urls = DIRECT_URL_PATTERN.findAll(data).mapNotNull { urlMatch ->
                    val url = urlMatch.value
                    if (isMediaUrl(url)) ExtractedMedia(url, "svelte-data", "svelte") else null
                }.toList()
                results.addAll(urls)
            }
        }
        
        return results
    }

    private fun extractPlayerSetup(html: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        for (pattern in PLAYER_PATTERNS) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues.getOrNull(1).orEmpty().trim()
                if (url.isNotBlank() && isMediaUrl(url)) {
                    results.add(ExtractedMedia(cleanUrl(url), "player-setup", "player"))
                }
            }
        }
        
        return results
    }

    private fun extractSourcesArray(html: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        SOURCES_ARRAY_PATTERN.findAll(html).forEach { match ->
            val sourcesBlock = match.groupValues.getOrNull(1).orEmpty()
            val urls = DIRECT_URL_PATTERN.findAll(sourcesBlock).mapNotNull { urlMatch ->
                val url = urlMatch.value
                if (isMediaUrl(url)) ExtractedMedia(cleanUrl(url), "sources-array", "player") else null
            }.toList()
            results.addAll(urls)
            
            FILE_COLON_PATTERN.findAll(sourcesBlock).forEach { fileMatch ->
                val url = fileMatch.groupValues.getOrNull(1).orEmpty().trim()
                if (url.isNotBlank() && (isMediaUrl(url) || isEmbedUrl(url))) {
                    results.add(ExtractedMedia(cleanUrl(url), "sources-array-file", "player"))
                }
            }
        }
        
        return results
    }

    private fun extractScriptUrls(html: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        SCRIPT_SRC_PATTERN.findAll(html).forEach { match ->
            val src = match.groupValues.getOrNull(1).orEmpty()
            if (src.isNotBlank() && (src.contains("player") || src.contains("video") || 
                src.contains("embed") || src.contains("stream") || src.contains("source"))) {
                results.add(ExtractedMedia(cleanUrl(src), "script-src", "script"))
            }
        }
        
        return results
    }

    private fun extractUrlsFromValue(value: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        val decoded = decodeValue(value)
        
        DIRECT_URL_PATTERN.findAll(decoded).forEach { match ->
            val url = match.value
            if (isMediaUrl(url)) {
                results.add(ExtractedMedia(cleanUrl(url), "var-url", "var"))
            }
        }
        
        BASE64_LONG_PATTERN.findAll(value).forEach { match ->
            val encoded = match.groupValues.getOrNull(1).orEmpty()
            val decoded = decodeBase64(encoded)
            if (decoded != null && isMediaUrl(decoded)) {
                results.add(ExtractedMedia(cleanUrl(decoded), "var-base64", "var"))
            }
        }
        
        return results
    }

    private fun extractUrlsFromJson(jsonStr: String, pageUrl: String): List<ExtractedMedia> {
        val results = mutableListOf<ExtractedMedia>()
        
        val urlMatches = DIRECT_URL_PATTERN.findAll(jsonStr).mapNotNull { match ->
            val url = match.value
            if (isMediaUrl(url)) ExtractedMedia(cleanUrl(url), "json-url", "json") else null
        }.toList()
        results.addAll(urlMatches)
        
        BASE64_LONG_PATTERN.findAll(jsonStr).forEach { match ->
            val encoded = match.groupValues.getOrNull(1).orEmpty()
            val decoded = decodeBase64(encoded)
            if (decoded != null && isMediaUrl(decoded)) {
                results.add(ExtractedMedia(cleanUrl(decoded), "json-base64", "json"))
            }
        }
        
        JSON_FIELD_PATTERNS.forEach { pattern ->
            pattern.findAll(jsonStr).forEach { match ->
                val url = match.groupValues.getOrNull(1).orEmpty().trim()
                if (url.isNotBlank() && isMediaUrl(url)) {
                    results.add(ExtractedMedia(cleanUrl(url), "json-field", "json"))
                }
            }
        }
        
        return results
    }

    private fun decodeValue(value: String): String {
        var decoded = value
        decoded = decoded.replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("\\u003f", "?")
            .replace("\\u003a", ":")
            .replace("\\u002f", "/")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .replace("\\x26", "&")
            .replace("\\x3d", "=")
            .replace("%3A", ":").replace("%3a", ":")
            .replace("%2F", "/").replace("%2f", "/")
            .replace("%3F", "?").replace("%3f", "?")
            .replace("%3D", "=").replace("%3d", "=")
            .replace("%26", "&")
        
        if (decoded.contains("\\u00")) {
            decoded = UNICODE_ESCAPE_REGEX.replace(decoded) { matchResult ->
                val code = matchResult.groupValues[1].toInt(16)
                code.toChar().toString()
            }
        }
        
        return decoded
    }

    private fun decodeBase64(encoded: String): String? {
        if (encoded.length < 12) return null
        return runCatching {
            val cleaned = encoded.replace('-', '+').replace('_', '/')
            val padded = cleaned.padEnd(cleaned.length + (4 - cleaned.length % 4) % 4, '=')
            val decoded = String(java.util.Base64.getDecoder().decode(padded), Charsets.UTF_8)
            decoded.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        }.getOrNull()
    }

    private fun isMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".webm") ||
            lower.contains(".mkv") || lower.contains(".ts") || lower.contains(".mov") ||
            lower.contains("/video") || lower.contains("/stream") || lower.contains("/play") ||
            lower.contains("get_video") || lower.contains("hls")
    }

    private fun isEmbedUrl(url: String): Boolean {
        val lower = url.lowercase()
        return EMBED_TOKENS.any { lower.contains(it) } ||
            lower.contains("/embed") || lower.contains("/player") ||
            lower.contains("/e/") || lower.contains("/v/") || lower.contains("/watch")
    }

    private fun cleanUrl(url: String): String {
        var cleaned = url.trim()
            .removeSurrounding("\"").removeSurrounding("'").removeSurrounding("`")
        if (cleaned.startsWith("//")) cleaned = "https:$cleaned"
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "https://$cleaned"
        }
        return cleaned
    }

    data class ExtractedMedia(
        val url: String,
        val extractionMethod: String,
        val sourceType: String
    )

    companion object {
        private val EMBED_TOKENS = listOf(
            "streamtape", "streamwish", "voe", "mixdrop", "doodstream", "ok.ru",
            "yourupload", "fembed", "streamsb", "vidmoly", "uqload", "sendvid",
            "mediafire", "mp4upload", "filemoon", "streamlare", "vidlox", "upstream",
            "wolfstream", "vidcloud", "vidsrc", "2embed", "embedsu", "vidplay",
            "filelions", "playhydrax", "hydrax"
        )
        
        private val DIRECT_URL_PATTERN = Regex(
            """https?://[^\s"'<>\\]+\.(?:m3u8|mp4|webm|mkv|ts|mov|avi|mp3|aac|m4a|ogg)[^\s"'<>\\]*""",
            RegexOption.IGNORE_CASE
        )
        
        private val VAR_PATTERNS = listOf(
            Regex("""var\s+(?:videos?|videoUrl|video_url|streamUrl|stream_url|sources?|src|file|url|stream|playbackUrl|hlsUrl|dashUrl|manifestUrl|playlistUrl)\s*=\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE),
            Regex("""(?:video|videoUrl|video_url|streamUrl|stream_url|source|src|file|url|stream|playbackUrl|hlsUrl|manifestUrl)\s*[:=]\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE),
            Regex("""['"`](?:video|videoUrl|video_url|streamUrl|stream_url|source|src|file|url|stream|playbackUrl|hlsUrl|manifestUrl)['"`]\s*:\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE),
            Regex("""var\s+(?:episodes?|servers?|options?|data)\s*=\s*(\[[\s\S]{5,2000}?\])""", RegexOption.IGNORE_CASE),
            Regex("""var\s+(?:videoData|playerData|config|setup|playerConfig)\s*=\s*(\{[\s\S]{5,3000}?\})""", RegexOption.IGNORE_CASE),
            Regex("""window\.\w+[Vv]ideo\s*=\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE),
            Regex("""window\.\w+[Ss]tream\s*=\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE),
            Regex("""window\.\w+[Uu]rl\s*=\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE)
        )
        
        private val JSON_PATTERNS = listOf(
            Regex("""["'](?:file|src|source|video|url|stream|hls|manifest)['"]\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""["'](?:file|src|source|video|url|stream|hls|manifest)['"]\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex(""""(?:file|src|source|video|url|stream|hls|manifest)"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        )
        
        private val JSON_FIELD_PATTERNS = listOf(
            Regex("""[{,]\s*["']?(?:file|src|source|video|url|stream|hls|manifest|code|embed|remote|link|href|download)["']?\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )
        
        private val SVENGER_PATTERNS = listOf(
            Regex("""__sveltekit_\w+\s*=\s*\{[^}]*data\s*:\s*(\[[\s\S]{5,5000}?\])""", RegexOption.IGNORE_CASE),
            Regex("""data\s*=\s*(\{["'][^}]*["']:\s*["'][^}]*["'][\s\S]{5,5000}?\})""", RegexOption.IGNORE_CASE)
        )
        
        private val PLAYER_PATTERNS = listOf(
            Regex("""player\s*\.\s*setup\s*\(\s*\{[\s\S]*?file\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""jwplayer\s*\([^)]*\)\s*\.setup\s*\(\s*\{[\s\S]*?file\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""videojs\s*\([^)]*\)\s*.\s*src\s*\(\s*["']([^"']+)["']\s*\)""", RegexOption.IGNORE_CASE),
            Regex("""(?:new\s+)?Player\s*\([^)]*\)\s*.\s*src\s*\(\s*["']([^"']+)["']\s*\)""", RegexOption.IGNORE_CASE),
            Regex("""<source\s+[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )
        
        private val SOURCES_ARRAY_PATTERN = Regex(
            """sources?\s*:\s*\[([\s\S]{5,5000}?)\]""", RegexOption.IGNORE_CASE
        )
        
        private val FILE_COLON_PATTERN = Regex(
            """(?:file|src|source|url|stream)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE
        )
        
        private val SCRIPT_SRC_PATTERN = Regex(
            """<script[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE
        )
        
        private val BASE64_LONG_PATTERN = Regex(
            """[A-Za-z0-9+/=_-]{12,}""", RegexOption.IGNORE_CASE
        )
        
        private val UNICODE_ESCAPE_REGEX = Regex("""\\u([0-9a-fA-F]{4})""")
    }
}