package com.example.apptracker

import android.app.Application
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class QuestCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val pm = application.packageManager

    // [통합] 두 가지 데이터 소스를 모두 사용
    // 1. Repository: AI 추천 기능을 위해 기존 기록을 불러올 때 사용
    private val repo = QuestRepository() 
    
    // 2. DB 직접 연결: 퀘스트 생성 시 지정한 경로(v3)에 저장하기 위해 사용 (Main)
    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    private val _appList = MutableStateFlow<List<App>>(emptyList())
    val appList = _appList.asStateFlow()

    // 추천된 앱 리스트
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
            // 1. 설치된 앱 로드
            val apps = withContext(Dispatchers.IO) {
                val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                allApps.filter { appInfo ->
                    pm.getLaunchIntentForPackage(appInfo.packageName) != null
                }.map { appInfo ->
                    App(
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        packageName = appInfo.packageName
                    )
                }.sortedBy { it.appName }
            }
            _appList.value = apps

            // 2. 추천 알고리즘 실행
            // 로드된 앱 리스트를 바탕으로 AI 추천을 계산
            loadRecommendations(apps)
        }
    }

    // 추가: 군집화 기반 추천 실행 함수
    private suspend fun loadRecommendations(allApps: List<App>) {
        // Repository를 통해 과거 데이터를 안전하게 불러옴
        val history = repo.loadAllQuests() 
        
        // 백그라운드에서 계산
        val recommendations = withContext(Dispatchers.Default) {
            AppClusteringEngine.getRecommendedApps(allApps, history)
        }
        _recommendedApps.value = recommendations
    }

    fun selectApp(app: App) { _selectedApp.value = app }
    fun setCondition(c: String) { _conditionType.value = c }
    fun setTargetMinutes(v: Int) { _targetMinutes.value = v }

    fun setStartHour(v: Int) { _startHour.value = v }
    fun setStartMinute(v: Int) { _startMinute.value = v }
    fun setEndHour(v: Int) { _endHour.value = v }
    fun setEndMinute(v: Int) { _endMinute.value = v }

    private fun today(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        return sdf.format(Date())
    }

    fun createQuest(onSuccess: () -> Unit) {
        if (_isLoading.value) return

        val app = selectedApp.value
        if (app == null) {
            Toast.makeText(getApplication(), "앱을 먼저 선택해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        _isLoading.value = true

        // 입력값 보정 (로직 통합)
        val finalMinutes = if (targetMinutes.value <= 0) 10 else targetMinutes.value
        
        // 시간 설정 로직
        val now = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = now.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, startHour.value)
        startCal.set(Calendar.MINUTE, startMinute.value)

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, endHour.value)
        endCal.set(Calendar.MINUTE, endMinute.value)

        // 종료 시간이 시작 시간보다 빠르면 다음 날로 처리
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
            createdDate = today(),
            status = "active"
        )

        // 닉네임 처리 (UserSession 사용 가정)
        val nickname = if (UserSession.nickname.isNotBlank()) UserSession.nickname else "demo_user"

        // DB에 직접 저장
        // 경로(quests_v3)를 그대로 사용하여 충돌을 방지
        db.child("quests_v3").child(nickname).child(quest.id)
            .setValue(quest)
            .addOnSuccessListener {
                _isLoading.value = false
                Toast.makeText(getApplication(), "퀘스트 생성 완료!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                _isLoading.value = false
                Toast.makeText(getApplication(), "오류 발생", Toast.LENGTH_SHORT).show()
            }
    }
}

data class App(
    val appName: String,
    val packageName: String
)