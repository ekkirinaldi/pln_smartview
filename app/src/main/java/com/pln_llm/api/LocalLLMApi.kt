package com.pln_llm.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LocalLLMApi {
    @POST("query")
    suspend fun getCompletion(
        @Body request: LocalLLMRequest
    ): Response<LocalLLMResponse>
}

data class LocalLLMRequest(
    val content: String
)

data class LocalLLMResponse(
    val answer: String,
    val relevant_contexts: List<String>
) 