package com.example.apptracker

import android.app.Application
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
// 🔥 [중요] AI 추천 엔진 import
import com.example.apptracker.ai.AppClusteringEngine
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

    // 1. 추천 알고리즘용 Repository
    private val repo = QuestRepository()

    // 2. 퀘스트 생성용 직접 DB 연결 (quests_v3)
    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    private val _appList = MutableStateFlow<List<App>>(emptyList())
    val appList = _appList.asStateFlow()

    // 🔥 추천 앱 리스트 (화면 에러 방지)
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
            val apps = withContext(Dispatchers.IO) {
                val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val myPackage = getApplication<Application>().packageName

                allApps.filter { appInfo ->
                    pm.getLaunchIntentForPackage(appInfo.packageName) != null &&
                            appInfo.packageName != myPackage
                }.map { appInfo ->
                    App(
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        packageName = appInfo.packageName
                    )
                }.sortedBy { it.appName }
            }
            _appList.value = apps

            // 🔥 앱 로딩 후 추천 알고리즘 실행
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

        val app = selectedApp.value
        if (app == null) {
            Toast.makeText(getApplication(), "앱을 먼저 선택해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        _isLoading.value = true
        val finalMinutes = if (targetMinutes.value <= 0) 10 else targetMinutes.value

        val now = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = now.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, startHour.value)
        startCal.set(Calendar.MINUTE, startMinute.value)

        if (startCal.timeInMillis < System.currentTimeMillis() - 60000) {
            startCal.timeInMillis = System.currentTimeMillis()
        }

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, endHour.value)
        endCal.set(Calendar.MINUTE, endMinute.value)

        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.add(Calendar.DAY_OF_MONTH, 1)
        }

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
                    db.child("quests_v3").child(nickname).child(quest.id)
                        .setValue(quest)
                        .await()
                }
                Toast.makeText(getApplication(), "퀘스트 생성 완료!", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// 🔥 [필수] App 데이터 클래스 (다른 파일에서 참조함)
data class App(
    val appName: String,
    val packageName: String
)