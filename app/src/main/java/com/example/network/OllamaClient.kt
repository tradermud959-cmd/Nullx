package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OllamaClient(private val ip: String, private val port: String) {
    private val client = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(NullXChatRequest::class.java)
    private val responseAdapter = moshi.adapter(NullXChatResponse::class.java)

    fun generateStream(prompt: String): Flow<String> = flow {
        val url = "http://$ip:$port/chat"
        val requestBody = NullXChatRequest(message = prompt)
        val json = requestAdapter.toJson(requestBody)

        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit("Error: ${response.code}")
                    return@use
                }

                // If the response is streaming, it might be JSON lines.
                // But if it's just a regular JSON response from FastAPI:
                val bodyString = response.body?.string() ?: ""
                try {
                    val parsed = responseAdapter.fromJson(bodyString)
                    if (parsed?.reply != null) {
                        emit(parsed.reply)
                    } else {
                        emit(bodyString)
                    }
                } catch (e: Exception) {
                    // Try parsing as lines if it failed (fallback for streaming)
                    bodyString.lines().forEach { line ->
                        try {
                            val parsedLine = responseAdapter.fromJson(line)
                            if (parsedLine?.reply != null) {
                                emit(parsedLine.reply)
                            }
                        } catch (e2: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun checkConnection(): Boolean {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val url = "http://$ip:$port/health"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: IOException) {
                false
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}
