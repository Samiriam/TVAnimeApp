package com.tvanime.app.data.extraction

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

@Singleton
class HttpPageFetcher @Inject constructor() {

    private val scraperClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLSv1.2").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(5, 60, TimeUnit.SECONDS))
            .proxy(Proxy.NO_PROXY)
            .build()
    }

    suspend fun fetchWithReferer(url: URI, referer: String): String = withContext(Dispatchers.IO) {
        doFetch(url, referer)
    }

    suspend fun fetch(url: URI): String = withContext(Dispatchers.IO) {
        doFetch(url, url.toString())
    }

    private fun doFetch(url: URI, referer: String): String {
        val request = Request.Builder()
            .url(url.toString())
            .removeHeader("User-Agent")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("DNT", "1")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Cache-Control", "max-age=0")
            .header("Referer", referer)
            .build()

        val response = scraperClient.newCall(request).execute()
        return response.use { resp ->
            if (!resp.isSuccessful) {
                val code = resp.code
                if (code in listOf(403, 503)) {
                    throw RuntimeException("Sitio protegido (HTTP $code). Prueba con otra URL.")
                }
                throw RuntimeException("Error HTTP $code al descargar la pagina.")
            }
            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) throw RuntimeException("La pagina no devolvio contenido util.")
            if (body.length > MAX_HTML_CHARS) body.take(MAX_HTML_CHARS) else body
        }
    }

    companion object {
        private const val MAX_HTML_CHARS = 2_000_000
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    }
}
