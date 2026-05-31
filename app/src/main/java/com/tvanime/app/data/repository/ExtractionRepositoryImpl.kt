package com.tvanime.app.data.repository

import com.tvanime.app.data.extraction.HtmlMediaExtractor
import com.tvanime.app.data.extraction.HttpPageFetcher
import com.tvanime.app.data.extraction.UrlPolicyValidator
import com.tvanime.app.data.extraction.EmbedResolverRegistry
import com.tvanime.app.data.extraction.CandidateNormalizer
import com.tvanime.app.domain.model.ExtractionResult
import javax.inject.Inject

class ExtractionRepositoryImpl @Inject constructor(
    private val urlPolicyValidator: UrlPolicyValidator,
    private val httpPageFetcher: HttpPageFetcher,
    private val htmlMediaExtractor: HtmlMediaExtractor,
    private val embedResolverRegistry: EmbedResolverRegistry,
    private val candidateNormalizer: CandidateNormalizer
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
        val resolvedCandidates = result.candidates.map { embedResolverRegistry.resolve(it) }
            .distinctBy { it.url }
            .sortedBy { it.priority }
        return result.copy(candidates = resolvedCandidates)
    }
}
