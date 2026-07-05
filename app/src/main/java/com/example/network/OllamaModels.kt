package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaRequest(
    val model: String = "llama3",
    val prompt: String,
    val stream: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OllamaResponse(
    val model: String?,
    val response: String?,
    val done: Boolean?
)
