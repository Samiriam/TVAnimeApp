package com.tvanime.app.data.capture

import android.webkit.WebView
import android.webkit.CookieManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebViewSessionManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {
    private val cookieManager = CookieManager.getInstance()

    init {
        cookieManager.setAcceptCookie(true)
    }

    fun configure(webView: WebView) {
        cookieManager.setAcceptThirdPartyCookies(webView, true)
    }

    fun getCookiesForUrl(url: String): Map<String, String> {
        val domain = try {
            java.net.URI(url).host
        } catch (e: Exception) {
            return emptyMap()
        }

        val rawCookies = cookieManager.getCookie(url) ?: return emptyMap()
        return rawCookies.split(";").associate {
            val parts = it.trim().split("=", limit = 2)
            parts.getOrElse(0) { "" }.trim() to parts.getOrElse(1) { "" }.trim()
        }
    }

    fun getCookieHeader(url: String): String {
        return cookieManager.getCookie(url) ?: ""
    }

    fun getPlaybackHeaders(url: String, referer: String): Map<String, String> {
        return buildMap {
            if (referer.isNotBlank()) put("Referer", referer)
            put("User-Agent", USER_AGENT)
            getCookieHeader(url).takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
        }
    }

    fun setCookies(url: String, cookies: Map<String, String>) {
        cookies.forEach { (name, value) ->
            cookieManager.setCookie(url, "$name=$value")
        }
        cookieManager.flush()
    }

    fun clearSession() {
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android TV 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 TVAnimeApp"
    }
}
