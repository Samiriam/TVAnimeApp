package com.tvanime.app.data.extraction

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpPageFetcher @Inject constructor(
    private val client: OkHttpClient
) {

    suspend fun fetch(url: URI): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url.toString())
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Cache-Control", "max-age=0")
            .build()

        val scopedClient = client.newBuilder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        scopedClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("No se pudo descargar la pagina (HTTP ${response.code}). Intenta con otra URL.")
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                throw RuntimeException("La pagina no devolvio contenido util.")
            }
            
            if (body.length > MAX_HTML_CHARS) {
                body.take(MAX_HTML_CHARS)
            } else {
                body
            }
        }
    }

    companion object {
        private const val MAX_HTML_CHARS = 2_000_000
        
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
