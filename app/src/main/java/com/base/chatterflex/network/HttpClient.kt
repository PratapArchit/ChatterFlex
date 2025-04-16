package com.base.chatterflex.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object HttpClient {
    private val client = OkHttpClient()

    fun fetchMessage(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "Error: ${response.code}"
            }

            val responseBody = response.body?.string() ?: return "Empty response"
            val json = JSONObject(responseBody)
            return json.getString("message")
        }
    }
}
