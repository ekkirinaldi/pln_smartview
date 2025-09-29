package com.pln_llm.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

data class SmartGlassResponse(
    val audio: ByteArray,
    val llm_result: String
)

interface SmartGlassApi {
    @Multipart
    @POST("api/smart/process-audio")
    suspend fun processAudio(
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part audio_file: MultipartBody.Part
    ): Response<ResponseBody>
} 