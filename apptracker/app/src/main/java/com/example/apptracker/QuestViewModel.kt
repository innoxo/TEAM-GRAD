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
        val currentList = activeQuests.toList()

        currentList.forEach { q ->
            if (now > q.endTime) return@forEach

            // 🔥 [수정] 목표와 조건을 같이 넘겨서 '엄격한 검사'를 요청함
            val used = session.measureAppUsage(
                start = q.startTime,
                end = now,
                pkg = q.targetPackage,
                goalMinutes = q.goalMinutes,
                condition = q.conditionType
            )

            if (q.progressMinutes != used) {
                var updated = q.copy(progressMinutes = used)

                // 실패 로직 (이미 사용량이 목표를 넘었으면 즉시 실패)
                if (updated.conditionType == "≤" && used > updated.goalMinutes) {
                    updated = updated.copy(status = "completed", success = false)
                    activeQuests.remove(q)
                    completedQuests.add(0, updated)
                    repo.saveQuest(updated)
                } else {
                    val index = activeQuests.indexOfFirst { it.id == q.id }
                    if (index != -1) activeQuests[index] = updated
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
        db.child("users").child(nickname).child("score").setValue(ServerValue.increment(rewardPoints.toLong()))
    }

    fun cancelQuest(q: QuestItem) = viewModelScope.launch {
        val failed = q.copy(status = "completed", success = false)
        repo.saveQuest(failed)
    }

    fun deleteCompleted(id: String) = viewModelScope.launch {
        repo.deleteQuest(id)
    }
}