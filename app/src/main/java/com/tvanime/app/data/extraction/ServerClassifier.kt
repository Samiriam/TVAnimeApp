package com.tvanime.app.data.extraction

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerClassifier @Inject constructor() {

    fun classify(url: String, explicitFormat: String? = null): ServerInfo {
        val normalizedHost = runCatching { URI(url).host.orEmpty().removePrefix("www.").lowercase() }
            .getOrDefault("")
        val lower = url.lowercase()

        val matched = SERVER_PATTERNS.firstOrNull { pattern ->
            pattern.hostTokens.any { normalizedHost.contains(it) } || pattern.urlTokens.any { lower.contains(it) }
        }

        val format = explicitFormat ?: inferFormat(lower, matched)
        val isDirect = format != "embed" && (matched?.requiresResolver != true)

        return ServerInfo(
            id = matched?.id ?: if (format == "hls") "m3u8hls" else "directo",
            label = matched?.label ?: if (format == "hls") "HLS" else "Directo",
            format = format,
            isDirect = isDirect,
            requiresResolver = !isDirect,
            priority = matched?.priority ?: if (format == "hls") 10 else 20
        )
    }

    private fun inferFormat(lower: String, matched: ServerPattern?): String = when {
        lower.contains(".m3u8") || matched?.id == "m3u8hls" -> "hls"
        lower.contains(".mp4") -> "mp4"
        lower.contains(".webm") || lower.contains(".mkv") -> "file"
        lower.contains(".mp3") || lower.contains(".aac") || lower.contains(".m4a") || lower.contains(".ogg") -> "audio"
        matched?.requiresResolver == true -> "embed"
        else -> "file"
    }

    private data class ServerPattern(
        val id: String,
        val label: String,
        val hostTokens: List<String> = emptyList(),
        val urlTokens: List<String> = emptyList(),
        val requiresResolver: Boolean,
        val priority: Int
    )

    companion object {
        private val SERVER_PATTERNS = listOf(
            ServerPattern("m3u8hls", "HLS", urlTokens = listOf(".m3u8", "hls"), requiresResolver = false, priority = 10),
            ServerPattern("directo", "Archivo directo", urlTokens = listOf(".mp4", ".webm", ".mkv"), requiresResolver = false, priority = 20),
            ServerPattern("jwplayer", "JWPlayer", urlTokens = listOf("jwplayer", "player.setup"), requiresResolver = true, priority = 30),
            ServerPattern("jwp", "JWP", hostTokens = listOf("jwp"), requiresResolver = true, priority = 35),
            ServerPattern("dailymotion", "Dailymotion", hostTokens = listOf("dailymotion"), requiresResolver = true, priority = 40),
            ServerPattern("blogger", "Blogger", hostTokens = listOf("blogger", "blogspot", "googlevideo"), requiresResolver = true, priority = 45),
            ServerPattern("archiveorg", "Archive.org", hostTokens = listOf("archive.org"), requiresResolver = false, priority = 50),
            ServerPattern("okru", "Ok.ru", hostTokens = listOf("ok.ru", "odnoklassniki"), requiresResolver = true, priority = 55),
            ServerPattern("mp4upload", "MP4Upload", hostTokens = listOf("mp4upload"), requiresResolver = true, priority = 60),
            ServerPattern("yourupload", "YourUpload", hostTokens = listOf("yourupload"), requiresResolver = true, priority = 65),
            ServerPattern("streamtape", "Streamtape", hostTokens = listOf("streamtape"), requiresResolver = true, priority = 70),
            ServerPattern("streamwish", "Streamwish/Filemoon", hostTokens = listOf("streamwish", "sfastwish", "filemoon", "flaswish"), requiresResolver = true, priority = 75),
            ServerPattern("voe", "VOE", hostTokens = listOf("voe"), requiresResolver = true, priority = 80),
            ServerPattern("mixdrop", "Mixdrop", hostTokens = listOf("mixdrop"), requiresResolver = true, priority = 85),
            ServerPattern("doodstream", "Doodstream", hostTokens = listOf("doodstream", "dood"), requiresResolver = true, priority = 90),
            ServerPattern("streamsb", "StreamSB", hostTokens = listOf("streamsb", "sbembed", "sbvideo", "sbrapid"), requiresResolver = true, priority = 95),
            ServerPattern("vidmoly", "Vidmoly", hostTokens = listOf("vidmoly"), requiresResolver = true, priority = 100),
            ServerPattern("uqload", "Uqload", hostTokens = listOf("uqload"), requiresResolver = true, priority = 105),
            ServerPattern("fembed", "Fembed", hostTokens = listOf("fembed", "feurl", "femax"), requiresResolver = true, priority = 110),
            ServerPattern("sendvid", "Sendvid", hostTokens = listOf("sendvid"), requiresResolver = true, priority = 115),
            ServerPattern("mediafire", "MediaFire", hostTokens = listOf("mediafire"), requiresResolver = true, priority = 120)
        )
    }
}

data class ServerInfo(
    val id: String,
    val label: String,
    val format: String,
    val isDirect: Boolean,
    val requiresResolver: Boolean,
    val priority: Int
)
