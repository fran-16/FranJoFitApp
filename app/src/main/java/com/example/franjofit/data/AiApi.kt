package com.example.franjofit.data

import retrofit2.http.Body
import retrofit2.http.POST

interface AiApi {

    @POST("ai/suggestions")
    suspend fun getSuggestions(
        @Body body: SuggestionRequestDto
    ): SuggestionResponseDto

    @POST("ai/chat")
    suspend fun chat(
        @Body body: ChatRequestDto
    ): ChatResponseDto
}
