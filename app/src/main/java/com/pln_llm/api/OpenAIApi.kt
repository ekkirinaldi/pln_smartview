package com.pln_llm.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface OpenAIApi {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") responseFormat: RequestBody = RequestBody.create(null, "json"),
        @Part("language") language: RequestBody = RequestBody.create(null, "id")
    ): Response<TranscriptionResponse>

    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @POST("v1/audio/speech")
    @Streaming
    suspend fun textToSpeech(
        @Body request: TextToSpeechRequest
    ): Response<ResponseBody>
}

data class TranscriptionResponse(
    val text: String
)

data class ChatRequest(
    val model: String = "gpt-4",
    val messages: List<Message>,
    val temperature: Double = 0.7  // Add some creativity while keeping responses focused
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

data class TextToSpeechRequest(
    val model: String = "tts-1",
    val input: String,
    val voice: String = "alloy"
) 