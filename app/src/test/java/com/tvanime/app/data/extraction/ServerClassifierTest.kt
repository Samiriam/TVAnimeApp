package com.tvanime.app.data.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerClassifierTest {

    private val classifier = ServerClassifier()

    @Test
    fun classifiesHlsAsDirect() {
        val info = classifier.classify("https://cdn.example.com/live/stream.m3u8")

        assertEquals("m3u8hls", info.id)
        assertEquals("hls", info.format)
        assertTrue(info.isDirect)
        assertFalse(info.requiresResolver)
    }

    @Test
    fun classifiesKnownEmbedHostAsResolverRequired() {
        val info = classifier.classify("https://streamtape.com/e/abc123")

        assertEquals("streamtape", info.id)
        assertEquals("embed", info.format)
        assertFalse(info.isDirect)
        assertTrue(info.requiresResolver)
    }
}
