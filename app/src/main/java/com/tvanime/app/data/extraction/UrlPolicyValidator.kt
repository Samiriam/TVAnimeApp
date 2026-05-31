package com.tvanime.app.data.extraction

import java.net.InetAddress
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlPolicyValidator @Inject constructor() {

    fun validate(rawUrl: String): URI {
        val url = rawUrl.trim()
        require(url.isNotBlank()) { "Ingresa una URL publica valida." }

        val uri = URI(url)
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Solo se permiten URLs https."
        }

        val host = uri.host?.lowercase().orEmpty()
        require(host.isNotBlank()) { "La URL no contiene un host valido." }
        require(!host.isBlockedHost()) { "El host no esta permitido." }

        return uri
    }

    private fun String.isBlockedHost(): Boolean {
        if (this == "localhost" || this.endsWith(".local")) return true
        if (this == "169.254.169.254") return true

        val ip = runCatching { InetAddress.getByName(this) }.getOrNull() ?: return false
        return ip.isAnyLocalAddress ||
            ip.isLoopbackAddress ||
            ip.isLinkLocalAddress ||
            ip.isSiteLocalAddress ||
            this.startsWith("0.")
    }
}
