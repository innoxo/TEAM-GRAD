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
            val used = session.measureAppUsage(q.startTime, now, q.targetPackage)

            if (q.progressMinutes != used) {
                val updated = q.copy(progressMinutes = used)
                val index = activeQuests.indexOfFirst { it.id == q.id }
                if (index != -1) activeQuests[index] = updated
                repo.saveQuest(updated) // 날짜 인자 제거됨
            }
        }
    }

    fun markCompleted(q: QuestItem) = viewModelScope.launch {
        val done = q.copy(status = "completed", isSuccess = true)
        repo.saveQuest(done) // 날짜 인자 제거됨

        val rewardPoints = if (q.goalMinutes > 0) q.goalMinutes else 50
        val nickname = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "demo_user"

        db.child("users").child(nickname).child("score")
            .setValue(ServerValue.increment(rewardPoints.toLong()))
    }

    fun cancelQuest(q: QuestItem) = viewModelScope.launch {
        val failed = q.copy(status = "completed", isSuccess = false)
        repo.saveQuest(failed) // 날짜 인자 제거됨
    }

    fun deleteCompleted(id: String) = viewModelScope.launch {
        repo.deleteQuest(id) // 날짜 인자 제거됨
    }
}