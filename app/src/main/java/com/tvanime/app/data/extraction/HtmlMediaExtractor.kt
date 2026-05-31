package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import com.tvanime.app.domain.model.ExtractionResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
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
            addAll(extractAnchorLinks(document, pageUrl))
            addAll(extractEmbeds(document, pageUrl))
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
                buildCandidate(url = url, pageUrl = pageUrl)
            }
    }

    private fun extractAnchorLinks(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("a[href]")
            .mapNotNull { element ->
                val url = element.absUrl("href").ifBlank { element.attr("href") }
                buildCandidate(url = url, pageUrl = pageUrl)
            }
    }

    private fun extractEmbeds(document: Document, pageUrl: URI): List<DetectedMedia> {
        return document.select("iframe[src]")
            .mapNotNull { element ->
                val url = element.absUrl("src").ifBlank { element.attr("src") }
                if (url.isBlank()) null else candidateNormalizer.normalize(
                    rawUrl = url,
                    pageUrl = pageUrl,
                    sourceName = "generic-html",
                    explicitFormat = "embed",
                    label = "Embed"
                )
            }
    }

    private fun buildCandidate(url: String, pageUrl: URI): DetectedMedia? {
        return candidateNormalizer.normalize(
            rawUrl = url,
            pageUrl = pageUrl,
            sourceName = "generic-html"
        )
    }
}
