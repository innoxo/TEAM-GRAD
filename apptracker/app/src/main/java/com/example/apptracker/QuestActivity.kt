package com.example.apptracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class QuestActivity : AppCompatActivity() {

    private lateinit var questContainer: LinearLayout
    private val prefs by lazy { getSharedPreferences("AppTracker", Context.MODE_PRIVATE) }

    private var usageData: HashMap<String, Int>? = null
    private var autoJob: Job? = null
    private lateinit var spinnerAppList: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest)

        questContainer = findViewById(R.id.questContainer)
        spinnerAppList = findViewById(R.id.spinnerAppList)

        val etTargetMinutes = findViewById<EditText>(R.id.etTargetMinutes)
        val rgGoalType = findViewById<RadioGroup>(R.id.rgGoalType)
        val etDeadlineDate = findViewById<EditText>(R.id.etDeadlineDate)
        val etDeadlineTime = findViewById<EditText>(R.id.etDeadlineTime)
        val btnSave = findViewById<Button>(R.id.btnSaveQuest)
        val btnBack = findViewById<Button>(R.id.btn_back)

        // 🔙 뒤로가기 버튼
        btnBack.setOnClickListener { finish() }

        @Suppress("UNCHECKED_CAST")
        usageData = intent.getSerializableExtra("usageData") as? HashMap<String, Int>

        // ✅ 오늘 실제 사용된 앱 목록 가져오기
        val usedApps = getRealtimeUsageData().keys.toList()
        val appNames = usedApps.map { getAppLabel(it) }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, appNames)
        spinnerAppList.adapter = adapter

        btnSave.setOnClickListener {
            val selectedIndex = spinnerAppList.selectedItemPosition
            if (selectedIndex == -1) {
                Toast.makeText(this, "앱을 선택하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedPkg = usedApps[selectedIndex]
            val appName = appNames[selectedIndex]
            val targetMinutes = etTargetMinutes.text.toString().toIntOrNull() ?: 0
            val goalType =
                if (rgGoalType.checkedRadioButtonId == R.id.rbBelow) "이하 사용" else "이상 사용"
            val deadlineDate = etDeadlineDate.text.toString().ifEmpty {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            val deadlineTime = etDeadlineTime.text.toString().ifEmpty { "23:59" }

            if (targetMinutes <= 0) {
                Toast.makeText(this, "목표 시간을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quest = QuestItem(appName, selectedPkg, targetMinutes, goalType, deadlineDate, deadlineTime)
            saveQuest(quest)
            Toast.makeText(this, "'$appName' 퀘스트가 저장되었습니다!", Toast.LENGTH_SHORT).show()

            // ✅ 강제 UI 갱신 (저장 직후 반영)
            CoroutineScope(Dispatchers.Main).launch {
                delay(100L)
                updateQuestList()
            }
        }

        updateQuestList()
    }

    override fun onResume() {
        super.onResume()
        startAutoUpdater()
    }

    override fun onPause() {
        super.onPause()
        autoJob?.cancel()
    }

    /** ✅ 1분마다 자동 갱신 */
    private fun startAutoUpdater() {
        autoJob?.cancel()
        autoJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateQuestList()
                delay(60_000L)
            }
        }
    }

    /** ✅ 실시간 앱 사용 데이터 가져오기 */
    private fun getRealtimeUsageData(): Map<String, Int> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
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
                    val startT = openTimes.remove(pkg)
                    if (startT != null && event.timeStamp > startT) {
                        val dur = event.timeStamp - startT
                        usageMapMs[pkg] = (usageMapMs[pkg] ?: 0L) + dur
                    }
                }
            }
        }

        val now = end
        for ((pkg, s) in openTimes) {
            usageMapMs[pkg] = (usageMapMs[pkg] ?: 0L) + (now - s)
        }

        return usageMapMs.mapValues { (it.value / 60000L).toInt() }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /** ✅ 퀘스트 저장/로드 */
    private fun loadQuests(): MutableList<QuestItem> {
        val json = prefs.getString("quests_json", "[]") ?: "[]"
        val list = mutableListOf<QuestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    QuestItem(
                        o.getString("appName"),
                        o.getString("packageName"),
                        o.getInt("targetMinutes"),
                        o.getString("goalType"),
                        o.getString("deadlineDate"),
                        o.getString("deadlineTime"),
                        o.optInt("currentMinutes", 0),
                        o.optBoolean("completed", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveQuest(quest: QuestItem) {
        val list = loadQuests().apply {
            removeAll { it.packageName.equals(quest.packageName, true) }
            add(quest)
        }
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("appName", it.appName)
            o.put("packageName", it.packageName)
            o.put("targetMinutes", it.targetMinutes)
            o.put("goalType", it.goalType)
            o.put("deadlineDate", it.deadlineDate)
            o.put("deadlineTime", it.deadlineTime)
            o.put("currentMinutes", it.currentMinutes)
            o.put("completed", it.completed)
            arr.put(o)
        }
        prefs.edit().putString("quests_json", arr.toString()).apply()
    }

    /** ✅ 퀘스트 상태 업데이트 및 표시 */
    private fun updateQuestList() {
        val quests = loadQuests()
        questContainer.removeAllViews()
        if (quests.isEmpty()) {
            val tv = TextView(this)
            tv.text = "저장된 퀘스트가 없습니다."
            tv.setTextColor(Color.WHITE)
            questContainer.addView(tv)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val usageNow = getRealtimeUsageData()
            var updated = false

            quests.forEach { quest ->
                val usedMin = usageNow[quest.packageName] ?: 0
                quest.currentMinutes = usedMin

                val now = System.currentTimeMillis()
                val deadline = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .parse("${quest.deadlineDate} ${quest.deadlineTime}")?.time ?: now

                val prev = quest.completed
                quest.completed = when (quest.goalType) {
                    "이하 사용" -> {
                        when {
                            usedMin > quest.targetMinutes -> false
                            now >= deadline -> usedMin <= quest.targetMinutes
                            else -> false
                        }
                    }
                    "이상 사용" -> usedMin >= quest.targetMinutes
                    else -> false
                }
                if (quest.completed != prev) updated = true
            }

            withContext(Dispatchers.Main) {
                questContainer.removeAllViews()
                quests.forEach { quest ->
                    val tv = TextView(this@QuestActivity)
                    val percent =
                        if (quest.targetMinutes == 0) 0 else (quest.currentMinutes * 100 / quest.targetMinutes).coerceAtMost(100)
                    val deadlineTime =
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .parse("${quest.deadlineDate} ${quest.deadlineTime}")?.time ?: 0

                    val statusText = when {
                        quest.completed -> "✅ 완료됨"
                        quest.goalType == "이하 사용" && quest.currentMinutes > quest.targetMinutes -> "❌ 실패"
                        System.currentTimeMillis() > deadlineTime -> "❌ 실패"
                        else -> "⏳ 진행중"
                    }

                    tv.text = buildString {
                        append("📱 ${quest.appName} (${quest.goalType} ${quest.targetMinutes}분)\n")
                        append("🔥 사용 ${quest.currentMinutes}분 (${percent}%)\n")
                        append("⏰ ${quest.deadlineDate} ${quest.deadlineTime}\n")
                        append(statusText)
                    }
                    tv.setTextColor(Color.WHITE)
                    tv.setBackgroundColor(
                        when {
                            quest.completed -> Color.parseColor("#004D40")
                            quest.goalType == "이하 사용" && quest.currentMinutes > quest.targetMinutes ->
                                Color.parseColor("#5C0000")
                            System.currentTimeMillis() > deadlineTime ->
                                Color.parseColor("#5C0000")
                            else -> Color.parseColor("#263238")
                        }
                    )
                    tv.setPadding(20, 20, 20, 20)
                    tv.textSize = 16f
                    questContainer.addView(tv)
                }

                if (updated) {
                    quests.forEach { saveQuest(it) }
                }
            }
        }
    }
}
