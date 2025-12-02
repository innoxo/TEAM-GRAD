package com.example.apptracker

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class QuestViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = QuestRepository()
    private val session = QuestSessionManager(application)

    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    var activeQuests = mutableStateListOf<QuestItem>()
    var completedQuests = mutableStateListOf<QuestItem>()

    fun refresh() {
        repo.observeQuests { quests ->
            activeQuests.clear()
            completedQuests.clear()

            quests.forEach {
                if (it.status == "active") activeQuests.add(it)
                else completedQuests.add(it)
            }
            activeQuests.sortByDescending { it.startTime }
            completedQuests.sortByDescending { it.endTime }
        }
    }

    fun updateProgress() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        // 리스트 복사본으로 반복 (중간에 삭제될 수 있어서 안전하게)
        val currentList = activeQuests.toList()

        currentList.forEach { q ->
            // 시간이 아직 시작 안 했거나 끝났으면 측정 안 함 (끝난 건 아래에서 처리될 수도 있음)
            if (now > q.endTime) return@forEach

            // 사용량 측정
            val used = session.measureAppUsage(q.startTime, now, q.targetPackage)

            if (q.progressMinutes != used) {
                var updated = q.copy(progressMinutes = used)

                // 🔥 [핵심 로직] "이하(≤)" 퀘스트인데 목표를 초과했다? -> 즉시 실패 처리!
                if (updated.conditionType == "≤" && used > updated.goalMinutes) {
                    // 1. 실패 상태로 변경 (success = false)
                    updated = updated.copy(status = "completed", success = false)

                    // 2. 리스트 이동 (진행중 -> 완료됨)
                    activeQuests.remove(q)
                    completedQuests.add(0, updated)

                    // 3. DB 저장
                    repo.saveQuest(updated)
                } else {
                    // 아직 실패 안 했으면 진행 상황만 업데이트
                    val index = activeQuests.indexOfFirst { it.id == q.id }
                    if (index != -1) {
                        activeQuests[index] = updated
                    }
                    repo.saveQuest(updated)
                }
            }
        }
    }

    fun markCompleted(q: QuestItem) = viewModelScope.launch {
        val done = q.copy(status = "completed", success = true)
        repo.saveQuest(done)

        val rewardPoints = if (q.goalMinutes > 0) q.goalMinutes else 50
        val nickname = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "demo_user"

        db.child("users").child(nickname).child("score")
            .setValue(ServerValue.increment(rewardPoints.toLong()))
    }

    fun cancelQuest(q: QuestItem) = viewModelScope.launch {
        val failed = q.copy(status = "completed", success = false)
        repo.saveQuest(failed)
    }

    fun deleteCompleted(id: String) = viewModelScope.launch {
        repo.deleteQuest(id)
    }

    private fun today(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        return sdf.format(Date())
    }
}