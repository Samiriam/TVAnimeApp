package com.tvanime.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * API genérica para fuentes externas.
 *
 * Configura la base URL y endpoints según tu fuente.
 * Cada fuente tendrá su propia interfaz separada.
 */
interface SourceApi {

    @GET
    suspend fun fetchCatalog(@Url url: String): String

    @GET
    suspend fun fetchStreamUrl(@Url url: String): String
}
