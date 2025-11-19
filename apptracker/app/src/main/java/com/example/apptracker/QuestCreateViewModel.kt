package com.example.apptracker

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class QuestCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val pm = application.packageManager
    private val repo = QuestRepository()

    private val _appList = MutableStateFlow<List<App>>(emptyList())
    val appList = _appList.asStateFlow()

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
            val apps = pm.getInstalledApplications(0)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map {
                    App(
                        appName = pm.getApplicationLabel(it).toString(),
                        packageName = it.packageName
                    )
                }
                .sortedBy { it.appName }

            _appList.value = apps
        }
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

        // 음수나 이상한 값 들어왔을 때 방어
        val safeTargetMinutes = if (targetMinutes.value < 0) 0 else targetMinutes.value
        val safeStartHour = startHour.value.coerceIn(0, 23)
        val safeEndHour = endHour.value.coerceIn(0, 23)

        // 오늘 날짜 기준으로 시간 설정
        val now = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = now.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, safeStartHour)

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, safeEndHour)

        // 종료 시간이 시작 시간보다 이르면 다음날로 넘김 (크래시 방지)
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
