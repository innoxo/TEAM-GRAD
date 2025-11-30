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

class MultiplayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RoomRepository()
    private val pm = application.packageManager

    private val _roomList = MutableStateFlow<List<Room>>(emptyList())
    val roomList = _roomList.asStateFlow()

    // 앱 목록 (방 만들 때 선택용)
    private val _installedApps = MutableStateFlow<List<App>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    init {
        repo.observeRooms { rooms ->
            _roomList.value = rooms
        }
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

    // 🔥 [수정됨] 구체적인 설정을 받아서 방 생성
    fun createRoom(
        title: String,
        mode: String,
        app: App,
        goalMinutes: Int,
        condition: String
    ) {
        val nickname = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "Guest"
        val roomId = System.currentTimeMillis().toString()

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
            startTime = 0L, // 아직 시작 안 함
            participants = mapOf(nickname to Participant(nickname, isReady = true))
        )

        viewModelScope.launch {
            repo.createRoom(newRoom)
        }
    }
}