package com.tvanime.app.data.extraction

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlPolicyValidator @Inject constructor() {

    fun validate(rawUrl: String): URI {
        var url = rawUrl.trim()
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        
        require(url.isNotBlank()) { "Ingresa una URL publica valida." }

        val uri = runCatching { URI(url) }.getOrElse { 
            throw IllegalArgumentException("URL invalida: ${it.message}")
        }
        
        require(uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true)) {
            "Solo se permiten URLs http o https."
        }

        val host = uri.host?.lowercase().orEmpty()
        require(host.isNotBlank()) { "La URL no contiene un host valido." }
        require(!host.isBlockedHost()) { "El host no esta permitido: $host" }

        return uri
    }

    private fun String.isBlockedHost(): Boolean {
        val lower = this.lowercase()
        
        if (lower == "localhost" || lower.endsWith(".local")) return true
        if (lower == "169.254.169.254") return true
        if (lower.startsWith("127.") || lower.startsWith("10.") || 
            lower.startsWith("192.168.") || lower.startsWith("172.")) return true
        if (lower.startsWith("0.") || lower == "0.0.0.0") return true
        
        return false
    }
}
