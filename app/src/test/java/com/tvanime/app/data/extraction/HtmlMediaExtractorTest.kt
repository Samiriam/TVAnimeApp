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

    @Test
    fun extractsScriptAndDataAttributeCandidates() {
        val html = """
            <html>
            <head><title>Script Demo</title></head>
            <body>
                <script>
                    jwplayer("player").setup({ file: "https://cdn.example.com/script-stream.m3u8" });
                </script>
                <div data-file="/media/from-data.mp4"></div>
            </body>
            </html>
        """.trimIndent()

        val result = extractor.extract(URI("https://example.com/page"), html)

        assertEquals("Script Demo", result.title)
        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/script-stream.m3u8" && it.format == "hls" })
        assertTrue(result.candidates.any { it.url == "https://example.com/media/from-data.mp4" && it.format == "mp4" })
    }

    @Test
    fun extractsAggressiveJsonEscapedAndNestedCandidates() {
        val html = """
            <html>
            <body>
                <script type="application/json">
                    {"sources":[{"url":"https:\/\/cdn.example.com\/escaped.m3u8"}],"embed":"https://player.example.com/watch?file=https%3A%2F%2Fcdn.example.com%2Fnested.mp4"}
                </script>
                <div data-embed="https://streamtape.com/e/abc123"></div>
            </body>
            </html>
        """.trimIndent()

        val result = extractor.extract(URI("https://example.com/page"), html)

        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/escaped.m3u8" && it.format == "hls" })
        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/nested.mp4" && it.format == "mp4" })
        assertTrue(result.candidates.any { it.url == "https://streamtape.com/e/abc123" && it.requiresResolver })
    }

    @Test
    fun extractsMaxAggressivePatterns() {
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("https://cdn.example.com/base64.m3u8".toByteArray())
        val html = """
            <html>
            <head><meta http-equiv="refresh" content="0;url=https://cdn.example.com/meta.mp4"></head>
            <body>
                <div data-config="https%3A%2F%2Fcdn.example.com%2Fconfig.m3u8"></div>
                <script>
                    const a = atob("$encoded");
                    const b = decodeURIComponent("https%3A%2F%2Fcdn.example.com%2Fdecode.mp4");
                    const c = { stream: `https://cdn.example.com/template.webm` };
                </script>
            </body>
            </html>
        """.trimIndent()

        val result = extractor.extract(URI("https://example.com/page"), html)

        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/meta.mp4" })
        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/config.m3u8" })
        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/base64.m3u8" })
        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/decode.mp4" })
        assertTrue(result.candidates.any { it.url == "https://cdn.example.com/template.webm" })
    }
}
