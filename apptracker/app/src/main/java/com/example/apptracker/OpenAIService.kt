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
            당신은 앱 사용 패턴 분석 전문가입니다.
            주어진 앱 이름을 아래 기준에 따라 한 가지 카테고리로 분류하세요.

            [분류 기준]
            - 공부: 인강, 독서, 학교/학습 앱
            - 정보수집: 검색, 뉴스, 지도, 날씨
            - 생산: 업무, 일정, 메모, 문서, 코딩
            - SNS: 채팅/메신저/소셜 플랫폼
            - 엔터테인먼트: 영상, 음악, 게임, 웹툰
            - 기타: 위 기준 어디에도 속하지 않으면 기타

            앱 이름: "$appName"

            출력 형식:
            - 카테고리 이름 한 단어만 출력 (예: 공부, SNS, 엔터테인먼트)
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
