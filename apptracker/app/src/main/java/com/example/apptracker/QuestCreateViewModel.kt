package com.example.apptracker

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apptracker.ai.AppClusteringEngine
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*

class QuestCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val pm = application.packageManager
    private val repo = QuestRepository()
    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    private val _appList = MutableStateFlow<List<App>>(emptyList())
    val appList = _appList.asStateFlow()

    private val _recommendedApps = MutableStateFlow<List<App>>(emptyList())
    val recommendedApps = _recommendedApps.asStateFlow()

    private val _selectedApp = MutableStateFlow<App?>(null)
    val selectedApp = _selectedApp.asStateFlow()

    private val _conditionType = MutableStateFlow("≤")
    val conditionType = _conditionType.asStateFlow()

    private val _targetMinutes = MutableStateFlow(10)
    val targetMinutes = _targetMinutes.asStateFlow()

    private val _startHour = MutableStateFlow(9)
    val startHour = _startHour.asStateFlow()
    private val _startMinute = MutableStateFlow(0)
    val startMinute = _startMinute.asStateFlow()

    private val _endHour = MutableStateFlow(18)
    val endHour = _endHour.asStateFlow()
    private val _endMinute = MutableStateFlow(0)
    val endMinute = _endMinute.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            // 오늘 사용량 조회 (00:00 ~ 현재)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis())

            val usageMap = mutableMapOf<String, Long>()
            stats?.forEach { if(it.totalTimeInForeground > 0) usageMap[it.packageName] = it.totalTimeInForeground }
            
            // 오늘 실제로 쓴 앱 개수
            val activeCount = usageMap.size

            val apps = withContext(Dispatchers.IO) {
                val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val myPackage = context.packageName

                val rawList = allApps.filter {
                    pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != myPackage
                }.map {
                    App(pm.getApplicationLabel(it).toString(), it.packageName)
                }

                // 4개 이상이면 사용량 순, 아니면 가나다 순 정렬함.
                if (activeCount >= 4) {
                    rawList.sortedWith(compareByDescending<App> { usageMap[it.packageName] ?: 0L }.thenBy { it.appName })
                } else {
                    rawList.sortedBy { it.appName }
                }
            }
            _appList.value = apps
            
            // AI 추천 로직
            loadRecommendations(apps)
        }
    }

    private suspend fun loadRecommendations(allApps: List<App>) {
        val history = repo.loadAllQuests()
        val recommendations = withContext(Dispatchers.Default) {
            AppClusteringEngine.getRecommendedApps(allApps, history)
        }
        _recommendedApps.value = recommendations
    }

    fun selectApp(app: App) { _selectedApp.value = app }
    fun setCondition(c: String) { _conditionType.value = c }
    fun setTargetMinutes(v: Int) { _targetMinutes.value = v }
    fun setStartHour(v: Int) { 
        val current = Calendar.getInstance()
        val currentHour = current.get(Calendar.HOUR_OF_DAY)
        if (v < currentHour) _startHour.value = currentHour else _startHour.value = v
    }
    fun setStartMinute(v: Int) { _startMinute.value = v }
    fun setEndHour(v: Int) { _endHour.value = v }
    fun setEndMinute(v: Int) { _endMinute.value = v }

    fun createQuest(onSuccess: () -> Unit) {
        if (_isLoading.value) return
        val app = selectedApp.value ?: return

        _isLoading.value = true
        val finalMinutes = if (targetMinutes.value <= 0) 10 else targetMinutes.value

        val now = Calendar.getInstance().apply { set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val startCal = now.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, startHour.value)
        startCal.set(Calendar.MINUTE, startMinute.value)
        
        if (startCal.timeInMillis < System.currentTimeMillis() - 60000) {
            startCal.timeInMillis = System.currentTimeMillis()
        }

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, endHour.value)
        endCal.set(Calendar.MINUTE, endMinute.value)
        if (endCal.timeInMillis <= startCal.timeInMillis) endCal.add(Calendar.DAY_OF_MONTH, 1)

        val quest = QuestItem(
            id = System.currentTimeMillis().toString(),
            targetPackage = app.packageName,
            appName = app.appName,
            conditionType = conditionType.value,
            goalMinutes = finalMinutes,
            startTime = startCal.timeInMillis,
            endTime = endCal.timeInMillis,
            createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date()),
            status = "active"
        )

        val nickname = if (UserSession.nickname.isNotBlank()) UserSession.nickname else "demo_user"

        viewModelScope.launch {
            try {
                withTimeout(3000L) {
                    db.child("quests_v3").child(nickname).child(quest.id).setValue(quest).await()
                }
                Toast.makeText(getApplication(), "퀘스트 생성 완료!", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
}