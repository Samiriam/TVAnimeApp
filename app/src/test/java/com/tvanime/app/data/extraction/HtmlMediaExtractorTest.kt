package com.tvanime.app.data.extraction

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlMediaExtractorTest {

    private val serverClassifier = ServerClassifier()
    private val normalizer = CandidateNormalizer(serverClassifier)
    private val extractor = HtmlMediaExtractor(normalizer)

    @Test
    fun extractsPlayableCandidatesAndDeduplicates() {
        val html = """
            <html>
            <head><title>Demo</title></head>
            <body>
                <video src="/media/video.mp4"></video>
                <source src="https://cdn.example.com/stream.m3u8" />
                <a href="https://cdn.example.com/stream.m3u8">duplicate</a>
                <iframe src="https://embed.example.com/player"></iframe>
            </body>
            </html>
        """.trimIndent()

        val result = extractor.extract(URI("https://example.com/page"), html)

        assertEquals("Demo", result.title)
        assertEquals(3, result.candidates.size)
        assertTrue(result.candidates.any { it.url == "https://example.com/media/video.mp4" && it.format == "mp4" })
        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/stream.m3u8" && it.format == "hls" })
        assertTrue(result.candidates.any { it.format == "embed" && it.requiresResolver })
        assertTrue(result.candidates.first().priority <= result.candidates.last().priority)
    }
}
