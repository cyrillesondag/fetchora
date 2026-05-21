package org.mediadownloader.data.remote

import org.mediadownloader.data.remote.model.CobaltRequest
import org.mediadownloader.data.remote.model.CobaltResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface CobaltApi {
    @POST("/")
    suspend fun resolve(@Body request: CobaltRequest): CobaltResponse
}
