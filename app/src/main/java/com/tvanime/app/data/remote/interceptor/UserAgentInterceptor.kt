package com.tvanime.app.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega un User-Agent válido en cada petición.
 * Muchos servidores rechazan peticiones con User-Agent por defecto.
 */
class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; TV)")
            .build()
        return chain.proceed(req)
    }
}
