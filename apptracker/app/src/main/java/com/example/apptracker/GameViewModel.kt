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
                if (now < current.startTime) { delay(1000); continue }

                if (now >= current.endTime) {
                    finishGameByTimeUp(current)
                    isTracking = false
                    break
                }

                // 🔥 [수정] 목표와 조건을 넘겨서 엄격한 검사 수행
                val used = session.measureAppUsage(
                    start = current.startTime,
                    end = min(now, current.endTime),
                    pkg = current.targetPackage,
                    goalMinutes = current.goalMinutes,
                    condition = current.condition
                )

                repo.updateParticipantProgress(current.roomId, myName, used)

                // 즉시 심판 (내 점수 반영)
                val myInfo = current.participants[myName]?.copy(currentMinutes = used) ?: Participant(myName, true, used, 0)
                val updatedParticipants = current.participants.toMutableMap()
                updatedParticipants[myName] = myInfo
                val roomForCheck = current.copy(participants = updatedParticipants)

                checkGameRule(roomForCheck)

                delay(2000)
            }
        }
    }

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
            } else {
                val loser = participants.find { it.currentMinutes > room.goalMinutes }
                if (loser != null) {
                    val winner = participants.find { it.nickname != loser.nickname }
                    if (winner != null) repo.finishGame(room.roomId, "finished", winner.nickname)
                    else repo.finishGame(room.roomId, "failed")
                }
            }
        }
    }

    private suspend fun finishGameByTimeUp(room: Room) {
        val participants = room.participants.values.toList()
        val totalUsage = participants.sumOf { it.currentMinutes }

        if (room.mode == "coop") {
            if (room.condition == "≤") {
                if (totalUsage <= room.goalMinutes) repo.finishGame(room.roomId, "finished")
                else repo.finishGame(room.roomId, "failed")
            } else {
                if (totalUsage < room.goalMinutes) repo.finishGame(room.roomId, "failed")
                else repo.finishGame(room.roomId, "finished")
            }
        } else {
            if (room.condition == "≤") {
                val sorted = participants.sortedBy { it.currentMinutes }
                val winner = sorted.firstOrNull()
                if (winner != null && winner.currentMinutes <= room.goalMinutes) {
                    repo.finishGame(room.roomId, "finished", winner.nickname)
                } else {
                    repo.finishGame(room.roomId, "failed")
                }
            } else {
                repo.finishGame(room.roomId, "finished")
            }
        }
    }
}