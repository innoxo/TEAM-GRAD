package com.example.apptracker

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

class OpenAIService(private val context: Context) {

    private val client = OkHttpClient()
    private val apiKey =
        ""
    // ----------------------------------------------------------
    // 앱 라벨 가져오기
    // ----------------------------------------------------------
    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    // ----------------------------------------------------------
    // 사전 분류 — 애매한 GPT 분류 방지 (필수 최소 기준만 포함)
    // ----------------------------------------------------------
    private fun preCategory(appLabel: String, packageName: String): String? {
        val name = appLabel.lowercase()
        val pkg = packageName.lowercase()

        // 엔터테인먼트 (확실한 경우만)
        if ("youtube" in name) return "엔터테인먼트"
        if ("netflix" in name) return "엔터테인먼트"
        if ("tiktok" in name) return "엔터테인먼트"

        // SNS
        if ("instagram" in name) return "SNS"
        if ("kakao" in name) return "SNS"

        // 정보수집
        if ("chrome" in name) return "정보수집"
        if ("naver" in name) return "정보수집"
        if ("map" in name) return "정보수집"

        // 생산
        if ("gmail" in name) return "생산"
        if ("notion" in name) return "생산"

        // 시스템 — 진짜 시스템만
        if ("setting" in name) return "시스템"
        if ("설정" in name) return "시스템"
        if ("system" in name && !"youtube".contains(name)) return "시스템"
        if (pkg.startsWith("com.android.")) return "시스템"
        if (pkg.startsWith("com.google.android.gms")) return "시스템"

        return null
    }

    // ----------------------------------------------------------
    // GPT 분류
    // ----------------------------------------------------------
    suspend fun classifyApp(packageName: String): String = withContext(Dispatchers.IO) {

        val appLabel = getAppLabel(packageName)

        // 1) 사전 필터 적용
        preCategory(appLabel, packageName)?.let {
            return@withContext it
        }

        // 2) GPT 프롬프트 (네가 원하는 원형 유지 + 최소 기능 보강)
        val prompt = """
            당신은 앱 카테고리 분류 전문가입니다.
            주어진 앱 이름을 아래 기준 중 하나로 정확하게 분류하세요.

            [카테고리 기준]
            - 공부: 인강, 독서, 학교/학습 앱
            - 정보수집: 검색, 뉴스, 지도, 날씨
            - 생산: 업무, 일정, 메모, 문서, 코딩
            - SNS: 채팅, 메신저, 커뮤니티, 소셜 플랫폼
            - 엔터테인먼트: 영상, 음악, 게임, 웹툰
            - 시스템: 설정, Google Play 서비스, OS 기능
            - 기타: 위 기준 어디에도 속하지 않으면 기타로 분류

            참고: 앱 이름이 모호한 경우, 일반적인 사용자 환경에서의 주 사용 목적을 기준으로 판단하세요.

            앱 이름: "$appLabel"
            패키지명: "$packageName"

            반드시 아래 형식으로만 출력:
            category: [카테고리명]
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

        val gptContent = try {
            JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } catch (e: Exception) {
            "category: [기타]"
        }

        val category = gptContent
            .replace("category:", "")
            .replace("[", "")
            .replace("]", "")
            .trim()

        return@withContext category
    }
}
