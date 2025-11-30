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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*

class QuestCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val pm = application.packageManager

    // DB 연결 (quests_v3 사용)
    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    private val _appList = MutableStateFlow<List<App>>(emptyList())
    val appList = _appList.asStateFlow()

    private val _selectedApp = MutableStateFlow<App?>(null)
    val selectedApp = _selectedApp.asStateFlow()

    private val _conditionType = MutableStateFlow("≤")
    val conditionType = _conditionType.asStateFlow()

    private val _targetMinutes = MutableStateFlow(10)
    val targetMinutes = _targetMinutes.asStateFlow()

    // 초기값: 현재 시간으로 설정
    private val now = Calendar.getInstance()
    private val _startHour = MutableStateFlow(now.get(Calendar.HOUR_OF_DAY))
    private val _startMinute = MutableStateFlow((now.get(Calendar.MINUTE) / 5) * 5) // 5분 단위 반올림

    val startHour = _startHour.asStateFlow()
    val startMinute = _startMinute.asStateFlow()

    // 종료 시간 초기값: 시작 시간 + 1시간
    private val _endHour = MutableStateFlow((now.get(Calendar.HOUR_OF_DAY) + 1) % 24)
    private val _endMinute = MutableStateFlow(0)

    val endHour = _endHour.asStateFlow()
    val endMinute = _endMinute.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch {
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
        }
    }

    fun selectApp(app: App) { _selectedApp.value = app }
    fun setCondition(c: String) { _conditionType.value = c }
    fun setTargetMinutes(v: Int) { _targetMinutes.value = v }

    // 🔥 [핵심] 시간 설정 시 유효성 검사 (과거 시간 방지)
    fun setStartHour(hour: Int) {
        val current = Calendar.getInstance()
        val currentHour = current.get(Calendar.HOUR_OF_DAY)

        // 현재 시간보다 이전 시간을 선택하면 무시 (또는 현재 시간으로 고정)
        if (hour < currentHour) {
            _startHour.value = currentHour
        } else {
            _startHour.value = hour
        }
        validateMinutes() // 분 단위도 체크
    }

    fun setStartMinute(minute: Int) {
        _startMinute.value = minute
        validateMinutes()
    }

    // 분 단위 유효성 검사 (같은 시간대인데 분이 과거인 경우 방지)
    private fun validateMinutes() {
        val current = Calendar.getInstance()
        val currentHour = current.get(Calendar.HOUR_OF_DAY)
        val currentMinute = current.get(Calendar.MINUTE)

        if (_startHour.value == currentHour && _startMinute.value < currentMinute) {
            // 현재 시간보다 이전 분이면 -> 5분 단위로 올림 처리
            val nextValidMinute = ((currentMinute / 5) + 1) * 5
            if (nextValidMinute < 60) {
                _startMinute.value = nextValidMinute
            } else {
                // 60분이 넘어가면 다음 시간 00분으로
                _startHour.value = (_startHour.value + 1) % 24
                _startMinute.value = 0
            }
        }
    }

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
        val finalMinutes = if (targetMinutes.value <= 0) 10 else targetMinutes.value

        val now = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = now.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, startHour.value)
        startCal.set(Calendar.MINUTE, startMinute.value)

        // 🔥 시작 시간이 현재보다 과거라면 (약간의 오차 허용) 현재 시간으로 보정
        if (startCal.timeInMillis < System.currentTimeMillis() - 60000) {
            Toast.makeText(getApplication(), "시작 시간이 이미 지났습니다. 현재 시간으로 설정합니다.", Toast.LENGTH_SHORT).show()
            startCal.timeInMillis = System.currentTimeMillis()
        }

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, endHour.value)
        endCal.set(Calendar.MINUTE, endMinute.value)

        // 종료 시간이 시작 시간보다 빠르면 다음날로 처리
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

data class App(
    val appName: String,
    val packageName: String
)