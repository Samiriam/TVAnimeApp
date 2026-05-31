package com.tvanime.app.data.extraction

import java.net.URI
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerSpecificResolvers @Inject constructor() {

    fun resolveForServer(serverId: String, html: String, embedUrl: String): String? {
        val resolver = RESOLVERS[serverId] ?: return null
        return resolver(html, embedUrl)
    }

    fun hasSpecificResolver(serverId: String): Boolean = serverId in RESOLVERS

    companion object {
        private val STREAMTAPE_PATTERNS = listOf(
            Regex("""(https?://[^\s"']*streamtape\.(?:com|net|site|to)/(?:get_video|ab-get-video)[^\s"']*)""", RegexOption.IGNORE_CASE),
            Regex("""videolink[^\n]+?href=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""["']([^"']*streamtape\.(?:com|net|site|to)/get_video[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""["']([^"']*streamtape\.(?:com|net|site|to)/ab-get-video[^"']*)["']""", RegexOption.IGNORE_CASE)
        )

        private val STREAMWISH_PATTERNS = listOf(
            Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""", RegexOption.IGNORE_CASE),
            Regex("""file\s*:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""sources\s*:\s*\[\s*\{[^}]*file\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""["']file["']\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""player\.setup\(\{[^}]*file\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )

        private val VOE_PATTERNS = listOf(
            Regex("""(?:mp4|hls)'\s*:\s*'([^']+)""", RegexOption.IGNORE_CASE),
            Regex("""sources.*?\{[^}]*:\s*"([^"]+)""", RegexOption.IGNORE_CASE),
            Regex("""["']file["']\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""let\s+[0-9a-f]+\s*=\s*'([A-Za-z0-9+/=]{20,})'""", RegexOption.IGNORE_CASE)
        )

        private val MIXDROP_PATTERNS = listOf(
            Regex("""MDCore\.\w+\s*=\s*"([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex("""file\s*:\s*["']([^"']+\.mp4[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""sources?\s*:\s*\[\s*\{[^}]*file\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )

        private val DOODSTREAM_PATTERNS = listOf(
            Regex("""["']file["']\s*:\s*["']([^"']+\.mp4[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""["']video["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(https?://[^\s"'<>]+\.mp4[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        )

        private val OKRU_PATTERNS = listOf(
            Regex("""["']url["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""flashvars\s*=\s*\{[^}]*src\s*:\s*"([^"]+)""", RegexOption.IGNORE_CASE),
            Regex("""videoUrl\s*=\s*"([^"]+)""", RegexOption.IGNORE_CASE),
            Regex("""["']name["']\s*:\s*["'][^"']*["']\s*,\s*["']url["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )

        private val YOURUPLOAD_PATTERNS = listOf(
            Regex("""file["']?\s*:\s*["']([^"']+\.mp4[^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""sources?\s*:\s*\[\s*\{[^}]*src\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""video\[[^\]]+\]\s*=\s*["']([^"']+\.mp4[^"']*)["']""", RegexOption.IGNORE_CASE)
        )

        private val FEMBED_PATTERNS = listOf(
            Regex("""sources?\s*:\s*\[\s*\{[^}]*src\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""file["']?\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""video\s*=\s*["']([^"']+\.mp4[^"']*)["']""", RegexOption.IGNORE_CASE)
        )

        private val STREAMSB_PATTERNS = listOf(
            Regex("""sources?\s*:\s*\[\s*\{[^}]*file\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""file\s*:\s*"([^"]+\.m3u8[^"]*)"""", RegexOption.IGNORE_CASE),
            Regex("""["']file["']\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
        )

        private val VIDMOLY_PATTERNS = listOf(
            Regex("""sources?\s*:\s*\[\s*\{[^}]*file\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""file\s*:\s*"([^"]+\.m3u8[^"]*)"""", RegexOption.IGNORE_CASE)
        )

        private val UQLOAD_PATTERNS = listOf(
            Regex("""sources?\s*:\s*\[\s*"(https?://[^"]+)"""", RegexOption.IGNORE_CASE),
            Regex("""file\s*:\s*"([^"]+\.mp4[^"]*)"""", RegexOption.IGNORE_CASE)
        )

        private val SENDVID_PATTERNS = listOf(
            Regex("""<source\s+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(https?://[^\s"'<>]+\.mp4[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        )

        private val MEDIAFIRE_PATTERNS = listOf(
            Regex("""href="(https?://download[^\s"']+)"""", RegexOption.IGNORE_CASE),
            Regex("""aria-label="Download file"[^>]*href="([^"]+)"""", RegexOption.IGNORE_CASE)
        )

        private val WINDOW_LOCATION_PATTERN = Regex(
            """window\.location(?:\.href)?\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE
        )

        private val GENERIC_DIRECT_PATTERNS = listOf(
            Regex("""file\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""["']file["']\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""sources?\s*:\s*\[\s*\{[^}]*(?:file|src)\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""player\.setup\(\{[^}]*(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""jwplayer\([^)]*\)\.setup\([\s\S]*?(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(https?://[^\s"'<>]+\.(?:m3u8|mp4|webm)[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        )

        private fun findFirstMatch(html: String, patterns: List<Regex>): String? {
            for (pattern in patterns) {
                val match = pattern.find(html) ?: continue
                val url = match.groupValues.getOrNull(1).orEmpty().trim()
                if (url.isNotBlank() && isLikelyPlayable(url)) {
                    return cleanExtractedUrl(url)
                }
            }
            return null
        }

        private fun isLikelyPlayable(url: String): Boolean {
            val lower = url.lowercase()
            if (NOISE_TOKENS.any { lower.contains(it) }) return false
            return lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".webm") ||
                lower.contains("video") || lower.contains("stream") || lower.contains("get_video") ||
                lower.contains("hls") || lower.contains("embed") || lower.contains("play")
        }

        private fun cleanExtractedUrl(url: String): String {
            return url.replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .replace("\\x26", "&")
                .trim()
        }

        private fun tryBase64DecodeVoe(html: String): String? {
            val match = VOE_PATTERNS[3].find(html) ?: return null
            val encoded = match.groupValues.getOrNull(1) ?: return null
            return runCatching {
                val decoded = String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
                if (decoded.startsWith("http")) {
                    decoded.split("&").firstOrNull()?.trim()
                } else {
                    val reversed = decoded.reversed()
                    val fileMatch = Regex("""["']file["']\s*:\s*["']([^"']+)""").find(reversed)
                    fileMatch?.groupValues?.getOrNull(1)
                }
            }.getOrNull()
        }

        private fun resolveStreamtape(html: String, embedUrl: String): String? {
            return findFirstMatch(html, STREAMTAPE_PATTERNS)
        }

        private fun resolveStreamwish(html: String, embedUrl: String): String? {
            return findFirstMatch(html, STREAMWISH_PATTERNS)
                ?: findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveVoe(html: String, embedUrl: String): String? {
            val direct = findFirstMatch(html, VOE_PATTERNS.take(3))
            if (direct != null) return direct
            val base64Result = tryBase64DecodeVoe(html)
            if (base64Result != null) return base64Result
            return findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveMixdrop(html: String, embedUrl: String): String? {
            val raw = findFirstMatch(html, MIXDROP_PATTERNS)
            if (raw != null) {
                return if (raw.startsWith("//")) "https:$raw" else raw
            }
            return findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveDoodstream(html: String, embedUrl: String): String? {
            return findFirstMatch(html, DOODSTREAM_PATTERNS)
                ?: findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveOkru(html: String, embedUrl: String): String? {
            val result = findFirstMatch(html, OKRU_PATTERNS)
            return result?.replace("\\u0026", "&")?.replace("%3B", ";")?.replace("u0026", "&")
        }

        private fun resolveYourUpload(html: String, embedUrl: String): String? {
            return findFirstMatch(html, YOURUPLOAD_PATTERNS)
                ?: findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveFembed(html: String, embedUrl: String): String? {
            return findFirstMatch(html, FEMBED_PATTERNS)
                ?: findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveStreamSB(html: String, embedUrl: String): String? {
            return findFirstMatch(html, STREAMSB_PATTERNS)
                ?: findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveVidmoly(html: String, embedUrl: String): String? {
            return findFirstMatch(html, VIDMOLY_PATTERNS)
                ?: findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveUqload(html: String, embedUrl: String): String? {
            return findFirstMatch(html, UQLOAD_PATTERNS)
                ?: findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveSendvid(html: String, embedUrl: String): String? {
            return findFirstMatch(html, SENDVID_PATTERNS)
                ?: findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private fun resolveMediafire(html: String, embedUrl: String): String? {
            return findFirstMatch(html, MEDIAFIRE_PATTERNS)
        }

        private fun resolveGeneric(html: String, embedUrl: String): String? {
            val redirect = WINDOW_LOCATION_PATTERN.find(html)?.groupValues?.getOrNull(1)
            if (redirect != null && isLikelyPlayable(redirect)) {
                return cleanExtractedUrl(redirect)
            }
            return findFirstMatch(html, GENERIC_DIRECT_PATTERNS)
        }

        private val RESOLVERS = mapOf(
            "streamtape" to ::resolveStreamtape,
            "streamwish" to ::resolveStreamwish,
            "voe" to ::resolveVoe,
            "mixdrop" to ::resolveMixdrop,
            "doodstream" to ::resolveDoodstream,
            "okru" to ::resolveOkru,
            "yourupload" to ::resolveYourUpload,
            "fembed" to ::resolveFembed,
            "streamsb" to ::resolveStreamSB,
            "vidmoly" to ::resolveVidmoly,
            "uqload" to ::resolveUqload,
            "sendvid" to ::resolveSendvid,
            "mediafire" to ::resolveMediafire,
            "mp4upload" to ::resolveGeneric,
            "jwplayer" to ::resolveGeneric,
            "jwp" to ::resolveGeneric,
            "dailymotion" to ::resolveGeneric,
            "blogger" to ::resolveGeneric
        )

        private val NOISE_TOKENS = listOf(
            "cloudflareinsights", "google-analytics", "googletagmanager",
            "facebook.net", "beacon.min.js", "analytics", "pixel",
            "placeholder", "favicon", ".js?", ".css?", ".png", ".jpg",
            ".gif", ".svg", ".ico", ".woff"
        )
    }
}
