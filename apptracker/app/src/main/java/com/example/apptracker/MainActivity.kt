package com.example.apptracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var tvTotalUsage: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvPoints: TextView
    private lateinit var chart: PieChart
    private lateinit var btnQuest: Button
    private var autoJob: Job? = null

    // ✅ OpenAI API 키
    private val apiKey =
       

    private val CATEGORY_SCORES = mapOf(
        "공부" to 100,
        "정보수집" to 30,
        "생산" to 50,
        "SNS" to 20,
        "엔터테인먼트" to 5,
        "기타" to 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvTotalUsage = findViewById(R.id.tv_total_usage)
        tvSummary = findViewById(R.id.tv_summary)
        tvPoints = findViewById(R.id.tv_points)
        chart = findViewById(R.id.pieChart)
        btnQuest = findViewById(R.id.btnQuest) // ✅ 버튼 ID 수정 완료

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // ✅ 퀘스트 화면 이동 (앱 사용 데이터 전달)
        btnQuest.setOnClickListener {
            val appLogs = getAppUsageSummary24h().mapValues { (it.value / 60000L).toInt() }
            val intent = Intent(this, QuestActivity::class.java)
            intent.putExtra("usageData", HashMap(appLogs))
            startActivity(intent)
        }

        if (!hasUsageAccess()) {
            tvSummary.text = "앱 사용 기록 접근 권한이 필요합니다. 설정에서 허용해주세요."
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
            return
        }
    }

    override fun onResume() {
        super.onResume()
        startAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        autoJob?.cancel()
    }

    /** ✅ 10초마다 자동 새로고침 */
    private fun startAutoRefresh() {
        autoJob?.cancel()
        autoJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                refreshUsageChart()
                delay(10_000L)
            }
        }
    }

    /** ✅ 차트 및 요약 새로고침 */
    private suspend fun refreshUsageChart() = withContext(Dispatchers.IO) {
        val appLogs = getAppUsageSummary24h()
        if (appLogs.isEmpty()) return@withContext

        val totalUsageMin = (appLogs.values.sum() / 60000L).toInt().coerceAtMost(24 * 60)
        val gptJson = callGPTCategorize(appLogs)
        val parsed = parseGptResultOrFallback(gptJson, appLogs)
        val (categoryMinutes, summaryLines) = aggregateByCategory(parsed)
        val totalScore = savePointsAndRender(categoryMinutes, summaryLines)

        withContext(Dispatchers.Main) {
            tvTotalUsage.text = "오늘 총 사용시간: ${totalUsageMin}분"
            tvSummary.text = summaryLines.joinToString("\n")
            tvPoints.text = "포인트: ${totalScore}점"
        }
    }

    /** ✅ 권한 확인 */
    private fun hasUsageAccess(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            true
        }
    }

    /** ✅ 앱 이름 가져오기 */
    private fun getAppLabel(packageName: String): String {
        return try {
            val pm: PackageManager = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /** ✅ 시스템 앱 필터 */
    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val ai: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            false
        }
    }

    /** ✅ 24시간 앱 사용 기록 수집 */
    private fun getAppUsageSummary24h(): Map<String, Long> {
        val end = System.currentTimeMillis()
        val start = end - 24L * 60 * 60 * 1000
        val events = usageStatsManager.queryEvents(start, end)
        val openTimes = hashMapOf<String, Long>()
        val usageMapMs = hashMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> openTimes[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val s = openTimes.remove(pkg)
                    if (s != null && event.timeStamp > s) {
                        val dur = event.timeStamp - s
                        usageMapMs[pkg] = (usageMapMs[pkg] ?: 0L) + max(0L, dur)
                    }
                }
            }
        }

        val now = end
        for ((pkg, s) in openTimes) {
            if (now > s) {
                usageMapMs[pkg] = (usageMapMs[pkg] ?: 0L) + (now - s)
            }
        }

        return usageMapMs
            .filter { (_, ms) -> ms >= 60_000L }
            .filterNot { (pkg, _) ->
                isSystemApp(pkg) ||
                        pkg.contains("launcher", true) ||
                        pkg.contains("home", true) ||
                        pkg.contains("systemui", true) ||
                        pkg.contains("inputmethod", true)
            }
    }

    /** ✅ GPT 카테고리 분류 */
    private suspend fun callGPTCategorize(appUsageMs: Map<String, Long>): String =
        withContext(Dispatchers.IO) {
            val items = appUsageMs.entries.joinToString("\n") { (pkg, ms) ->
                val appName = getAppLabel(pkg)
                val minutes = (ms / 60000L).toInt()
                "- package: \"$pkg\", appName: \"$appName\", minutes: $minutes"
            }

            val prompt = """
다음은 사용자의 오늘 앱 사용 요약입니다(분 단위).
각 항목을 반드시 [공부, 정보수집, 생산, SNS, 엔터테인먼트, 기타] 중 하나로 분류하세요.
출력은 반드시 JSON 배열로만.
데이터:
$items
""".trimIndent()

            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = """
            {"model": "gpt-4o-mini","messages":[{"role":"user","content":${JSONObject.quote(prompt)}}],"temperature":0.0}
            """.trimIndent().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val text = response.body?.string().orEmpty()
            response.close()

            try {
                JSONObject(text)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } catch (_: Exception) {
                "[]"
            }
        }

    /** ✅ GPT 결과 파싱 */
    private fun parseGptResultOrFallback(gptJson: String, appUsageMs: Map<String, Long>): List<CategorizedItem> {
        return try {
            val arr = JSONArray(gptJson)
            val list = mutableListOf<CategorizedItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val pkg = o.optString("package")
                val appName = o.optString("appName", getAppLabel(pkg))
                val minutes = o.optInt("minutes", ((appUsageMs[pkg] ?: 0L) / 60000L).toInt())
                val category = o.optString("category", "기타")
                list.add(CategorizedItem(pkg, appName, minutes, category))
            }
            list.ifEmpty { fallbackCategorized(appUsageMs) }
        } catch (_: Exception) {
            fallbackCategorized(appUsageMs)
        }
    }

    /** ✅ GPT 실패 시 기본 분류 */
    private fun fallbackCategorized(appUsageMs: Map<String, Long>): List<CategorizedItem> {
        return appUsageMs.map { (pkg, ms) ->
            val label = getAppLabel(pkg).lowercase()
            val category = when {
                listOf("youtube", "netflix", "tving", "watcha").any { label.contains(it) } -> "엔터테인먼트"
                listOf("chrome", "naver", "google", "safari").any { label.contains(it) } -> "정보수집"
                listOf("instagram", "kakao", "twitter", "facebook").any { label.contains(it) } -> "SNS"
                listOf("word", "excel", "slack", "notion", "github").any { label.contains(it) } -> "생산"
                listOf("zoom", "class", "study", "ridi").any { label.contains(it) } -> "공부"
                else -> "기타"
            }
            CategorizedItem(pkg, getAppLabel(pkg), (ms / 60000L).toInt(), category)
        }
    }

    data class CategorizedItem(
        val pkg: String,
        val appName: String,
        val minutes: Int,
        val category: String
    )

    /** ✅ 카테고리별 합계 및 문장 생성 */
    private fun aggregateByCategory(items: List<CategorizedItem>): Pair<Map<String, Int>, List<String>> {
        val catMinutes = linkedMapOf<String, Int>()
        val lines = mutableListOf<String>()
        items.forEach {
            catMinutes[it.category] = (catMinutes[it.category] ?: 0) + it.minutes
            lines += "[${it.category}] ${it.appName} ${it.minutes}분"
        }
        return Pair(catMinutes, lines)
    }

    /** ✅ 포인트 및 차트 표시 */
    private suspend fun savePointsAndRender(categoryMinutes: Map<String, Int>, summaryLines: List<String>): Int =
        withContext(Dispatchers.Main) {
            var totalScore = 0
            categoryMinutes.forEach { (cat, minutes) ->
                if (minutes > 0) totalScore += (CATEGORY_SCORES[cat] ?: 0)
            }

            val entries = categoryMinutes.filter { it.value > 0 }
                .map { (cat, minutes) -> PieEntry(minutes.toFloat(), cat) }

            val dataSet = PieDataSet(entries, "오늘의 앱 사용 비율")
            dataSet.colors = listOf(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#03A9F4"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#F44336"),
                Color.parseColor("#607D8B")
            )
            dataSet.valueTextColor = Color.WHITE

            chart.data = PieData(dataSet)
            chart.setEntryLabelColor(Color.WHITE)
            chart.legend.textColor = Color.WHITE
            chart.setHoleColor(Color.TRANSPARENT)
            chart.description.isEnabled = false
            chart.invalidate()

            return@withContext totalScore
        }
}
