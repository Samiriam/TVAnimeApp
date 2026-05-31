package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbedResolverRegistryTest {

    private val serverSpecificResolvers = ServerSpecificResolvers()
    private val fetcher = HttpPageFetcher()
    private val normalizer = CandidateNormalizer(ServerClassifier())
    private val registry = EmbedResolverRegistry(
        httpPageFetcher = fetcher,
        candidateNormalizer = normalizer,
        serverSpecificResolvers = serverSpecificResolvers
    )

    @Test
    fun resolvesDirectCandidateFromEmbedHtml() {
        val embed = DetectedMedia(
            url = "https://embed.example.com/player/123",
            label = "Episode 1",
            mediaType = "video",
            format = "embed",
            referer = "https://example.com/watch",
            sourceName = "test",
            server = "jwplayer",
            requiresResolver = true,
            isDirect = false
        )
        val html = """
            <html><body>
                <script>player.setup({ file: "https://cdn.example.com/final.m3u8" });</script>
            </body></html>
        """.trimIndent()

        val resolved = registry.resolveFromHtml(embed, html)

        requireNotNull(resolved)
        assertEquals("https://cdn.example.com/final.m3u8", resolved.url)
        assertEquals("Episode 1", resolved.label)
        assertEquals("hls", resolved.format)
        assertFalse(resolved.requiresResolver)
        assertTrue(resolved.isDirect)
        assertEquals("https://embed.example.com/player/123", resolved.headers["Referer"])
    }
}
