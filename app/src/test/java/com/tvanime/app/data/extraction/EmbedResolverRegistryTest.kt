package com.tvanime.app.data.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbedResolverRegistryTest {

    private val jsEvaluator = JsEvaluator()
    private val packerUnpacker = PackerUnpacker()

    @Test
    fun extractsJsVariableUrl() {
        val result = jsEvaluator.evaluateJsVariables(
            """var videoUrl = "https://cdn.example.com/video.mp4";""",
            "https://example.com"
        )
        assertTrue(result.isNotEmpty())
        assertEquals("https://cdn.example.com/video.mp4", result[0].url)
    }

    @Test
    fun extractsSourcesArray() {
        val html = """sources: [{file: "https://cdn.example.com/stream.m3u8"}, {file: "https://cdn.example.com/stream2.mp4"}]"""
        val result = jsEvaluator.evaluateJsVariables(html, "https://example.com")
        assertTrue("Should find at least one source", result.isNotEmpty())
    }

    @Test
    fun extractsBase64EncodedUrlFromHtmlExtractor() {
        val base64VideoUrl = java.util.Base64.getEncoder().encodeToString("https://cdn.example.com/stream.m3u8".toByteArray())
        val html = """<script>var x = atob("$base64VideoUrl");</script>"""
        val extractor = HtmlMediaExtractor(CandidateNormalizer(ServerClassifier()))
        val result = extractor.extract(java.net.URI("https://example.com"), html)
        val urls = result.candidates.map { it.url }
        assertTrue("Should contain the base64-decoded URL. Got: $urls",
            urls.any { it.contains("cdn.example.com/stream.m3u8") })
    }

    @Test
    fun packerUnpackerHandlesEmptyInput() {
        val result = packerUnpacker.unpack("not a packed string")
        assertEquals(null, result)
    }

    @Test
    fun extractsFileColonPattern() {
        val result = jsEvaluator.evaluateJsVariables(
            """file: "https://cdn.example.com/episode.mp4"""",
            "https://example.com"
        )
        assertTrue("Should find file: URL", result.isNotEmpty())
    }

    @Test
    fun extractsVideoTagSrcPattern() {
        val result = jsEvaluator.evaluateJsVariables(
            """video_url: "https://cdn.example.com/anime.mp4"""",
            "https://example.com"
        )
        assertTrue("Should find video_url pattern", result.isNotEmpty())
    }

    @Test
    fun normalizerClassifiesHlsCorrectly() {
        val normalizer = CandidateNormalizer(ServerClassifier())
        val result = normalizer.normalize(
            rawUrl = "https://cdn.example.com/stream.m3u8",
            pageUrl = java.net.URI("https://example.com"),
            sourceName = "test"
        )
        assertNotNull(result)
        assertEquals("hls", result!!.format)
        assertTrue(result.isDirect)
    }

    @Test
    fun normalizerClassifiesEmbedCorrectly() {
        val normalizer = CandidateNormalizer(ServerClassifier())
        val result = normalizer.normalize(
            rawUrl = "https://streamtape.com/embed/abc123",
            pageUrl = java.net.URI("https://example.com"),
            sourceName = "test"
        )
        assertNotNull(result)
        assertEquals("embed", result!!.format)
        assertTrue(result.requiresResolver)
    }

    @Test
    fun serverClassifierIdentifiesStreamtape() {
        val classifier = ServerClassifier()
        val result = classifier.classify("https://streamtape.com/embed/abc123")
        assertEquals("streamtape", result.id)
        assertTrue(result.requiresResolver)
    }

    @Test
    fun serverClassifierIdentifiesM3u8() {
        val classifier = ServerClassifier()
        val result = classifier.classify("https://cdn.example.com/video.m3u8")
        assertEquals("m3u8hls", result.id)
        assertFalse(result.requiresResolver)
    }

    private fun assertFalse(condition: Boolean) {
        org.junit.Assert.assertFalse(condition)
    }
}