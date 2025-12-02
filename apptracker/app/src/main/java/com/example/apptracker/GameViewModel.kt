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

    fun joinAndObserve(roomId: String) {
        viewModelScope.launch {
            repo.observeRoomDetail(roomId) { room ->
                _currentRoom.value = room
                if (room != null && room.status == "active") {
                    checkTimeOver(room)
                    if (System.currentTimeMillis() < room.endTime) {
                        startTracking(room)
                    }
                }
            }
            try { repo.joinRoom(roomId, myName) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun checkTimeAndRefresh() {
        val room = _currentRoom.value ?: return
        checkTimeOver(room)
    }

    private fun checkTimeOver(room: Room) {
        val now = System.currentTimeMillis()
        if (now >= room.endTime && room.status == "active") {
            // 🔥 [수정] 시간이 다 됐으면, 방장뿐만 아니라 '누구든' 발견한 사람이 종료 신호를 보냅니다.
            viewModelScope.launch { finishGameByTimeUp(room) }
        }
    }

    fun toggleReady() {
        val room = _currentRoom.value ?: return
        viewModelScope.launch { repo.toggleReady(room.roomId, myName, !(room.participants[myName]?.isReady ?: false)) }
    }

    fun startGame() {
        val room = _currentRoom.value ?: return
        if (room.creator == myName) viewModelScope.launch { repo.startGame(room.roomId) }
    }

    fun claimReward() {
        val room = _currentRoom.value ?: return
        val myInfo = room.participants[myName] ?: return
        if (myInfo.rewardClaimed) return

        viewModelScope.launch {
            var points = 0
            if (room.mode == "coop") {
                if (room.condition == "≥") points = myInfo.currentMinutes else points = room.goalMinutes
            } else {
                points = room.goalMinutes
            }
            if (points > 0) {
                repo.claimReward(room.roomId, myName, points)
                Toast.makeText(getApplication(), "$points 포인트 획득!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var isTracking = false
    private fun startTracking(room: Room) {
        if (isTracking) return
        isTracking = true

        viewModelScope.launch {
            while (true) {
                val current = _currentRoom.value
                if (current == null || current.status != "active") {
                    isTracking = false
                    break
                }

                val now = System.currentTimeMillis()

                if (now < current.startTime) {
                    delay(1000)
                    continue
                }

                if (now >= current.endTime) {
                    // 🔥 [수정] 시간 종료도 누구나 처리 가능
                    finishGameByTimeUp(current)
                    isTracking = false
                    break
                }

                val used = session.measureAppUsage(current.startTime, min(now, current.endTime), current.targetPackage)
                repo.updateParticipantProgress(current.roomId, myName, used)

                // 🔥 [핵심 수정] 방장만 체크하던 걸 '모든 참가자'가 체크하도록 변경!
                // 이제 내가 룰 위반을 감지하면 내가 바로 게임을 끝내버립니다.
                checkGameRule(current)

                delay(2000)
            }
        }
    }

    private suspend fun checkGameRule(room: Room) {
        val participants = room.participants.values.toList()
        val totalUsage = participants.sumOf { it.currentMinutes }

        if (room.mode == "coop") {
            if (room.condition == "≥") {
                // 이상: 다같이 목표 달성 시 성공
                if (totalUsage >= room.goalMinutes) repo.finishGame(room.roomId, "finished")
            } else {
                // 🔥 이하: 목표 초과 시 즉시 실패 (누구든 감지하면 펑!)
                if (totalUsage > room.goalMinutes) repo.finishGame(room.roomId, "failed")
            }
        } else {
            if (room.condition == "≥") {
                val winner = participants.find { it.currentMinutes >= room.goalMinutes }
                if (winner != null) repo.finishGame(room.roomId, "finished", winner.nickname)
            }
        }
    }

    private suspend fun finishGameByTimeUp(room: Room) {
        val participants = room.participants.values.toList()
        val totalUsage = participants.sumOf { it.currentMinutes }

        if (room.mode == "coop") {
            if (room.condition == "≤") {
                // 시간이 끝났는데 목표 이하라면 성공
                if (totalUsage <= room.goalMinutes) repo.finishGame(room.roomId, "finished")
                else repo.finishGame(room.roomId, "failed")
            } else {
                // 시간이 끝났는데 목표 미달이면 실패
                if (totalUsage < room.goalMinutes) repo.finishGame(room.roomId, "failed")
                else repo.finishGame(room.roomId, "finished")
            }
        } else {
            repo.finishGame(room.roomId, "finished")
        }
    }
}