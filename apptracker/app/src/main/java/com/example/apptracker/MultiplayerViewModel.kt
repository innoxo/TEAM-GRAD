package com.example.apptracker

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MultiplayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RoomRepository()
    private val pm = application.packageManager

    private val _roomList = MutableStateFlow<List<Room>>(emptyList())
    val roomList = _roomList.asStateFlow()

    private val _installedApps = MutableStateFlow<List<App>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    // 🔥 시간 설정 상태 추가
    private val now = Calendar.getInstance()
    private val _startHour = MutableStateFlow(now.get(Calendar.HOUR_OF_DAY))
    val startHour = _startHour.asStateFlow()
    private val _startMinute = MutableStateFlow((now.get(Calendar.MINUTE) / 5) * 5)
    val startMinute = _startMinute.asStateFlow()

    private val _endHour = MutableStateFlow((now.get(Calendar.HOUR_OF_DAY) + 1) % 24)
    val endHour = _endHour.asStateFlow()
    private val _endMinute = MutableStateFlow(0)
    val endMinute = _endMinute.asStateFlow()

    init {
        repo.observeRooms { rooms -> _roomList.value = rooms }
        loadApps()
    }

    private fun loadApps() {
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
            _installedApps.value = apps
        }
    }

    // 시간 설정 함수들
    fun setStartHour(v: Int) {
        val current = Calendar.getInstance()
        val currentHour = current.get(Calendar.HOUR_OF_DAY)
        if (v < currentHour) _startHour.value = currentHour else _startHour.value = v
    }
    fun setStartMinute(v: Int) { _startMinute.value = v }
    fun setEndHour(v: Int) { _endHour.value = v }
    fun setEndMinute(v: Int) { _endMinute.value = v }

    fun createRoom(
        title: String,
        mode: String,
        app: App,
        goalMinutes: Int,
        condition: String
    ) {
        val nickname = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "Guest"
        val roomId = System.currentTimeMillis().toString()

        // 시간 계산
        val nowCal = Calendar.getInstance()

        val startCal = nowCal.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, startHour.value)
        startCal.set(Calendar.MINUTE, startMinute.value)
        startCal.set(Calendar.SECOND, 0)

        // 과거 시간이면 현재로 보정
        if (startCal.timeInMillis < System.currentTimeMillis() - 60000) {
            startCal.timeInMillis = System.currentTimeMillis()
        }

        val endCal = nowCal.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, endHour.value)
        endCal.set(Calendar.MINUTE, endMinute.value)
        endCal.set(Calendar.SECOND, 0)

        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val newRoom = Room(
            roomId = roomId,
            title = title,
            mode = mode,
            targetAppName = app.appName,
            targetPackage = app.packageName,
            goalMinutes = goalMinutes,
            condition = condition,
            creator = nickname,
            status = "waiting",
            startTime = startCal.timeInMillis, // 🔥 설정된 시작 시간
            endTime = endCal.timeInMillis,     // 🔥 설정된 종료 시간
            participants = mapOf(nickname to Participant(nickname, isReady = true))
        )

        viewModelScope.launch {
            repo.createRoom(newRoom)
        }
    }
}