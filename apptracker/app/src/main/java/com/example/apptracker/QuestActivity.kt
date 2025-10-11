// QuestItem을 통해 생성된 퀘스트를 데이터베이스에도 업로드

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

// 지금 퀘스트 내용이 폰 내부에만 저장되고 있음. 퀘스트 내용과 로그를 분석해서 포인트를 제공해야 하므로 DB에도 업로드
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

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

            val selectedPkg = usedApps[selectedIndex]   // ✅ 패키지명
            val appName = appNames[selectedIndex]       // ✅ 앱 이름
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
            updateQuestList()
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

    /** ✅ 실제 앱 사용 데이터 실시간 가져오기 */
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

        // Firebase에도 저장하는 로직 추가 - 수정된 부분
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.database.getReference("quests").child(uid).push().setValue(quest)
    }

    /** ✅ 실시간 사용량 업데이트 + 판정 */
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
                            usedMin > quest.targetMinutes -> {
                                // 목표치 초과 즉시 실패
                                false
                            }
                            now >= deadline -> {
                                // 마감 후 이하라면 성공
                                usedMin <= quest.targetMinutes
                            }
                            else -> false
                        }
                    }
                    "이상 사용" -> {
                        usedMin >= quest.targetMinutes
                    }
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
