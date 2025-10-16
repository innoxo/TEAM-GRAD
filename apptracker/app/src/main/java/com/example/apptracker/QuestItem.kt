package com.example.apptracker

data class QuestItem(
    val appName: String,
    val packageName: String,
    val targetMinutes: Int,
    val goalType: String,
    val deadlineDate: String,
    val deadlineTime: String,
    var currentMinutes: Int = 0,
    var completed: Boolean = false
)

