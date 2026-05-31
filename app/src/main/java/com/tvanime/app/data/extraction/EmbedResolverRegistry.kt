package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbedResolverRegistry @Inject constructor(
    private val httpPageFetcher: HttpPageFetcher,
    private val candidateNormalizer: CandidateNormalizer,
    private val serverSpecificResolvers: ServerSpecificResolvers
) {

    suspend fun resolve(candidate: DetectedMedia): DetectedMedia {
        if (!candidate.requiresResolver) return candidate

        return runCatching {
            val embedUri = URI(candidate.url)
            val html = httpPageFetcher.fetch(embedUri)
            resolveFromHtml(candidate, html, embedUri) ?: candidate.copy(
                diagnostics = candidate.diagnostics + "resolver=no-direct-candidate"
            )
        }.getOrElse { error ->
            candidate.copy(diagnostics = candidate.diagnostics + "resolver_error=${error.message.orEmpty()}")
        }
    }

    suspend fun resolveAll(candidates: List<DetectedMedia>): List<DetectedMedia> = coroutineScope {
        candidates.map { candidate ->
            async { resolve(candidate) }
        }.map { it.await() }
    }

    fun resolveFromHtml(candidate: DetectedMedia, html: String, embedUri: URI? = null): DetectedMedia? {
        val pageUri = embedUri ?: runCatching { URI(candidate.url) }.getOrNull() ?: return null

        val serverResolved = serverSpecificResolvers.resolveForServer(candidate.server, html, candidate.url)
        if (serverResolved != null) {
            val normalized = candidateNormalizer.normalize(
                rawUrl = serverResolved,
                pageUrl = pageUri,
                sourceName = "${candidate.sourceName}:resolver:${candidate.server}",
                diagnostics = candidate.diagnostics + "resolved_specific=${candidate.server}"
            )
            if (normalized != null && !normalized.requiresResolver) {
                return normalized.copy(
                    label = candidate.label.ifBlank { normalized.label },
                    referer = candidate.referer,
                    headers = candidate.headers + mapOf("Referer" to candidate.url)
                )
            }
        }

        val redirectMatch = WINDOW_LOCATION_PATTERN.find(html)
        if (redirectMatch != null) {
            val redirectUrl = redirectMatch.groupValues.getOrNull(1).orEmpty()
            if (redirectUrl.isNotBlank()) {
                val normalized = candidateNormalizer.normalize(
                    rawUrl = redirectUrl,
                    pageUrl = pageUri,
                    sourceName = "${candidate.sourceName}:resolver:redirect",
                    diagnostics = candidate.diagnostics + "resolved_redirect=${candidate.server}"
                )
                if (normalized != null && !normalized.requiresResolver) {
                    return normalized.copy(
                        label = candidate.label.ifBlank { normalized.label },
                        referer = candidate.referer,
                        headers = candidate.headers + mapOf("Referer" to candidate.url)
                    )
                }
            }
        }

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
        private val WINDOW_LOCATION_PATTERN = Regex(
            """window\.location(?:\.href)?\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE
        )

        private val DIRECT_PATTERNS = listOf(
            Regex("""(?:file|src|source|video|url|embed|iframe|stream|hls|mp4)\s*[:=]\s*["'`]([^"'`]+)["'`]""", RegexOption.IGNORE_CASE),
            Regex("""["'](?:file|src|source|video|url|embed|iframe|stream|hls|mp4)["']\s*:\s*["'`]([^"'`]+)["'`]""", RegexOption.IGNORE_CASE),
            Regex("""sources?\s*:\s*\[[\s\S]*?(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""player\.setup\([\s\S]*?(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""jwplayer\([^)]*\)\.setup\([\s\S]*?(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""data-(?:src|file|video|source)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(?:eval|unescape|decodeURIComponent)\(["']([^"']+)["']\)""", RegexOption.IGNORE_CASE),
            Regex("""atob\(["']([A-Za-z0-9+/=_-]{12,})["']\)""", RegexOption.IGNORE_CASE),
            Regex("""(https?:\\/\\/[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE),
            Regex("""(https?://[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        )
    }
}
