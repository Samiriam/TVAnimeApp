package com.tvanime.app.data.extraction

import com.tvanime.app.domain.model.DetectedMedia
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CandidateScorer @Inject constructor() {

    fun score(candidate: DetectedMedia): Int {
        var score = 0

        score += transportScore(candidate)
        score += qualityScore(candidate)
        score += codecScore(candidate)
        score += serverScore(candidate)
        score += directnessScore(candidate)
        score -= penaltyScore(candidate)

        return score
    }

    fun rank(candidates: List<DetectedMedia>): List<DetectedMedia> {
        return candidates.sortedByDescending { score(it) }
    }

    fun formatBadge(candidate: DetectedMedia): String {
        val parts = mutableListOf<String>()
        val url = candidate.url.lowercase()
        val label = "${candidate.label} ${candidate.quality.orEmpty()}".lowercase()

        when {
            url.contains(".mp4") -> parts.add("MP4")
            url.contains(".m3u8") -> parts.add("HLS")
            url.contains(".webm") -> parts.add("WEBM")
            url.contains(".mkv") -> parts.add("MKV")
            url.contains(".mp3") || url.contains(".aac") || url.contains(".m4a") -> parts.add("AUDIO")
            else -> parts.add("HTTP")
        }

        when {
            label.contains("hevc") || label.contains("x265") -> parts.add("HEVC")
            label.contains("h264") || label.contains("x264") -> parts.add("H264")
        }

        candidate.quality?.let { q ->
            val normalized = q.lowercase()
            if (normalized.isNotBlank() && normalized !in parts.map { it.lowercase() }) {
                parts.add(q.uppercase())
            }
        }

        return parts.joinToString("/")
    }

    private fun transportScore(candidate: DetectedMedia): Int {
        val url = candidate.url.lowercase()
        return when {
            url.contains(".mp4") -> 120
            url.contains(".m3u8") -> 100
            url.contains(".webm") -> 70
            url.contains(".mkv") -> 40
            url.contains(".mp3") || url.contains(".aac") || url.contains(".m4a") -> 60
            else -> 50
        }
    }

    private fun qualityScore(candidate: DetectedMedia): Int {
        val q = candidate.quality?.lowercase() ?: return 0
        return when {
            q.contains("2160") || q == "4k" -> 2160
            q.contains("1440") -> 1440
            q.contains("1080") || q == "fhd" -> 1080
            q.contains("720") || q == "hd" -> 720
            q.contains("480") || q == "sd" -> 480
            q.contains("360") -> 360
            else -> 0
        }
    }

    private fun codecScore(candidate: DetectedMedia): Int {
        val text = "${candidate.label} ${candidate.quality.orEmpty()}".lowercase()
        var score = 0
        if (text.contains("hevc") || text.contains("x265") || text.contains("10bit") ||
            text.contains("hdr") || text.contains("dolby") || text.contains("remux")) {
            score -= 400
        }
        if (text.contains("x264") || text.contains("h264") || text.contains("aac")) {
            score += 80
        }
        return score
    }

    private fun serverScore(candidate: DetectedMedia): Int {
        return when (candidate.server) {
            "directo" -> 100
            "m3u8hls" -> 90
            "archiveorg" -> 80
            "blogger" -> 75
            "okru" -> 70
            "yourupload" -> 65
            "mp4upload" -> 60
            "streamwish" -> 55
            "voe" -> 50
            "streamtape" -> 45
            "mixdrop" -> 40
            "doodstream" -> 35
            "fembed" -> 30
            "streamsb" -> 25
            "vidmoly" -> 20
            "uqload" -> 15
            "sendvid" -> 10
            "mediafire" -> 5
            else -> 0
        }
    }

    private fun directnessScore(candidate: DetectedMedia): Int {
        return if (candidate.isDirect && !candidate.requiresResolver) 200 else 0
    }

    private fun penaltyScore(candidate: DetectedMedia): Int {
        var penalty = 0
        val url = candidate.url.lowercase()
        if (url.contains("auto")) penalty += 40
        if (candidate.diagnostics.any { it.contains("no-direct-candidate") }) penalty += 100
        if (candidate.diagnostics.any { it.contains("resolver_error") }) penalty += 200
        return penalty
    }
}
