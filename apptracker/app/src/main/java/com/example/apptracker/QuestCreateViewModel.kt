package com.example.apptracker

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class QuestCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val pm = application.packageManager
    private val repo = QuestRepository() // Repository 활용

    private val _appList = MutableStateFlow<List<App>>(emptyList())
    val appList = _appList.asStateFlow()

    // ✨ [추가] 추천된 앱 리스트
    private val _recommendedApps = MutableStateFlow<List<App>>(emptyList())
    val recommendedApps = _recommendedApps.asStateFlow()

    private val _selectedApp = MutableStateFlow<App?>(null)
    val selectedApp = _selectedApp.asStateFlow()

    private val _conditionType = MutableStateFlow("≤")
    val conditionType = _conditionType.asStateFlow()

    private val _targetMinutes = MutableStateFlow(0)
    val targetMinutes = _targetMinutes.asStateFlow()

    private val _startHour = MutableStateFlow(0)
    val startHour = _startHour.asStateFlow()

    private val _endHour = MutableStateFlow(1)
    val endHour = _endHour.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch {
            // 1. 설치된 앱 로드
            val apps = withContext(Dispatchers.IO) {
                pm.getInstalledApplications(0)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    .map {
                        App(
                            appName = pm.getApplicationLabel(it).toString(),
                            packageName = it.packageName
                        )
                    }
                    .sortedBy { it.appName }
            }
            _appList.value = apps

            // 2. ✨ [추가] 추천 알고리즘 실행
            loadRecommendations(apps)
        }
    }

    // ✨ [추가] 군집화 기반 추천 실행 함수
    private suspend fun loadRecommendations(allApps: List<App>) {
        val history = repo.loadAllQuests() // 기존 퀘스트 기록 로드
        
        // 백그라운드에서 계산 (무거운 작업일 수 있음)
        val recommendations = withContext(Dispatchers.Default) {
            AppClusteringEngine.getRecommendedApps(allApps, history)
        }
        _recommendedApps.value = recommendations
    }

    fun selectApp(app: App) {
        _selectedApp.value = app
    }

    fun setCondition(c: String) {
        _conditionType.value = c
    }

    fun setTargetMinutes(v: Int) {
        _targetMinutes.value = v
    }

    fun setStartHour(v: Int) {
        _startHour.value = v
    }

    fun setEndHour(v: Int) {
        _endHour.value = v
    }

    private fun today(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        return sdf.format(Date())
    }

    fun createQuest() {
        val app = selectedApp.value ?: return

        // 음수 방어
        val safeTargetMinutes = if (targetMinutes.value < 0) 0 else targetMinutes.value
        val safeStartHour = startHour.value.coerceIn(0, 23)
        val safeEndHour = endHour.value.coerceIn(0, 23)

        val now = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = now.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, safeStartHour)

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, safeEndHour)

        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val quest = QuestItem(
            id = System.currentTimeMillis().toString(),
            targetPackage = app.packageName,
            appName = app.appName,
            conditionType = conditionType.value,
            goalMinutes = safeTargetMinutes,
            startTime = startCal.timeInMillis,
            endTime = endCal.timeInMillis,
            createdDate = today(),
            status = "active"
        )

        viewModelScope.launch {
            repo.saveQuest(today(), quest)
        }
    }
}

data class App(
    val appName: String,
    val packageName: String
)