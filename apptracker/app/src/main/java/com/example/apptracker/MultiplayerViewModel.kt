package com.example.apptracker

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
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

class MultiplayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RoomRepository()
    private val pm = application.packageManager

    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    // 🔥 [추가] 로비 화면에서 '나'를 식별하기 위해 필요
    val myName = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "Guest"

    private val _activeRooms = MutableStateFlow<List<Room>>(emptyList())
    val activeRooms = _activeRooms.asStateFlow()

    private val _completedRooms = MutableStateFlow<List<Room>>(emptyList())
    val completedRooms = _completedRooms.asStateFlow()

    private val _installedApps = MutableStateFlow<List<App>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    // 시간 설정
    private val now = Calendar.getInstance()
    private val _startHour = MutableStateFlow(now.get(Calendar.HOUR_OF_DAY))
    val startHour = _startHour.asStateFlow()
    private val _startMinute = MutableStateFlow((now.get(Calendar.MINUTE) / 5) * 5)
    val startMinute = _startMinute.asStateFlow()
    private val _endHour = MutableStateFlow((now.get(Calendar.HOUR_OF_DAY) + 1) % 24)
    val endHour = _endHour.asStateFlow()
    private val _endMinute = MutableStateFlow(0)
    val endMinute = _endMinute.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        repo.observeRooms { rooms ->
            _activeRooms.value = rooms.filter { it.status == "waiting" || it.status == "active" }
            _completedRooms.value = rooms.filter { it.status == "finished" || it.status == "failed" }
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

    fun setStartHour(v: Int) {
        val current = Calendar.getInstance()
        val currentHour = current.get(Calendar.HOUR_OF_DAY)
        if (v < currentHour) _startHour.value = currentHour else _startHour.value = v
    }
    fun setStartMinute(v: Int) { _startMinute.value = v }
    fun setEndHour(v: Int) { _endHour.value = v }
    fun setEndMinute(v: Int) { _endMinute.value = v }

    fun createRoom(
        title: String, mode: String, app: App, goalMinutes: Int, condition: String
    ) {
        if (_isLoading.value) return
        _isLoading.value = true

        val roomId = System.currentTimeMillis().toString()
        val nowCal = Calendar.getInstance()
        val startCal = nowCal.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, startHour.value)
        startCal.set(Calendar.MINUTE, startMinute.value)
        startCal.set(Calendar.SECOND, 0)

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
            creator = myName,
            status = "waiting",
            startTime = startCal.timeInMillis,
            endTime = endCal.timeInMillis,
            participants = hashMapOf(myName to Participant(myName, isReady = true))
        )

        viewModelScope.launch {
            try {
                repo.createRoom(newRoom)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}