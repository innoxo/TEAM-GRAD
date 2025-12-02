package com.example.apptracker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = ""
    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun preCategory(appLabel: String, packageName: String): String? {
        val name = appLabel.lowercase()
        if ("youtube" in name || "netflix" in name || "tiktok" in name) return "엔터테인먼트"
        if ("instagram" in name || "kakao" in name || "twitter" in name) return "SNS"
        if ("chrome" in name || "naver" in name || "browser" in name) return "정보수집"
        if ("game" in name || "com.nexon" in packageName || "com.supercell" in packageName) return "게임"
        return null
    }

    suspend fun classifyApp(packageName: String): String = withContext(Dispatchers.IO) {
        val appLabel = getAppLabel(packageName)
        preCategory(appLabel, packageName)?.let { return@withContext it }

        val prompt = "앱 이름: $appLabel\n패키지: $packageName\n이 앱을 [공부, 정보수집, 생산, SNS, 엔터테인먼트, 게임, 시스템, 기타] 중 하나로 분류해. 출력형식: category: [카테고리명]"
        try {
            callGpt(prompt).replace("category:", "").replace("[", "").replace("]", "").trim()
        } catch (e: Exception) {
            "기타"
        }
    }

    suspend fun generateDailySummary(categoryMinutes: Map<String, Int>): String = withContext(Dispatchers.IO) {
        val meaningfulData = categoryMinutes.filterKeys { it != "시스템" && it != "기타" && it != "설정" }
        val totalMinutes = meaningfulData.values.sum()
        val playMinutes = (meaningfulData["엔터테인먼트"] ?: 0) + (meaningfulData["SNS"] ?: 0) + (meaningfulData["게임"] ?: 0)
        val playRatio = if (totalMinutes > 0) (playMinutes.toDouble() / totalMinutes * 100).toInt() else 0
        val dataString = meaningfulData.entries.joinToString(", ") { "${it.key}: ${it.value}분" }

        val prompt = """
            사용자의 오늘 앱 사용 내역이야:
            [총 사용: ${totalMinutes}분]
            [노는 앱 비중: ${playRatio}%]
            [상세: $dataString]
            
            이걸 보고 다정한 친구처럼 한마디 해줘. (반말, 50자 이내)
            1. 노는 비중 30% 미만: "오늘 정말 알차게 보냈네! 멋져 👍"
            2. 노는 비중 30%~50%: "적당히 잘 쉬었네! 이제 슬슬 집중해볼까?"
            3. 노는 비중 50% 이상: "오늘 좀 많이 놀았는데? 눈 건강 생각해서 조금만 줄이자!"
        """.trimIndent()

        try {
            callGpt(prompt)
        } catch (e: Exception) {
            "분석 실패: ${e.message}"
        }
    }

    // 🔥 [핵심 수정] 과거 기록(History)을 분석해서 맞춤형 추천을 해주는 로직
    suspend fun recommendQuestFromHistory(
        history: List<QuestItem>,
        installedAppNames: List<String>
    ): String = withContext(Dispatchers.IO) {

        // 1. 기록이 아예 없으면 기본 추천
        if (history.isEmpty()) {
            return@withContext "아직 퀘스트 기록이 없네. 자주 쓰는 앱으로 '30분 줄이기'부터 시작해보는 건 어때?"
        }

        // 2. 과거 기록을 문자열로 요약 (최근 5개)
        // 예: "- 유튜브: 30분 이하 (실패), - 인스타: 20분 이하 (성공)"
        val historySummary = history.take(5).joinToString("\n") {
            "- ${it.appName}: ${it.goalMinutes}분 ${if(it.conditionType == "≤") "줄이기" else "채우기"} -> 결과: ${if(it.success) "성공" else "실패"}"
        }

        val myAppsString = installedAppNames.take(20).joinToString(", ")

        val prompt = """
            사용자의 최근 퀘스트 기록이야:
            $historySummary
            
            사용자가 가진 앱 목록:
            [$myAppsString]
            
            이 기록을 분석해서 **다음에 도전할 딱 하나의 퀘스트**를 추천해줘.
            
            [추천 논리 - 매우 중요]
            1. **실패한 기록이 있다면**: "지난번에 [앱이름] 퀘스트 실패했네? 이번엔 목표를 조금 더 쉽게 잡아서 다시 도전해보자!" (예: 시간을 늘려주거나 줄여주기)
            2. **성공한 기록이 있다면**: "오, [앱이름] 퀘스트 성공했네! 이번엔 난이도를 조금 높여볼까?"
            3. 기록이 다양하면, 가장 많이 실패한 앱을 골라서 재도전을 권유해줘.
            4. 반드시 사용자가 가진 앱 목록에 있는 앱이어야 해.
            5. 말투: 다정한 코치처럼 반말. (60자 이내)
        """.trimIndent()

        try {
            callGpt(prompt)
        } catch (e: Exception) {
            "새로운 퀘스트에 도전해볼까?"
        }
    }

    private fun callGpt(prompt: String): String {
        val json = JSONObject()
        json.put("model", "gpt-3.5-turbo")
        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })
        json.put("messages", messagesArray)

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) throw Exception("API Error")

        return JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}