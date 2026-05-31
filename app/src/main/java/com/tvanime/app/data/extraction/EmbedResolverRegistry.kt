package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbedResolverRegistry @Inject constructor(
    private val httpPageFetcher: HttpPageFetcher,
    private val candidateNormalizer: CandidateNormalizer
) {

    suspend fun resolve(candidate: DetectedMedia): DetectedMedia {
        if (!candidate.requiresResolver) return candidate

        return runCatching {
            val embedUri = URI(candidate.url)
            val html = httpPageFetcher.fetch(embedUri)
            resolveFromHtml(candidate, html) ?: candidate.copy(
                diagnostics = candidate.diagnostics + "resolver=no-direct-candidate"
            )
        }.getOrElse { error ->
            candidate.copy(diagnostics = candidate.diagnostics + "resolver_error=${error.message.orEmpty()}")
        }
    }

    fun resolveFromHtml(candidate: DetectedMedia, html: String): DetectedMedia? {
        val pageUri = runCatching { URI(candidate.url) }.getOrNull() ?: return null

        for (pattern in DIRECT_PATTERNS) {
            val match = pattern.find(html) ?: continue
            val rawUrl = match.groupValues.getOrNull(1).orEmpty()
            val normalized = candidateNormalizer.normalize(
                rawUrl = rawUrl,
                pageUrl = pageUri,
                sourceName = "${candidate.sourceName}:resolver",
                diagnostics = candidate.diagnostics + "resolved_from=${candidate.server}"
            )
            if (normalized != null && !normalized.requiresResolver) {
                return normalized.copy(
                    label = candidate.label.ifBlank { normalized.label },
                    referer = candidate.referer,
                    headers = candidate.headers + mapOf("Referer" to candidate.url)
                )
            }
        }

        return null
    }

    companion object {
        private val DIRECT_PATTERNS = listOf(
            Regex("""(?:file|src|source|video|url|embed|iframe|stream|hls|mp4)\s*[:=]\s*["'`]([^"'`]+)["'`]""", RegexOption.IGNORE_CASE),
            Regex("""["'](?:file|src|source|video|url|embed|iframe|stream|hls|mp4)["']\s*:\s*["'`]([^"'`]+)["'`]""", RegexOption.IGNORE_CASE),
            Regex("""sources?\s*:\s*\[[\s\S]*?(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""player\.setup\([\s\S]*?(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""jwplayer\([^)]*\)\.setup\([\s\S]*?(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""data-(?:src|file|video|source)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(?:eval|unescape|decodeURIComponent)\(["']([^"']+)["']\)""", RegexOption.IGNORE_CASE),
            Regex("""atob\(["']([A-Za-z0-9+/=_-]{12,})["']\)""", RegexOption.IGNORE_CASE),
            Regex("""([A-Za-z0-9+/=_-]{24,})"""),
            Regex("""(https?:\\/\\/[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE),
            Regex("""(https?://[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        )
    }
}
