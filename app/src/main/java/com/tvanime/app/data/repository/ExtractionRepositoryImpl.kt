package com.tvanime.app.data.repository

import com.tvanime.app.data.extraction.CandidateNormalizer
import com.tvanime.app.data.extraction.CandidateScorer
import com.tvanime.app.data.extraction.EmbedResolverRegistry
import com.tvanime.app.data.extraction.HtmlMediaExtractor
import com.tvanime.app.data.extraction.HttpPageFetcher
import com.tvanime.app.data.extraction.UrlPolicyValidator
import com.tvanime.app.domain.model.ExtractionResult
import javax.inject.Inject

class ExtractionRepositoryImpl @Inject constructor(
    private val urlPolicyValidator: UrlPolicyValidator,
    private val httpPageFetcher: HttpPageFetcher,
    private val htmlMediaExtractor: HtmlMediaExtractor,
    private val embedResolverRegistry: EmbedResolverRegistry,
    private val candidateNormalizer: CandidateNormalizer,
    private val candidateScorer: CandidateScorer
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

        val html = httpPageFetcher.fetch(validatedUrl)
        val result = htmlMediaExtractor.extract(validatedUrl, html)
        val resolvedCandidates = embedResolverRegistry.resolveAll(result.candidates)
        val deduped = smartDeduplicate(resolvedCandidates)
        val ranked = candidateScorer.rank(deduped)
        return result.copy(candidates = ranked)
    }

    private fun smartDeduplicate(candidates: List<com.tvanime.app.domain.model.DetectedMedia>): List<com.tvanime.app.domain.model.DetectedMedia> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<com.tvanime.app.domain.model.DetectedMedia>()

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

    private fun normalizeFilenameKey(candidate: com.tvanime.app.domain.model.DetectedMedia): String {
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
