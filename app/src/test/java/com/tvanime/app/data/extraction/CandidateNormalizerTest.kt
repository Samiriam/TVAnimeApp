package com.tvanime.app.data.extraction

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateNormalizerTest {

    private val normalizer = CandidateNormalizer(ServerClassifier())

    @Test
    fun cleansEscapedUrlAndClassifiesHls() {
        val candidate = normalizer.normalize(
            rawUrl = "https:%2F%2Fcdn.example.com%2Fstream.m3u8?token=1\\u0026v=2",
            pageUrl = URI("https://example.com/watch"),
            sourceName = "test"
        )

        requireNotNull(candidate)
        assertEquals("https://cdn.example.com/stream.m3u8?token=1&v=2", candidate.url)
        assertEquals("m3u8hls", candidate.server)
        assertEquals("hls", candidate.format)
        assertTrue(candidate.isDirect)
    }

    @Test
    fun decodesBase64DirectUrl() {
        val encoded = java.util.Base64.getEncoder().encodeToString("https://videos.example.com/movie.mp4".toByteArray())

        val candidate = normalizer.normalize(
            rawUrl = encoded,
            pageUrl = URI("https://example.com/watch"),
            sourceName = "test"
        )

        requireNotNull(candidate)
        assertEquals("https://videos.example.com/movie.mp4", candidate.url)
        assertEquals("mp4", candidate.format)
    }

    @Test
    fun rejectsAnalyticsNoise() {
        val candidate = normalizer.normalize(
            rawUrl = "https://www.google-analytics.com/beacon.mp4",
            pageUrl = URI("https://example.com/watch"),
            sourceName = "test"
        )

        assertNull(candidate)
    }

    @Test
    fun extractsNestedPlayableUrlFromEncodedQuery() {
        val candidate = normalizer.normalize(
            rawUrl = "https://player.example.com/embed?url=https%3A%2F%2Fcdn.example.com%2Fnested.m3u8%3Ftoken%3Dabc",
            pageUrl = URI("https://example.com/watch"),
            sourceName = "test"
        )

        requireNotNull(candidate)
        assertEquals("https://cdn.example.com/nested.m3u8?token=abc", candidate.url)
        assertEquals("hls", candidate.format)
    }

    @Test
    fun decodesBase64UrlWithoutPadding() {
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("https://cdn.example.com/no-padding.mp4".toByteArray())

        val candidate = normalizer.normalize(
            rawUrl = encoded,
            pageUrl = URI("https://example.com/watch"),
            sourceName = "test"
        )

        requireNotNull(candidate)
        assertEquals("https://cdn.example.com/no-padding.mp4", candidate.url)
        assertEquals("mp4", candidate.format)
    }
}
