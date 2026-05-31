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
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        val scopedClient = client.newBuilder()
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

        scopedClient.newCall(request).execute().use { response ->
            require(response.isSuccessful) {
                "No se pudo descargar la pagina (${response.code})."
            }

            val body = response.body?.string().orEmpty()
            require(body.isNotBlank()) { "La pagina no devolvio HTML util." }
            require(body.length <= MAX_HTML_CHARS) { "La pagina es demasiado grande para este primer extractor." }
            body
        }
    }

    companion object {
        private const val MAX_HTML_CHARS = 1_000_000
    }
}
