package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NullXChatRequest(
    val message: String
)

@JsonClass(generateAdapter = true)
data class NullXChatResponse(
    val reply: String
)
