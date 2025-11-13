package com.example.apptracker

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class QuestViewModel : ViewModel() {

    private val _active = mutableStateListOf<QuestItem>()
    val activeQuests: List<QuestItem> get() = _active

    private val _completed = mutableStateListOf<QuestItem>()
    val completedQuests: List<QuestItem> get() = _completed

    private val prefsKey = "quests_json"

    fun loadQuests(context: Context) {
        val pref = context.getSharedPreferences("AppTracker", Context.MODE_PRIVATE)
        val json = pref.getString(prefsKey, "[]") ?: "[]"

        val arr = JSONArray(json)
        val all = mutableListOf<QuestItem>()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            all.add(
                QuestItem(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    appName = o.getString("appName"),
                    packageName = o.getString("packageName"),
                    targetMinutes = o.getInt("targetMinutes"),
                    goalType = o.getString("goalType"),
                    deadlineDate = o.getString("deadlineDate"),
                    deadlineTime = o.getString("deadlineTime"),
                    currentMinutes = o.optInt("currentMinutes", 0),
                    completed = o.optBoolean("completed", false)
                )
            )
        }

        _active.clear()
        _completed.clear()

        _active.addAll(all.filter { !it.completed })
        _completed.addAll(all.filter { it.completed })
    }

    private fun save(context: Context) {
        val all = (_active + _completed)

        val arr = JSONArray()
        all.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("appName", it.appName)
            o.put("packageName", it.packageName)
            o.put("targetMinutes", it.targetMinutes)
            o.put("goalType", it.goalType)
            o.put("deadlineDate", it.deadlineDate)
            o.put("deadlineTime", it.deadlineTime)
            o.put("currentMinutes", it.currentMinutes)
            o.put("completed", it.completed)
            arr.put(o)
        }

        context.getSharedPreferences("AppTracker", Context.MODE_PRIVATE)
            .edit().putString(prefsKey, arr.toString()).apply()
    }

    fun deleteQuest(context: Context, quest: QuestItem) {
        _active.removeIf { it.id == quest.id }
        _completed.removeIf { it.id == quest.id }
        save(context)
    }

    fun toggleComplete(context: Context, quest: QuestItem) {
        quest.completed = true
        _active.removeIf { it.id == quest.id }
        _completed.add(quest)
        save(context)
    }
}
