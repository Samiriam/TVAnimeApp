package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import com.tvanime.app.domain.model.ExtractionResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlMediaExtractor @Inject constructor(
    private val candidateNormalizer: CandidateNormalizer
) {

    fun extract(pageUrl: URI, html: String): ExtractionResult {
        val document = Jsoup.parse(html, pageUrl.toString())
        val title = document.title().ifBlank { pageUrl.host }

        val candidates = buildList {
            addAll(extractVideoAndAudio(document, pageUrl))
            addAll(extractIframes(document, pageUrl))
            addAll(extractOnClickHandlers(document, pageUrl))
            addAll(extractDataAttributes(document, pageUrl))
            addAll(extractAnchorLinks(document, pageUrl))
            addAll(extractScriptContent(html, pageUrl))
            addAll(extractBroadAttributeCandidates(document, pageUrl))
            addAll(extractMetaRefresh(document, pageUrl))
            addAll(extractRawUrls(html, pageUrl))
            addAll(extractBase64Urls(html, pageUrl))
            addAll(extractJsonLd(document, pageUrl))
            addAll(extractOgVideo(document, pageUrl))
        }.distinctBy { it.url }.sortedBy { it.priority }

        require(candidates.isNotEmpty()) { "No se detectaron enlaces multimedia compatibles en la pagina." }

        return ExtractionResult(
            pageUrl = pageUrl.toString(),
            sourceHost = pageUrl.host.orEmpty(),
            title = title,
            candidates = candidates
        )
    }

    private fun extractVideoAndAudio(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("video[src], video source[src], audio[src], audio source[src]")
            .mapNotNull { element ->
                val url = element.absUrl("src").ifBlank { element.attr("src") }
                buildCandidate(url = url, pageUrl = pageUrl, sourceName = "video-audio-tag")
            }
    }

    private fun extractIframes(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("iframe[src], iframe[data-src], iframe[data-lazy-src]")
            .mapNotNull { element ->
                val url = element.absUrl("src").ifBlank {
                    element.attr("data-src").ifBlank { element.attr("data-lazy-src") }
                }
                if (url.isBlank()) null else candidateNormalizer.normalize(
                    rawUrl = url,
                    pageUrl = pageUrl,
                    sourceName = "iframe",
                    explicitFormat = "embed",
                    label = "Embed"
                )
            }
    }

    private fun extractOnClickHandlers(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("[onclick]")
            .flatMap { element ->
                val onclick = element.attr("onclick")
                ONCLICK_PATTERNS.mapNotNull { pattern ->
                    pattern.find(onclick)?.groupValues?.getOrNull(1)
                }
            }
            .mapNotNull { rawUrl ->
                buildCandidate(url = rawUrl, pageUrl = pageUrl, sourceName = "onclick")
            }
    }

    private fun extractDataAttributes(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("[data-player], [data-video], [data-src], [data-file], [data-url], [data-embed], [data-iframe]")
            .flatMap { element ->
                listOf(
                    element.attr("data-player"),
                    element.attr("data-video"),
                    element.attr("data-src"),
                    element.attr("data-file"),
                    element.attr("data-url"),
                    element.attr("data-embed"),
                    element.attr("data-iframe")
                ).filter { it.isNotBlank() }
            }
            .mapNotNull { rawUrl ->
                buildCandidate(url = rawUrl, pageUrl = pageUrl, sourceName = "data-attr")
            }
    }

    private fun extractAnchorLinks(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("a[href]")
            .mapNotNull { element ->
                val url = element.absUrl("href").ifBlank { element.attr("href") }
                buildCandidate(url = url, pageUrl = pageUrl, sourceName = "anchor")
            }
    }

    private fun extractScriptContent(html: String, pageUrl: URI): List<DetectedMedia> {
        val candidates = mutableListOf<DetectedMedia>()

        documentScripts(html).forEach { scriptContent ->
            SCRIPT_VAR_PATTERNS.forEach { pattern ->
                pattern.findAll(scriptContent).forEach { match ->
                    val rawUrl = match.groupValues.getOrNull(1).orEmpty()
                    val decoded = decodeObfuscatedUrl(rawUrl)
                    buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "script-var")?.let {
                        candidates.add(it)
                    }
                }
            }

            SOURCES_OBJ_PATTERNS.forEach { pattern ->
                pattern.findAll(scriptContent).forEach { match ->
                    val block = match.groupValues.getOrNull(1).orEmpty()
                    SOURCES_INNER_PATTERNS.forEach { innerPattern ->
                        innerPattern.findAll(block).forEach { innerMatch ->
                            val url = innerMatch.groupValues.getOrNull(1).orEmpty()
                            val decoded = decodeObfuscatedUrl(url)
                            buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "sources-obj")?.let {
                                candidates.add(it)
                            }
                        }
                    }
                }
            }

            BASE64_PATTERNS.forEach { pattern ->
                pattern.findAll(scriptContent).forEach { match ->
                    val encoded = match.groupValues.getOrNull(1).orEmpty()
                    val decoded = decodeBase64Url(encoded)
                    if (decoded != null) {
                        buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "base64")?.let {
                            candidates.add(it)
                        }
                    }
                }
            }

            DECODE_URI_PATTERNS.forEach { pattern ->
                pattern.findAll(scriptContent).forEach { match ->
                    val rawUrl = match.groupValues.getOrNull(1).orEmpty()
                    val decoded = decodeObfuscatedUrl(rawUrl)
                    buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "decode-uri")?.let {
                        candidates.add(it)
                    }
                }
            }

            WINDOW_LOCATION_PATTERNS.forEach { pattern ->
                pattern.findAll(scriptContent).forEach { match ->
                    val rawUrl = match.groupValues.getOrNull(1).orEmpty()
                    val decoded = decodeObfuscatedUrl(rawUrl)
                    buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "window-location")?.let {
                        candidates.add(it)
                    }
                }
            }
        }

        return candidates
    }

    private fun extractBroadAttributeCandidates(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.allElements
            .flatMap { element ->
                BROAD_ATTRIBUTES.mapNotNull { attr ->
                    element.attr(attr).takeIf { it.isNotBlank() }
                }
            }
            .mapNotNull { rawUrl ->
                buildCandidate(url = rawUrl, pageUrl = pageUrl, sourceName = "broad-attr")
            }
    }

    private fun extractMetaRefresh(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("meta[http-equiv=refresh], meta[content]")
            .mapNotNull { element -> element.attr("content").takeIf { it.isNotBlank() } }
            .mapNotNull { content -> META_REFRESH_PATTERN.find(content)?.groupValues?.getOrNull(1) }
            .mapNotNull { rawUrl ->
                buildCandidate(url = rawUrl, pageUrl = pageUrl, sourceName = "meta-refresh")
            }
    }

    private fun extractRawUrls(html: String, pageUrl: URI): List<DetectedMedia> {
        return RAW_URL_PATTERNS.flatMap { pattern ->
            pattern.findAll(html).mapNotNull { match ->
                val rawUrl = match.groupValues.getOrNull(1).orEmpty()
                buildCandidate(url = rawUrl, pageUrl = pageUrl, sourceName = "raw-url")
            }.toList()
        }
    }

    private fun extractBase64Urls(html: String, pageUrl: URI): List<DetectedMedia> {
        val candidates = mutableListOf<DetectedMedia>()
        
        STANDALONE_BASE64_PATTERN.findAll(html).forEach { match ->
            val encoded = match.groupValues.getOrNull(1).orEmpty()
            val decoded = decodeBase64Url(encoded)
            if (decoded != null) {
                buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "base64-standalone")?.let {
                    candidates.add(it)
                }
            }
        }

        DATA_SRC_BASE64_PATTERN.findAll(html).forEach { match ->
            val b64data = match.groupValues.getOrNull(2).orEmpty()
            val decoded = decodeBase64Url(b64data)
            if (decoded != null) {
                buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "data-src-base64")?.let {
                    candidates.add(it)
                }
            }
        }

        return candidates
    }

    private fun extractJsonLd(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("script[type=application/ld+json]")
            .mapNotNull { element ->
                val jsonText = element.data()
                JSON_LD_URL_PATTERNS.flatMap { pattern ->
                    pattern.findAll(jsonText).mapNotNull { match ->
                        val url = match.groupValues.getOrNull(1).orEmpty()
                        if (url.isNotBlank()) buildCandidate(url = url, pageUrl = pageUrl, sourceName = "json-ld") else null
                    }
                }
            }.flatten()
    }

    private fun extractOgVideo(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("meta[property^=og:video], meta[property^=og:url]")
            .mapNotNull { element ->
                val url = element.attr("content")
                if (url.isNotBlank()) buildCandidate(url = url, pageUrl = pageUrl, sourceName = "og-meta") else null
            }
    }

    private fun documentScripts(html: String): List<String> {
        val doc = Jsoup.parse(html)
        return doc.select("script").map { it.data() }.filter { it.isNotBlank() }
    }

    private fun buildCandidate(url: String, pageUrl: URI, sourceName: String = "generic"): DetectedMedia? {
        if (url.isBlank()) return null
        return candidateNormalizer.normalize(
            rawUrl = url,
            pageUrl = pageUrl,
            sourceName = "html:$sourceName"
        )
    }

    private fun decodeObfuscatedUrl(url: String): String {
        var decoded = url
        decoded = decoded.replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("\\u003f", "?")
            .replace("\\/", "/")
            .replace("&amp;", "&")

        if (decoded.contains("%")) {
            decoded = runCatching {
                java.net.URLDecoder.decode(decoded, "UTF-8")
            }.getOrDefault(decoded)
        }

        return decoded
    }

    private fun decodeBase64Url(encoded: String): String? {
        if (encoded.length < 12) return null

        return runCatching {
            val cleaned = encoded.replace('-', '+').replace('_', '/')
            val padded = cleaned.padEnd(cleaned.length + (4 - cleaned.length % 4) % 4, '=')
            val decoded = String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
            if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                decoded
            } else null
        }.getOrNull()
    }

    companion object {
        private val BROAD_ATTRIBUTES = listOf(
            "src", "href", "data-src", "data-file", "data-video", "data-source", "data-url",
            "data-href", "data-embed", "data-iframe", "data-player", "data-link", "data-stream",
            "data-config", "data-json", "file", "url", "source", "video", "poster"
        )

        private val META_REFRESH_PATTERN = Regex("""url\s*=\s*([^;]+)""", RegexOption.IGNORE_CASE)

        private val ONCLICK_PATTERNS = listOf(
            Regex("""window\.open\(['"]([^'"]+)['"]\)""", RegexOption.IGNORE_CASE),
            Regex("""window\.location(?:\.href)?\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""location\.href\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""open\(['"]([^'"]+)['"]\)""", RegexOption.IGNORE_CASE)
        )

        private val SCRIPT_VAR_PATTERNS = listOf(
            Regex("""var\s+(?:video|videoUrl|video_url|streamUrl|stream_url|source|src|file|url|stream)\s*=\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE),
            Regex("""(?:video|videoUrl|video_url|streamUrl|stream_url|source|src|file|url|stream)\s*[:=]\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE),
            Regex("""['"`](?:video|videoUrl|video_url|streamUrl|stream_url|source|src|file|url|stream)['"`]\s*:\s*['"`]([^'"`]+)['"`]""", RegexOption.IGNORE_CASE),
            Regex("""player\.setup\(\{[^}]*file\s*:\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""jwplayer\([^)]*\)\.setup\(\{[^}]*file\s*:\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
        )

        private val SOURCES_OBJ_PATTERNS = listOf(
            Regex("""sources?\s*:\s*\[([\s\S]{5,3000}?)\]""", RegexOption.IGNORE_CASE),
            Regex("""(?:file|src|source|video|url)\s*:\s*\[([\s\S]{5,3000}?)\]""", RegexOption.IGNORE_CASE)
        )

        private val SOURCES_INNER_PATTERNS = listOf(
            Regex("""(?:file|src|source|url)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(?:file|src|source|url)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )

        private val WINDOW_LOCATION_PATTERNS = listOf(
            Regex("""window\.location(?:\.href)?\s*=\s*["'`](?:https?:)?//([^"'`]+)["'`]""", RegexOption.IGNORE_CASE),
            Regex("""window\.location(?:\.href)?\s*=\s*["'`](https?://[^"'`]+)["'`]""", RegexOption.IGNORE_CASE)
        )

        private val DECODE_URI_PATTERNS = listOf(
            Regex("""(?:eval|unescape|decodeURIComponent|atob)\s*\(\s*["']([^"']+)["']\s*\)""", RegexOption.IGNORE_CASE)
        )

        private val BASE64_PATTERNS = listOf(
            Regex("""atob\(['"]([A-Za-z0-9+/=_-]{12,})['"]\)""", RegexOption.IGNORE_CASE),
            Regex("""atob\(['"]([A-Za-z0-9+/=_-]{12,})['"]\)""", RegexOption.IGNORE_CASE)
        )

        private val STANDALONE_BASE64_PATTERN = Regex(
            """[A-Za-z0-9+/]{40,}={0,2}""", RegexOption.IGNORE_CASE
        )

        private val DATA_SRC_BASE64_PATTERN = Regex(
            """data-src\s*=\s*["']base64,([A-Za-z0-9+/=_-]+)["']""", RegexOption.IGNORE_CASE
        )

        private val JSON_LD_URL_PATTERNS = listOf(
            Regex("""(?:contentUrl|embedUrl|url)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""["'](?:contentUrl|embedUrl|url)["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )

        private val RAW_URL_PATTERNS = listOf(
            Regex("""(https?://[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|ts|mov|avi|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE),
            Regex("""(https?:\\/\\/[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|ts|mov|avi|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        )
    }
}