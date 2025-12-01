package com.example.apptracker

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.Long.min

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RoomRepository()
    private val session = QuestSessionManager(application)
    val myName = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "Guest"

    private val _currentRoom = MutableStateFlow<Room?>(null)
    val currentRoom = _currentRoom.asStateFlow()

    // 방 입장 및 감시
    fun joinAndObserve(roomId: String) {
        viewModelScope.launch {
            repo.observeRoomDetail(roomId) { room ->
                _currentRoom.value = room
                // 방이 활성화(active) 상태라면 추적 로직 시작
                if (room?.status == "active") {
                    startTracking(room)
                }
            }
            // 내 입장 정보 저장
            try { repo.joinRoom(roomId, myName) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 준비 상태 토글
    fun toggleReady() {
        val room = _currentRoom.value ?: return
        viewModelScope.launch {
            repo.toggleReady(room.roomId, myName, !(room.participants[myName]?.isReady ?: false))
        }
    }

    // 게임 시작 (방장만)
    fun startGame() {
        val room = _currentRoom.value ?: return
        if (room.creator == myName) {
            viewModelScope.launch { repo.startGame(room.roomId) }
        }
    }

    // 보상 받기
    fun claimReward() {
        val room = _currentRoom.value ?: return
        val myInfo = room.participants[myName] ?: return
        if (myInfo.rewardClaimed) return

        viewModelScope.launch {
            var points = 0
            if (room.mode == "coop") {
                if (room.condition == "≥") points = myInfo.currentMinutes else points = room.goalMinutes
            } else {
                // 경쟁 모드
                points = room.goalMinutes
            }

            if (points > 0) {
                repo.claimReward(room.roomId, myName, points)
                Toast.makeText(getApplication(), "$points 포인트 획득!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔥 [핵심] 실시간 추적 로직 (시간 대기 기능 포함)
    private var isTracking = false
    private fun startTracking(room: Room) {
        if (isTracking) return
        isTracking = true

        viewModelScope.launch {
            while (true) {
                val current = _currentRoom.value
                // 방이 없거나 종료되었으면 중단
                if (current == null || current.status != "active") {
                    isTracking = false
                    break
                }

                val now = System.currentTimeMillis()

                // 🔥 시작 시간이 아직 안 됐으면, 측정하지 않고 대기합니다!
                if (now < current.startTime) {
                    delay(1000) // 1초 대기
                    continue    // 다음 루프로 넘어감 (아래 측정 로직 실행 안 함)
                }

                // 종료 시간 체크
                if (now >= current.endTime) {
                    if (current.creator == myName) finishGameByTimeUp(current)
                }

                // 사용량 측정
                val used = session.measureAppUsage(current.startTime, min(now, current.endTime), current.targetPackage)
                repo.updateParticipantProgress(current.roomId, myName, used)

                // 승패 판정 (방장만)
                if (current.creator == myName) checkGameRule(current)

                delay(2000)
            }
        }
    }

    // 승패 판정 로직
    private suspend fun checkGameRule(room: Room) {
        val participants = room.participants.values.toList()
        val totalUsage = participants.sumOf { it.currentMinutes }

        if (room.mode == "coop") {
            if (room.condition == "≥") {
                if (totalUsage >= room.goalMinutes) repo.finishGame(room.roomId, "finished")
            } else {
                if (totalUsage > room.goalMinutes) repo.finishGame(room.roomId, "failed")
            }
        } else {
            if (room.condition == "≥") {
                val winner = participants.find { it.currentMinutes >= room.goalMinutes }
                if (winner != null) repo.finishGame(room.roomId, "finished", winner.nickname)
            }
        }
    }

    // 시간 초과 시 판정
    private suspend fun finishGameByTimeUp(room: Room) {
        if (room.mode == "coop") {
            if (room.condition == "≤") repo.finishGame(room.roomId, "finished")
            else repo.finishGame(room.roomId, "failed")
        } else {
            repo.finishGame(room.roomId, "finished")
        }
    }
}