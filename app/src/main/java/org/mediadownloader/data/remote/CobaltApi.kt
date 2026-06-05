package org.mediadownloader.data.remote

import org.mediadownloader.data.remote.model.CobaltInfoResponse
import org.mediadownloader.data.remote.model.CobaltRequest
import org.mediadownloader.data.remote.model.CobaltResolveResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface CobaltApi {
    @POST("/")
    @Headers( "Accept: application/json",
        "Content-Type: application/json")
    suspend fun resolve(@Body request: CobaltRequest): CobaltResolveResponse


    @GET
    @Headers("Accept: application/json")
    suspend fun info(@Url cobaltUrl: String): CobaltInfoResponse
}
