package com.example.apptracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

class OpenAIService {

    private val client = OkHttpClient()
    private val apiKey = ""

    suspend fun classifyApp(appName: String): String = withContext(Dispatchers.IO) {

        val prompt = """
            아래 앱 이름을 보고 카테고리를 하나로 분류해라.
            가능한 값: [공부, SNS, 엔터테인먼트]
            앱 이름: $appName
            답변은 단어 하나만 출력.
        """.trimIndent()

        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", listOf(
                JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }
            ))
        }

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val response = client.newCall(request).execute()
        val raw = response.body?.string() ?: ""

        val content = try {
            JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } catch (e: Exception) {
            "엔터테인먼트"
        }

        return@withContext content
    }
}
