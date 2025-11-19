package com.example.apptracker

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class QuestViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = QuestRepository()
    private val session = QuestSessionManager(application)

    var activeQuests = mutableStateListOf<QuestItem>()
    var completedQuests = mutableStateListOf<QuestItem>()

    private fun today(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        return sdf.format(Date())
    }

    fun loadQuests() = viewModelScope.launch {
        val quests = repo.loadAllQuests()

        activeQuests.clear()
        completedQuests.clear()

        quests.forEach {
            if (it.status == "active") activeQuests.add(it)
            else completedQuests.add(it)
        }

        activeQuests.sortByDescending { it.startTime }
        completedQuests.sortByDescending { it.endTime }
    }

    fun createQuest(
        pkg: String,
        appName: String,
        condition: String,
        goal: Int,
        startTime: Long,
        endTime: Long
    ) {
        val id = System.currentTimeMillis().toString()

        val quest = QuestItem(
            id = id,
            targetPackage = pkg,
            appName = appName,
            conditionType = condition,
            goalMinutes = goal,
            startTime = startTime,
            endTime = endTime,
            createdDate = today()
        )

        activeQuests.add(0, quest)

        viewModelScope.launch {
            repo.saveQuest(today(), quest)
        }
    }

    fun updateProgress() = viewModelScope.launch {
        val now = System.currentTimeMillis()

        activeQuests.forEachIndexed { idx, q ->
            if (now > q.endTime) return@forEachIndexed

            val used = session.measureAppUsage(q.startTime, now, q.targetPackage)
            val updated = q.copy(progressMinutes = used)
            activeQuests[idx] = updated

            repo.saveQuest(q.createdDate, updated)
        }
    }

    fun markCompleted(q: QuestItem) = viewModelScope.launch {
        activeQuests.removeAll { it.id == q.id }
        val done = q.copy(status = "completed")

        completedQuests.add(0, done)
        repo.saveQuest(done.createdDate, done)
    }

    fun deleteCompleted(id: String) = viewModelScope.launch {
        completedQuests.removeAll { it.id == id }
        repo.deleteQuest(today(), id)
    }

    fun cancelQuest(id: String) = viewModelScope.launch {
        activeQuests.removeAll { it.id == id }
        repo.deleteQuest(today(), id)
    }
}
