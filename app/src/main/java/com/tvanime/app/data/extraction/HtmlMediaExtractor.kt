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
            addAll(extractScriptVariables(html, pageUrl))
            addAll(extractBroadAttributeCandidates(document, pageUrl))
            addAll(extractMetaRefresh(document, pageUrl))
            addAll(extractRawUrls(html, pageUrl))
        }.sortedBy { it.priority }.distinctBy { it.url }

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

    private fun extractScriptVariables(html: String, pageUrl: URI): List<DetectedMedia> {
        val candidates = mutableListOf<DetectedMedia>()
        
        SCRIPT_VAR_PATTERNS.forEach { pattern ->
            pattern.findAll(html).forEach { match ->
                val rawUrl = match.groupValues.getOrNull(1).orEmpty()
                val decoded = decodeObfuscatedUrl(rawUrl)
                buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "script-var")?.let {
                    candidates.add(it)
                }
            }
        }
        
        BASE64_PATTERNS.forEach { pattern ->
            pattern.findAll(html).forEach { match ->
                val encoded = match.groupValues.getOrNull(1).orEmpty()
                val decoded = decodeBase64Url(encoded)
                if (decoded != null) {
                    buildCandidate(url = decoded, pageUrl = pageUrl, sourceName = "base64")?.let {
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

    private fun buildCandidate(url: String, pageUrl: URI, sourceName: String = "generic"): DetectedMedia? {
        if (url.isBlank()) return null
        return candidateNormalizer.normalize(
            rawUrl = url,
            pageUrl = pageUrl,
            sourceName = "generic-html:$sourceName"
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
            Regex("""var\s+(?:video|videoUrl|video_url|streamUrl|stream_url|source|src|file|url)\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""(?:video|videoUrl|video_url|streamUrl|stream_url|source|src|file|url)\s*[:=]\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""['"](?:video|videoUrl|video_url|streamUrl|stream_url|source|src|file|url)['"]\s*:\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""player\.setup\(\{[^}]*file\s*:\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""jwplayer\([^)]*\)\.setup\(\{[^}]*file\s*:\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""sources\s*:\s*\[[^\]]*file\s*:\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
        )

        private val BASE64_PATTERNS = listOf(
            Regex("""atob\(['"]([A-Za-z0-9+/=_-]{12,})['"]\)""", RegexOption.IGNORE_CASE),
            Regex("""btoa\(['"]([^'"]+)['"]\)""", RegexOption.IGNORE_CASE)
        )

        private val RAW_URL_PATTERNS = listOf(
            Regex("""(https?://[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE),
            Regex("""(https?:\\/\\/[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        )
    }
}
