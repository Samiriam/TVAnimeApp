package com.tvanime.app.data.repository

import android.util.Log
import com.tvanime.app.data.extraction.CandidateNormalizer
import com.tvanime.app.data.extraction.CandidateScorer
import com.tvanime.app.data.extraction.EmbedResolverRegistry
import com.tvanime.app.data.extraction.HtmlMediaExtractor
import com.tvanime.app.data.extraction.HttpPageFetcher
import com.tvanime.app.data.extraction.JsEvaluator
import com.tvanime.app.data.extraction.PackerUnpacker
import com.tvanime.app.data.extraction.UrlPolicyValidator
import com.tvanime.app.data.extraction.WebViewFetcher
import com.tvanime.app.domain.model.DetectedMedia
import com.tvanime.app.domain.model.ExtractionResult
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class ExtractionRepositoryImpl @Inject constructor(
    private val urlPolicyValidator: UrlPolicyValidator,
    private val httpPageFetcher: HttpPageFetcher,
    private val webViewFetcher: WebViewFetcher,
    private val htmlMediaExtractor: HtmlMediaExtractor,
    private val embedResolverRegistry: EmbedResolverRegistry,
    private val candidateNormalizer: CandidateNormalizer,
    private val candidateScorer: CandidateScorer,
    private val jsEvaluator: JsEvaluator,
    private val packerUnpacker: PackerUnpacker
) : ExtractionRepository {

    override suspend fun extractFromPage(pageUrl: String): ExtractionResult {
        val validatedUrl = urlPolicyValidator.validate(pageUrl)

        val directCandidate = candidateNormalizer.normalize(
            rawUrl = validatedUrl.toString(),
            pageUrl = validatedUrl,
            sourceName = "direct-url"
        )
        if (directCandidate != null && directCandidate.isDirect) {
            return ExtractionResult(
                pageUrl = validatedUrl.toString(),
                sourceHost = validatedUrl.host.orEmpty(),
                title = validatedUrl.path.substringAfterLast('/').ifBlank { validatedUrl.host.orEmpty() },
                candidates = listOf(directCandidate)
            )
        }

        val html = tryOkHttpFetch(validatedUrl.toString())

        val result = if (html != null && html.length > 500) {
            extractFromHtml(html, validatedUrl)
        } else {
            tryWebViewFetch(validatedUrl.toString())
        }

        val resolvedCandidates = embedResolverRegistry.resolveAll(result.candidates)
        val deduped = smartDeduplicate(resolvedCandidates)
        val ranked = candidateScorer.rank(deduped)

        if (ranked.isEmpty() && html != null && html.length > 500) {
            val webViewResult = tryWebViewFetch(validatedUrl.toString())
            if (webViewResult.candidates.isNotEmpty()) {
                val wvResolved = embedResolverRegistry.resolveAll(webViewResult.candidates)
                val wvDeduped = smartDeduplicate(wvResolved)
                val wvRanked = candidateScorer.rank(wvDeduped)
                if (wvRanked.isNotEmpty()) {
                    return webViewResult.copy(candidates = wvRanked)
                }
            }
        }

        return result.copy(candidates = ranked)
    }

    private suspend fun tryOkHttpFetch(url: String): String? {
        return runCatching {
            val uri = java.net.URI(url)
            httpPageFetcher.fetch(uri)
        }.getOrNull()?.takeIf { it.length > 500 }
    }

    private suspend fun tryWebViewFetch(url: String): ExtractionResult {
        return runCatching {
            val wvResult = webViewFetcher.fetchPage(url)
            if (wvResult.success && wvResult.html.length > 500) {
                val uri = java.net.URI(wvResult.finalUrl)
                extractFromHtml(wvResult.html, uri)
            } else {
                ExtractionResult(
                    pageUrl = url,
                    sourceHost = java.net.URI(url).host.orEmpty(),
                    title = "Sin resultados",
                    candidates = emptyList()
                )
            }
        }.getOrElse { error ->
            ExtractionResult(
                pageUrl = url,
                sourceHost = java.net.URI(url).host.orEmpty(),
                title = "Error",
                candidates = emptyList()
            )
        }
    }

    private fun extractFromHtml(html: String, pageUrl: java.net.URI): ExtractionResult {
        val allCandidates = mutableListOf<DetectedMedia>()

        val extractorResult = runCatching { htmlMediaExtractor.extract(pageUrl, html) }.getOrNull()
        if (extractorResult != null) {
            allCandidates.addAll(extractorResult.candidates)
        }

        val jsMedia = jsEvaluator.evaluateJsVariables(html, pageUrl.toString())
        for (media in jsMedia) {
            val normalized = candidateNormalizer.normalize(
                rawUrl = media.url,
                pageUrl = pageUrl,
                sourceName = "js-eval:${media.extractionMethod}",
                diagnostics = listOf("js-eval=${media.sourceType}")
            )
            if (normalized != null && normalized.url !in allCandidates.map { it.url }) {
                allCandidates.add(normalized)
            }
        }

        val unpackedResults = packerUnpacker.tryUnpackAll(html)
        for (unpacked in unpackedResults) {
            val unpackedMedia = jsEvaluator.evaluateJsVariables(unpacked, pageUrl.toString())
            for (media in unpackedMedia) {
                val normalized = candidateNormalizer.normalize(
                    rawUrl = media.url,
                    pageUrl = pageUrl,
                    sourceName = "js-unpack:${media.extractionMethod}",
                    diagnostics = listOf("js-unpack=${media.sourceType}")
                )
                if (normalized != null && normalized.url !in allCandidates.map { it.url }) {
                    allCandidates.add(normalized)
                }
            }
        }

        val title = runCatching {
            org.jsoup.Jsoup.parse(html, pageUrl.toString()).title().ifBlank { pageUrl.host }
        }.getOrDefault(pageUrl.host.orEmpty())

        if (allCandidates.isEmpty()) {
            throw RuntimeException("No se detectaron enlaces multimedia en la pagina. Intenta con la URL directa del video.")
        }

        return ExtractionResult(
            pageUrl = pageUrl.toString(),
            sourceHost = pageUrl.host.orEmpty(),
            title = title,
            candidates = allCandidates
        )
    }

    private fun smartDeduplicate(candidates: List<DetectedMedia>): List<DetectedMedia> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<DetectedMedia>()

        for (candidate in candidates) {
            val exactKey = candidate.url.trim().lowercase()
            if (exactKey in seen) continue

            val normalizedKey = normalizeFilenameKey(candidate)
            val hostKey = extractHost(candidate.url)
            val qualityKey = candidate.quality?.lowercase() ?: ""
            val smartKey = if (normalizedKey.isNotBlank()) {
                "fn:$normalizedKey|$qualityKey"
            } else if (hostKey.isNotBlank()) {
                "host:$hostKey|$qualityKey"
            } else {
                "exact:$exactKey"
            }

            if (smartKey in seen) continue
            seen.add(exactKey)
            seen.add(smartKey)
            result.add(candidate)
        }

        return result
    }

    private fun normalizeFilenameKey(candidate: DetectedMedia): String {
        val url = candidate.url.lowercase()
        val path = url.substringBefore('?').substringAfterLast('/')
        return path.replace(Regex("\\.[a-z0-9]{2,5}$"), "")
            .replace(Regex("\\[[^\\]]+]|\\([^)]+\\)"), " ")
            .replace(Regex("[_+.]+"), " ")
            .replace(Regex("\\b(2160p|1440p|1080p|720p|480p|360p|hevc|x265|x264|h264|hdr|hdr10\\+?|dovi|dv|10bit|aac|atmos|web[- ]dl|webrip|bluray|multi)\\b"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractHost(url: String): String {
        return runCatching {
            java.net.URI(url).host.orEmpty().removePrefix("www.").lowercase()
        }.getOrDefault("")
    }
}