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

    // 1. 방 입장 및 실시간 감시
    fun joinAndObserve(roomId: String) {
        viewModelScope.launch {
            // 방 정보 구독
            repo.observeRoomDetail(roomId) { room ->
                _currentRoom.value = room

                // 이미 게임 중이라면 추적 시작
                if (room != null && room.status == "active") {
                    // 들어오자마자 시간 끝났는지 체크
                    checkTimeOver(room)

                    // 시간이 남았으면 추적기 가동
                    if (System.currentTimeMillis() < room.endTime) {
                        startTracking(room)
                    }
                }
            }
            // 내 입장 정보 저장 (백그라운드)
            try { repo.joinRoom(roomId, myName) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 2. 화면 다시 켰을 때(ON_RESUME) 시간 체크용 함수
    fun checkTimeAndRefresh() {
        val room = _currentRoom.value ?: return
        checkTimeOver(room)
    }

    // 시간 초과 체크 (공통 로직)
    private fun checkTimeOver(room: Room) {
        val now = System.currentTimeMillis()
        if (now >= room.endTime && room.status == "active") {
            // 시간이 다 됐는데 아직 안 끝났으면 -> 방장이 종료 처리
            if (room.creator == myName) {
                viewModelScope.launch {
                    finishGameByTimeUp(room)
                }
            }
        }
    }

    // 3. 준비 상태 토글
    fun toggleReady() {
        val room = _currentRoom.value ?: return
        viewModelScope.launch {
            repo.toggleReady(room.roomId, myName, !(room.participants[myName]?.isReady ?: false))
        }
    }

    // 4. 게임 시작 (방장 전용)
    fun startGame() {
        val room = _currentRoom.value ?: return
        if (room.creator == myName) {
            viewModelScope.launch { repo.startGame(room.roomId) }
        }
    }

    // 5. 보상 받기
    fun claimReward() {
        val room = _currentRoom.value ?: return
        val myInfo = room.participants[myName] ?: return
        if (myInfo.rewardClaimed) return

        viewModelScope.launch {
            var points = 0

            // 협력 모드 보상
            if (room.mode == "coop") {
                if (room.condition == "≥") {
                    // 이상(채우기): 내 기여도만큼
                    points = myInfo.currentMinutes
                } else {
                    // 이하(참기): 목표 시간만큼 보너스
                    points = room.goalMinutes
                }
            }
            // 경쟁 모드 보상
            else {
                points = room.goalMinutes
            }

            if (points > 0) {
                repo.claimReward(room.roomId, myName, points)
                Toast.makeText(getApplication(), "$points 포인트 획득!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔥 6. 실시간 추적 루프 (핵심 엔진)
    private var isTracking = false
    private fun startTracking(room: Room) {
        if (isTracking) return
        isTracking = true

        viewModelScope.launch {
            while (true) {
                val current = _currentRoom.value
                // 방이 없거나 종료되었으면 루프 탈출
                if (current == null || current.status != "active") {
                    isTracking = false
                    break
                }

                val now = System.currentTimeMillis()

                // 시작 시간 전이면 대기
                if (now < current.startTime) {
                    delay(1000)
                    continue
                }

                // 종료 시간 지났으면 처리 후 탈출
                if (now >= current.endTime) {
                    if (current.creator == myName) finishGameByTimeUp(current)
                    isTracking = false
                    break
                }

                // 앱 사용량 측정 (QuestSessionManager가 정밀 측정함)
                val used = session.measureAppUsage(current.startTime, min(now, current.endTime), current.targetPackage)

                // 내 점수 업데이트
                repo.updateParticipantProgress(current.roomId, myName, used)

                // 방장은 승패 판정도 수행
                if (current.creator == myName) checkGameRule(current)

                delay(2000) // 2초 주기
            }
        }
    }

    // 7. 실시간 승패 판정 (진행 중일 때)
    private suspend fun checkGameRule(room: Room) {
        val participants = room.participants.values.toList()
        val totalUsage = participants.sumOf { it.currentMinutes }

        if (room.mode == "coop") {
            if (room.condition == "≥") {
                // 이상: 다같이 합쳐서 목표 넘으면 성공!
                if (totalUsage >= room.goalMinutes) repo.finishGame(room.roomId, "finished")
            } else {
                // 이하: 합쳐서 목표 넘으면 즉시 실패!
                if (totalUsage > room.goalMinutes) repo.finishGame(room.roomId, "failed")
            }
        } else {
            if (room.condition == "≥") {
                // 경쟁(이상): 누구라도 목표 넘으면 그 사람이 승리!
                val winner = participants.find { it.currentMinutes >= room.goalMinutes }
                if (winner != null) repo.finishGame(room.roomId, "finished", winner.nickname)
            }
        }
    }

    // 8. 시간이 다 됐을 때 판정 (타임아웃)
    private suspend fun finishGameByTimeUp(room: Room) {
        val participants = room.participants.values.toList()
        val totalUsage = participants.sumOf { it.currentMinutes }

        if (room.mode == "coop") {
            if (room.condition == "≤") {
                // 이하(참기): 시간 끝날 때까지 안 터졌으면 성공! (0분이어도 성공)
                if (totalUsage <= room.goalMinutes) repo.finishGame(room.roomId, "finished")
                else repo.finishGame(room.roomId, "failed")
            } else {
                // 이상(채우기): 시간 끝났는데 못 채웠으면 실패!
                if (totalUsage < room.goalMinutes) repo.finishGame(room.roomId, "failed")
                else repo.finishGame(room.roomId, "finished")
            }
        } else {
            // 경쟁(참기): 시간 종료 시점의 승패는 개별 판단 (일단 게임 종료 처리)
            repo.finishGame(room.roomId, "finished")
        }
    }
}