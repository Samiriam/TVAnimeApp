package com.tvanime.app.data.parser

/**
 * Parser de listas M3U.
 *
 * ### Formato soportado
 * ```
 * #EXTM3U
 * #EXTINF:-1 group-title="Anime",Nombre del video
 * https://servidor.com/stream.m3u8
 * ```
 *
 * ### Uso
 * - Colocar la lista en `assets/playlist.m3u` o apuntar a una URL accesible.
 * - Inyectar o crear `M3uPlaylistParser(okHttpClient)`.
 * - Llamar a `parseAsset()` o `parseFromUrl()`.
 */
class M3uPlaylistParser(private val client: okhttp3.OkHttpClient) {

    /**
     * Descarga y parsea una lista M3U desde una URL.
     */
    suspend fun parseFromUrl(url: String): List<PlaylistItem> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val response = client.newCall(
                okhttp3.Request.Builder().url(url).build()
            ).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string().orEmpty()
            parse(body)
        }

    /**
     * Parsea una lista M3U desde una cadena de texto.
     */
    suspend fun parseAsset(content: String): List<PlaylistItem> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            parse(content)
        }

    /**
     * Parsea una cadena M3U.
     */
    private fun parse(raw: String): List<PlaylistItem> {
        val items = mutableListOf<PlaylistItem>()
        var currentTitle = ""
        var currentLogo = ""
        var currentGroup = ""
        var currentUrl = ""

        for (line in raw.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF:") -> {
                    val afterComma = trimmed.substringAfterLast(',')
                    currentTitle = afterComma.trim().ifEmpty { trimmed }
                    currentLogo = Regex("""tvg-logo="([^"]*)"""")
                        .find(trimmed)?.groupValues?.getOrNull(1).orEmpty()
                    currentGroup = Regex("""group-title="([^"]*)"""")
                        .find(trimmed)?.groupValues?.getOrNull(1).orEmpty()
                }

                trimmed.startsWith("http") -> {
                    currentUrl = trimmed
                    items += PlaylistItem(
                        title = currentTitle.ifEmpty { "Sin título" },
                        url = currentUrl,
                        logoUrl = currentLogo,
                        group = currentGroup
                    )
                    currentTitle = ""
                    currentLogo = ""
                    currentGroup = ""
                    currentUrl = ""
                }
            }
        }
        return items
    }
}

/**
 * Item parseado desde la lista M3U.
 */
data class PlaylistItem(
    val title: String,
    val url: String,
    val logoUrl: String = "",
    val group: String = ""
)
