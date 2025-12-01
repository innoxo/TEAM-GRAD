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

    // 🔥 [수정 1] 총 사용량 기준 분석 & 절대 시간 고려
    suspend fun generateDailySummary(categoryMinutes: Map<String, Int>): String = withContext(Dispatchers.IO) {
        val meaningfulData = categoryMinutes.filterKeys { it != "시스템" && it != "기타" && it != "설정" }
        val playMinutes = (meaningfulData["엔터테인먼트"] ?: 0) + (meaningfulData["SNS"] ?: 0) + (meaningfulData["게임"] ?: 0)

        // 시스템 포함한 전체 시간 계산 (비율 왜곡 방지)
        val totalRealTime = categoryMinutes.values.sum()

        // 딴짓 비율 (시스템 포함 전체 시간 대비)
        val playRatio = if(totalRealTime > 0) (playMinutes.toDouble() / totalRealTime * 100).toInt() else 0

        val dataString = meaningfulData.entries.joinToString(", ") { "${it.key}: ${it.value}분" }

        val prompt = """
            사용자의 오늘 앱 사용 내역:
            - 총 사용 시간(시스템 포함): ${totalRealTime}분
            - 노는 앱(엔터/SNS/게임) 사용: ${playMinutes}분
            - 상세 내역: [$dataString]
            
            친구처럼 반말로 한마디 해줘 (50자 이내).
            
            [판단 기준 - 중요!]
            1. **총 사용 시간이 2시간(120분) 미만이면**: 비율이 높든 말든 무조건 "오늘 폰 별로 안 썼네? 갓생 살았구나! 👍" 라고 칭찬해. (이게 제일 중요)
            2. 총 사용 시간이 3시간을 넘는데 노는 비율이 50% 이상이면: 그때만 "너무 많이 놀았다"고 걱정해줘.
            3. 엉뚱한 소리 하지 말고 데이터에 근거해서 말해.
        """.trimIndent()

        try {
            callGpt(prompt)
        } catch (e: Exception) {
            "분석 실패: ${e.message}"
        }
    }

    // 🔥 [수정 2] 설치된(사용된) 앱 목록을 받아서 그 안에서만 추천
    suspend fun recommendQuestFromHistory(
        history: List<QuestItem>,
        installedAppNames: List<String> // 👈 추가됨: 내가 가진 앱 목록
    ): String = withContext(Dispatchers.IO) {

        // 내가 가진 앱 목록을 문자열로 변환
        val myAppsString = installedAppNames.joinToString(", ")

        val recentHistory = if (history.isEmpty()) "기록 없음" else history.take(5).joinToString("\n") {
            "- 앱: ${it.appName}, 결과: ${if(it.success) "성공" else "실패"}"
        }

        val prompt = """
            사용자가 현재 가지고 있는 앱 목록: [$myAppsString]
            사용자의 최근 퀘스트 기록:
            $recentHistory
            
            이 정보를 바탕으로 **다음에 도전할 퀘스트 하나를 추천**해줘.
            
            [절대 규칙]
            1. **반드시 '사용자가 가지고 있는 앱 목록'에 있는 앱 중에서만 골라야 해.** (없는 앱 추천하면 죽어!)
            2. 넷플릭스, 유튜브 같은 딴짓 앱은 '시간 줄이기(이하)', 공부 앱은 '시간 늘리기(이상)' 추천.
            3. 말투는 부드러운 반말. 60자 이내.
            4. 형식: "[앱이름]으로 [00분] [이하/이상] 도전 어때?"
        """.trimIndent()

        try {
            callGpt(prompt)
        } catch (e: Exception) {
            "유튜브 30분 줄이기 퀘스트 어때?"
        }
    }

    private fun callGpt(prompt: String): String {
        val json = JSONObject()
        json.put("model", "gpt-3.5-turbo")

        val messagesArray = JSONArray()
        val messageObject = JSONObject()
        messageObject.put("role", "user")
        messageObject.put("content", prompt)
        messagesArray.put(messageObject)
        json.put("messages", messagesArray)

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) throw Exception("API 오류")

        return JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}