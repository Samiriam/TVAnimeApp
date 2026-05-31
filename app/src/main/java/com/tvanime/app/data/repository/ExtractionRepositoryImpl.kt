package com.tvanime.app.data.repository

import com.tvanime.app.data.extraction.HtmlMediaExtractor
import com.tvanime.app.data.extraction.HttpPageFetcher
import com.tvanime.app.data.extraction.UrlPolicyValidator
import com.tvanime.app.domain.model.ExtractionResult
import javax.inject.Inject

class ExtractionRepositoryImpl @Inject constructor(
    private val urlPolicyValidator: UrlPolicyValidator,
    private val httpPageFetcher: HttpPageFetcher,
    private val htmlMediaExtractor: HtmlMediaExtractor
) : ExtractionRepository {

    override suspend fun extractFromPage(pageUrl: String): ExtractionResult {
        val validatedUrl = urlPolicyValidator.validate(pageUrl)
        val html = httpPageFetcher.fetch(validatedUrl)
        return htmlMediaExtractor.extract(validatedUrl, html)
    }
}
