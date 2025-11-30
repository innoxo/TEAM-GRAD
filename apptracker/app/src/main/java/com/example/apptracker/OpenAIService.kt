package com.example.apptracker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAIService(private val context: Context) {

    private val client = OkHttpClient()
    
    // 🔥 주의: 실제 배포 시에는 API 키를 안전하게 관리해야 합니다.
    private val apiKey = "" 

    // ----------------------------------------------------------
    // 1. 앱 라벨 가져오기
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
    // 2. 앱 카테고리 분류 (재시도 로직 적용)
    // ----------------------------------------------------------
    suspend fun classifyApp(packageName: String): String = withContext(Dispatchers.IO) {
        val appLabel = getAppLabel(packageName)

        // 1) 사전 필터 적용 (비용 절감 및 속도 향상)
        preCategory(appLabel, packageName)?.let {
            return@withContext it
        }

        // 2) 시스템 프롬프트
        val systemPrompt = """
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
            
            출력은 오직 카테고리 명사 하나만 하세요. (예: 엔터테인먼트)
        """.trimIndent()

        val userPrompt = "앱 이름: \"$appLabel\", 패키지명: \"$packageName\""

        // 3) 공통 API 호출
        return@withContext callGPT(systemPrompt, userPrompt, isJsonMode = false)
    }

    // ----------------------------------------------------------
    // 3. 하루 한 줄 요약 기능
    // ----------------------------------------------------------
    suspend fun getDailySummary(usageMap: Map<String, Int>): String = withContext(Dispatchers.IO) {
        if (usageMap.isEmpty()) return@withContext "오늘은 스마트폰 사용 기록이 거의 없네요. 훌륭합니다!"

        // 데이터 포맷팅
        val sortedData = usageMap.entries.sortedByDescending { it.value }.take(5)
        val dataString = sortedData.joinToString(", ") { "${it.key}: ${it.value}분" }

        val systemPrompt = """
            당신은 사용자의 디지털 웰빙을 돕는 친근한 AI 비서입니다.
            제공된 앱 사용 시간을 분석하여, 오늘 하루 사용자의 스마트폰 사용 패턴을 '한 문장'으로 요약해 주세요.
            
            [작성 가이드]
            - 가장 많이 사용한 카테고리나 앱을 언급하며 패턴을 분석하세요.
            - 말투는 친구에게 말하듯 부드럽고 격려하는 어조(해요체)를 사용하세요.
            - 부정적인 비난보다는 긍정적인 피드백이나 가벼운 조언을 포함하세요.
        """.trimIndent()

        val userPrompt = "오늘의 앱 사용 기록: $dataString"

        return@withContext callGPT(systemPrompt, userPrompt, isJsonMode = false)
    }

    // ----------------------------------------------------------
    // [공통 내부 함수] GPT API 호출 (재시도 및 예외 처리 반영)
    // ----------------------------------------------------------
    private suspend fun callGPT(systemPrompt: String, userPrompt: String, isJsonMode: Boolean): String {
        val maxRetries = 3
        var lastError: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                val jsonBody = JSONObject().apply {
                    put("model", "gpt-4o-mini")
                    put("messages", JSONArray().apply {
                        put(JSONObject().put("role", "system").put("content", systemPrompt))
                        put(JSONObject().put("role", "user").put("content", userPrompt))
                    })
                    put("temperature", 0.3)
                    if (isJsonMode) {
                        put("response_format", JSONObject().put("type", "json_object"))
                    }
                }

                val body = RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    jsonBody.toString()
                )

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .post(body)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()

                val response = client.newCall(request).execute()
                val raw = response.body?.string() ?: ""
                response.close()

                if (response.isSuccessful) {
                    return JSONObject(raw)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                } else {
                    Log.e("OpenAIService", "API Error: ${response.code} - $raw")
                    throw Exception("HTTP ${response.code}")
                }

            } catch (e: Exception) {
                lastError = e
                Log.w("OpenAIService", "시도 $attempt 실패: ${e.message}")
                
                if (attempt < maxRetries) {
                    delay(1000L * attempt) 
                }
            }
        }
        
        Log.e("OpenAIService", "최종 실패: ${lastError?.message}")
        return "정보를 불러올 수 없습니다."
    }

    // ----------------------------------------------------------
    // [Helper] 사전 분류 로직
    // ----------------------------------------------------------
    private fun preCategory(appLabel: String, packageName: String): String? {
        val name = appLabel.lowercase()
        val pkg = packageName.lowercase()

        // 리스트 통합함.
        if ("youtube" in name || "netflix" in name || "tiktok" in name) return "엔터테인먼트"
        if ("instagram" in name || "kakao" in name || "facebook" in name) return "SNS"
        if ("chrome" in name || "naver" in name || "map" in name) return "정보수집"
        if ("gmail" in name || "notion" in name) return "생산"
        if ("setting" in name || "설정" in name || "system" in name) return "시스템"
        if (pkg.startsWith("com.android.") || pkg.startsWith("com.google.android.gms")) return "시스템"

        return null
    }
}