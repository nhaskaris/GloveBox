package com.eliteonetube.glovebox.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VinDecoderApi {
    @GET("vehicles/decodevin/{vin}")
    suspend fun decodeVin(
        @Path("vin") vin: String,
        @Query("format") format: String = "json"
    ): VinResponse
}

@JsonClass(generateAdapter = true)
data class VinResponse(
    @Json(name = "Count") val Count: Int,
    @Json(name = "Message") val Message: String,
    @Json(name = "Results") val Results: List<VinResult>
)

@JsonClass(generateAdapter = true)
data class VinResult(
    @Json(name = "Value") val Value: String?,
    @Json(name = "ValueId") val ValueId: String?,
    @Json(name = "Variable") val Variable: String,
    @Json(name = "VariableId") val VariableId: Int
)
