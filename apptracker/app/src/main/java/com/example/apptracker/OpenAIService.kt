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
    // 🔥 주의: 실제 배포 시에는 API 키를 안전하게 관리해야 합니다.
    private val apiKey = ""

    // 1. 앱 라벨 가져오기
    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    // 2. 사전 분류
    private fun preCategory(appLabel: String, packageName: String): String? {
        val name = appLabel.lowercase()
        val pkg = packageName.lowercase()

        if ("youtube" in name) return "엔터테인먼트"
        if ("netflix" in name) return "엔터테인먼트"
        if ("tiktok" in name) return "엔터테인먼트"
        if ("instagram" in name) return "SNS"
        if ("kakao" in name) return "SNS"
        if ("chrome" in name) return "정보수집"
        if ("naver" in name) return "정보수집"
        if ("map" in name) return "정보수집"
        if ("gmail" in name) return "생산"
        if ("notion" in name) return "생산"
        if ("setting" in name) return "시스템"
        if ("설정" in name) return "시스템"
        if ("system" in name && !"youtube".contains(name)) return "시스템"
        if (pkg.startsWith("com.android.")) return "시스템"
        if (pkg.startsWith("com.google.android.gms")) return "시스템"

        return null
    }

    // 3. 앱 카테고리 분류 (기존 기능)
    suspend fun classifyApp(packageName: String): String = withContext(Dispatchers.IO) {
        val appLabel = getAppLabel(packageName)
        preCategory(appLabel, packageName)?.let { return@withContext it }

        val prompt = """
            앱 이름: "$appLabel"
            패키지명: "$packageName"
            
            이 앱을 [공부, 정보수집, 생산, SNS, 엔터테인먼트, 시스템, 기타] 중 하나로 분류해.
            반드시 아래 형식으로만 출력:
            category: [카테고리명]
        """.trimIndent()

        callGpt(prompt).replace("category:", "").replace("[", "").replace("]", "").trim()
    }

    // 🔥 [추가된 기능] 오늘의 사용 패턴 한 줄 요약
    suspend fun generateDailySummary(categoryMinutes: Map<String, Int>): String = withContext(Dispatchers.IO) {
        if (categoryMinutes.isEmpty()) return@withContext "아직 사용 기록이 없네요! 폰을 조금 더 써보세요."

        // 데이터 문자열로 변환 (예: 엔터테인먼트: 120분, 공부: 10분)
        val dataString = categoryMinutes.entries.joinToString(", ") { "${it.key}: ${it.value}분" }

        val prompt = """
            사용자의 오늘 스마트폰 앱 사용 내역이야:
            [$dataString]
            
            이 사용자를 위해 '팩트 폭격' 또는 '따뜻한 조언'을 한 문장으로 해줘.
            - 엔터테인먼트/SNS가 많으면: 약간 비꼬거나 정신 차리라는 조언 (유머러스하게)
            - 공부/생산이 많으면: 칭찬
            - 반말 모드로 친근하게.
            - 길이는 50자 이내.
            
            예시: "유튜브만 3시간이라니... 눈 안 아파? 공부 좀 하자!"
        """.trimIndent()

        try {
            callGpt(prompt)
        } catch (e: Exception) {
            "오늘도 알찬 하루 보내세요!"
        }
    }

    // GPT 호출 공통 함수
    private fun callGpt(prompt: String): String {
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

        return JSONObject(raw)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}