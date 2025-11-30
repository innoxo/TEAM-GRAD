package com.example.apptracker

data class QuestItem(
    val id: String = "",
    val targetPackage: String = "",
    val appName: String = "",
    val conditionType: String = "≤",
    val goalMinutes: Int = 0,

    val startTime: Long = 0L,
    val endTime: Long = 0L,

    val createdDate: String = "",
    val progressMinutes: Int = 0,
    val status: String = "active",

    // 🔥 [수정됨] isSuccess -> success (파이어베이스 버그 해결)
    val success: Boolean = false
)