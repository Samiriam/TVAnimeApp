package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbedResolverRegistry @Inject constructor(
    private val httpPageFetcher: HttpPageFetcher,
    private val candidateNormalizer: CandidateNormalizer,
    private val serverSpecificResolvers: ServerSpecificResolvers,
    private val jsEvaluator: JsEvaluator,
    private val packerUnpacker: PackerUnpacker
) {

    suspend fun resolve(candidate: DetectedMedia): List<DetectedMedia> {
        if (!candidate.requiresResolver) return listOf(candidate)

        val results = mutableListOf<DetectedMedia>()

        val html = runCatching {
            val referer = candidate.referer.ifBlank { candidate.url }
            httpPageFetcher.fetchWithReferer(URI(candidate.url), referer)
        }.getOrNull()

        if (html != null && html.length > 200) {
            val serverResolved = serverSpecificResolvers.resolveForServer(candidate.server, html, candidate.url)
            if (serverResolved != null) {
                val normalized = candidateNormalizer.normalize(
                    rawUrl = serverResolved,
                    pageUrl = URI(candidate.url),
                    sourceName = "${candidate.sourceName}:resolver:${candidate.server}",
                    diagnostics = candidate.diagnostics + "resolved_specific=${candidate.server}"
                )
                if (normalized != null && !normalized.requiresResolver) {
                    results.add(normalized.copy(
                        label = candidate.label.ifBlank { normalized.label },
                        referer = candidate.referer,
                        headers = candidate.headers + mapOf("Referer" to candidate.url)
                    ))
                }
            }

            val scriptResults = jsEvaluator.evaluateJsVariables(html, candidate.url)
            for (media in scriptResults) {
                val normalized = candidateNormalizer.normalize(
                    rawUrl = media.url,
                    pageUrl = URI(candidate.url),
                    sourceName = "${candidate.sourceName}:js-eval:${media.extractionMethod}",
                    diagnostics = candidate.diagnostics + "js-eval=${media.sourceType}"
                )
                if (normalized != null && !normalized.requiresResolver) {
                    results.add(normalized.copy(
                        headers = candidate.headers + mapOf("Referer" to candidate.url)
                    ))
                }
            }

            val unpackedResults = packerUnpacker.tryUnpackAll(html)
            for (unpacked in unpackedResults) {
                val unpackedMedia = jsEvaluator.evaluateJsVariables(unpacked, candidate.url)
                for (media in unpackedMedia) {
                    val normalized = candidateNormalizer.normalize(
                        rawUrl = media.url,
                        pageUrl = URI(candidate.url),
                        sourceName = "${candidate.sourceName}:unpack:${media.extractionMethod}",
                        diagnostics = candidate.diagnostics + "unpacked=${media.sourceType}"
                    )
                    if (normalized != null && !normalized.requiresResolver) {
                        results.add(normalized.copy(
                            headers = candidate.headers + mapOf("Referer" to candidate.url)
                        ))
                    }
                }
            }

            for (pattern in DIRECT_PATTERNS) {
                val match = pattern.find(html) ?: continue
                val rawUrl = match.groupValues.getOrNull(1).orEmpty()
                if (rawUrl.isBlank()) continue
                val normalized = candidateNormalizer.normalize(
                    rawUrl = rawUrl,
                    pageUrl = URI(candidate.url),
                    sourceName = "${candidate.sourceName}:resolver:direct",
                    diagnostics = candidate.diagnostics + "resolved_direct=${candidate.server}"
                )
                if (normalized != null && !normalized.requiresResolver) {
                    results.add(normalized.copy(
                        label = candidate.label.ifBlank { normalized.label },
                        referer = candidate.referer,
                        headers = candidate.headers + mapOf("Referer" to candidate.url)
                    ))
                }
            }

            val redirectMatch = WINDOW_LOCATION_PATTERN.find(html)
            if (redirectMatch != null) {
                val redirectUrl = redirectMatch.groupValues.getOrNull(1).orEmpty()
                if (redirectUrl.isNotBlank()) {
                    val normalized = candidateNormalizer.normalize(
                        rawUrl = redirectUrl,
                        pageUrl = URI(candidate.url),
                        sourceName = "${candidate.sourceName}:resolver:redirect",
                        diagnostics = candidate.diagnostics + "resolved_redirect=${candidate.server}"
                    )
                    if (normalized != null) {
                        if (!normalized.requiresResolver) {
                            results.add(normalized.copy(
                                label = candidate.label.ifBlank { normalized.label },
                                referer = candidate.referer,
                                headers = candidate.headers + mapOf("Referer" to candidate.url)
                            ))
                        } else if (normalized.url != candidate.url) {
                            val subResolved = resolveRecursive(normalized, depth = 1)
                            results.addAll(subResolved)
                        }
                    }
                }
            }

            val iframeSrcPatterns = listOf(
                Regex("""<iframe[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<iframe[^>]+data-src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            )
            for (pattern in iframeSrcPatterns) {
                pattern.findAll(html).forEach { iframeMatch ->
                    val iframeUrl = iframeMatch.groupValues.getOrNull(1).orEmpty()
                    if (iframeUrl.isNotBlank() && iframeUrl != candidate.url) {
                        val iframeNormalized = candidateNormalizer.normalize(
                            rawUrl = iframeUrl,
                            pageUrl = URI(candidate.url),
                            sourceName = "${candidate.sourceName}:nested-iframe",
                            explicitFormat = "embed",
                            diagnostics = candidate.diagnostics + "nested_iframe"
                        )
                        if (iframeNormalized != null && iframeNormalized.requiresResolver) {
                            val subResults = resolveRecursive(iframeNormalized, depth = 1)
                            results.addAll(subResults)
                        }
                    }
                }
            }
        }

        if (results.isEmpty()) {
            results.add(candidate.copy(diagnostics = candidate.diagnostics + "resolver=no-direct-candidate"))
        }

        return results.distinctBy { it.url }
    }

    private suspend fun resolveRecursive(candidate: DetectedMedia, depth: Int): List<DetectedMedia> {
        if (depth >= MAX_RESOLVE_DEPTH) return emptyList()

        val html = runCatching {
            val referer = candidate.referer.ifBlank { candidate.url }
            httpPageFetcher.fetchWithReferer(URI(candidate.url), referer)
        }.getOrNull() ?: return emptyList()

        val results = mutableListOf<DetectedMedia>()

        val serverResolved = serverSpecificResolvers.resolveForServer(candidate.server, html, candidate.url)
        if (serverResolved != null) {
            val normalized = candidateNormalizer.normalize(
                rawUrl = serverResolved,
                pageUrl = URI(candidate.url),
                sourceName = "${candidate.sourceName}:nested-resolver:${candidate.server}",
                diagnostics = candidate.diagnostics + "nested_resolved_specific=${candidate.server}"
            )
            if (normalized != null && !normalized.requiresResolver) {
                results.add(normalized.copy(headers = candidate.headers + mapOf("Referer" to candidate.url)))
            }
        }

        for (pattern in DIRECT_PATTERNS) {
            val match = pattern.find(html) ?: continue
            val rawUrl = match.groupValues.getOrNull(1).orEmpty()
            if (rawUrl.isBlank()) continue
            val normalized = candidateNormalizer.normalize(
                rawUrl = rawUrl,
                pageUrl = URI(candidate.url),
                sourceName = "${candidate.sourceName}:nested-direct",
                diagnostics = candidate.diagnostics + "nested_resolved_direct"
            )
            if (normalized != null && !normalized.requiresResolver) {
                results.add(normalized.copy(headers = candidate.headers + mapOf("Referer" to candidate.url)))
            }
        }

        val jsResults = jsEvaluator.evaluateJsVariables(html, candidate.url)
        for (media in jsResults) {
            val normalized = candidateNormalizer.normalize(
                rawUrl = media.url,
                pageUrl = URI(candidate.url),
                sourceName = "${candidate.sourceName}:nested-js:${media.extractionMethod}",
                diagnostics = candidate.diagnostics + "nested_js-eval=${media.sourceType}"
            )
            if (normalized != null && !normalized.requiresResolver) {
                results.add(normalized.copy(headers = candidate.headers + mapOf("Referer" to candidate.url)))
            }
        }

        return results
    }

    suspend fun resolveAll(candidates: List<DetectedMedia>): List<DetectedMedia> = coroutineScope {
        candidates.map { candidate ->
            async { resolve(candidate) }
        }.awaitAll().flatten()
    }

    companion object {
        private const val MAX_RESOLVE_DEPTH = 2

        private val WINDOW_LOCATION_PATTERN = Regex(
            """window\.location(?:\.href)?\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE
        )

        private val DIRECT_PATTERNS = listOf(
            Regex("""(?:file|src|source|video|url|embed|iframe|stream|hls|mp4)\s*[:=]\s*["'`]([^"'`]+)["'`]""", RegexOption.IGNORE_CASE),
            Regex("""["'](?:file|src|source|video|url|embed|iframe|stream|hls|mp4)["']\s*:\s*["'`]([^"'`]+)["'`]""", RegexOption.IGNORE_CASE),
            Regex("""sources?\s*:\s*\[[\s\S]*?(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""data-(?:src|file|video|source)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(https?:\\/\\/[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|ts|mov|avi|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE),
            Regex("""(https?://[^\s"'<>]+\.(?:m3u8|mp4|webm|mkv|ts|mov|avi|mp3|aac|m4a|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        )
    }
}