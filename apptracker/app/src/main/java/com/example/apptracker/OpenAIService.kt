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

        if (totalMinutes == 0) return@withContext "폰을 거의 안 썼네? 오늘 정말 갓생 살았구나! 최고야 👍"

        val playMinutes = (meaningfulData["엔터테인먼트"] ?: 0) + (meaningfulData["SNS"] ?: 0) + (meaningfulData["게임"] ?: 0)
        val playRatio = if (totalMinutes > 0) (playMinutes.toDouble() / totalMinutes * 100).toInt() else 0
        val dataString = meaningfulData.entries.joinToString(", ") { "${it.key}: ${it.value}분" }

        val prompt = """
            사용자의 오늘 앱 사용 내역이야:
            [총 사용: ${totalMinutes}분]
            [노는 앱 비중: ${playRatio}%]
            [상세: $dataString]
            
            이걸 보고 다정한 친구처럼 한마디 해줘. (반말, 50자 이내)
            1. 절대 혼내거나 비꼬지 마.
            2. 많이 썼으면: "눈이 피곤하겠다, 조금 쉬어주는 건 어때?" 처럼 걱정해주고 격려해줘.
            3. 적게 썼으면: "오늘 하루 알차게 보냈구나! 정말 대단해" 라고 듬뿍 칭찬해줘.
        """.trimIndent()

        try { callGpt(prompt) } catch (e: Exception) { "분석 실패: ${e.message}" }
    }

    // 🔥 [핵심 수정] 퀘스트 추천 로직 강화 (최소 시간 1분 보장 및 난이도 조절 명확화)
    suspend fun recommendQuestFromHistory(history: List<QuestItem>): String = withContext(Dispatchers.IO) {
        if (history.isEmpty()) return@withContext "아직 퀘스트 기록이 없네. 자주 쓰는 앱으로 가볍게 시작해볼까? 🌱"

        val recentHistory = history.take(10).joinToString("\n") {
            "- 앱: ${it.appName}, 목표: ${it.goalMinutes}분 ${it.conditionType}, 결과: ${if(it.success) "성공" else "실패"}"
        }

        // 과거에 퀘스트 했던 앱들만 추출
        val usedQuestApps = history.map { it.appName }.distinct().joinToString(", ")

        val prompt = """
            사용자의 최근 퀘스트 기록:
            $recentHistory
            
            이전에 퀘스트를 진행했던 앱 목록: [$usedQuestApps]
            
            이 기록을 바탕으로 **다음에 도전할 퀘스트 하나**를 추천해줘.
            
            [추천 절대 규칙 - 이거 어기면 안됨]
            1. **무조건 '이전에 퀘스트를 진행했던 앱' 중에서만 골라.** (새로운 앱 금지)
            2. **방향성(이상/이하) 유지**:
               - 예전에 '이하(≤)'로 했던 앱은 이번에도 무조건 '이하(≤)'로 추천해. (절대 '이상(≥)'으로 바꾸지 마!)
               - 예전에 '이상(≥)'으로 했던 앱은 이번에도 무조건 '이상(≥)'으로 추천해.
            3. **난이도 조절**:
               - 성공했으면: 난이도를 높여. (이하(≤)는 목표 시간을 줄여야 난이도가 높아짐, 이상(≥)은 목표 시간을 늘려야 난이도가 높아짐)
               - 실패했으면: 난이도를 낮춰. (이하(≤)는 목표 시간을 늘려야 난이도가 낮아짐, 이상(≥)은 목표 시간을 줄여야 난이도가 낮아짐)
            4. **최소 시간**: 목표 시간은 **1분 이상**으로 설정해. (0분이나 초 단위는 절대로 추천 금지)
            5. **출력**: 추천 앱, 목표 시간, 조건을 포함한 다정한 코치 말투의 문장 하나만 출력해. (60자 이내, 반말)
            
            예시 출력:
            유튜브 10분 이하 성공했으니, 이번엔 5분 이하로 줄여보는 건 어때?
            
            추천을 시작해줘.
        """.trimIndent()

        try { callGpt(prompt) } catch (e: Exception) { "새로운 퀘스트에 도전해볼까?" }
    }

    private fun callGpt(prompt: String): String {
        // ... (API 호출 코드는 변경 없음)
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

        if (!response.isSuccessful) throw Exception("API Error ${response.code}")

        return JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}