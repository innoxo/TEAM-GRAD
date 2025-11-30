package com.example.apptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RoomRepository()
    private val session = QuestSessionManager(application) // 측정기 준비

    val myName = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "Guest"

    private val _currentRoom = MutableStateFlow<Room?>(null)
    val currentRoom = _currentRoom.asStateFlow()

    // 방 입장
    fun joinAndObserve(roomId: String) {
        viewModelScope.launch {
            repo.joinRoom(roomId, myName)
            repo.observeRoomDetail(roomId) { room ->
                _currentRoom.value = room

                // 🔥 [추가] 게임이 'active' 상태가 되면 측정을 시작한다!
                if (room?.status == "active") {
                    startTracking(room)
                }
            }
        }
    }

    // 준비
    fun toggleReady() {
        val room = _currentRoom.value ?: return
        val myInfo = room.participants[myName] ?: return
        viewModelScope.launch { repo.toggleReady(room.roomId, myName, !myInfo.isReady) }
    }

    // 시작 (방장)
    fun startGame() {
        val room = _currentRoom.value ?: return
        if (room.creator != myName) return
        viewModelScope.launch { repo.startGame(room.roomId) }
    }

    // 🔥 [핵심] 실시간 사용량 추적 루프
    private var isTracking = false
    private fun startTracking(room: Room) {
        if (isTracking) return // 이미 돌고 있으면 패스
        isTracking = true

        viewModelScope.launch {
            while (true) {
                // 방이 끝났거나 없어지면 중단
                val current = _currentRoom.value
                if (current == null || current.status != "active") {
                    isTracking = false
                    break
                }

                // 1. 실제 사용량 측정 (게임 시작 시간 ~ 현재)
                val now = System.currentTimeMillis()
                val usedMinutes = session.measureAppUsage(
                    start = room.startTime, // 게임 시작된 시점부터 측정
                    end = now,
                    pkg = room.targetPackage
                )

                // 2. 서버에 내 점수 업데이트
                repo.updateParticipantProgress(room.roomId, myName, usedMinutes)

                delay(2000) // 2초마다 갱신
            }
        }
    }
}