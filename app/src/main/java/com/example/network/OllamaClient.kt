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
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(OllamaRequest::class.java)
    private val responseAdapter = moshi.adapter(OllamaResponse::class.java)

    fun generateStream(prompt: String): Flow<String> = flow {
        val url = "http://$ip:$port/api/generate"
        val requestBody = OllamaRequest(prompt = prompt)
        val json = requestAdapter.toJson(requestBody)

        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit("Error: ${response.code}")
                return@use
            }

            val source = response.body?.source() ?: return@use
            while (!source.exhausted()) {
                val line = source.readUtf8Line()
                if (line != null) {
                    try {
                        val parsed = responseAdapter.fromJson(line)
                        if (parsed?.response != null) {
                            emit(parsed.response)
                        }
                    } catch (e: Exception) {
                        // ignore parse errors for partial lines or invalid json
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun checkConnection(): Boolean {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val url = "http://$ip:$port/"
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
