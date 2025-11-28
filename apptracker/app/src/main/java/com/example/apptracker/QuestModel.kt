package com.example.apptracker

data class QuestItem(
    val id: String = "",
    val targetPackage: String = "",
    val appName: String = "",
    val conditionType: String = "≤", // ≤ 또는 ≥
    val goalMinutes: Int = 0,

    val startTime: Long = 0L,        // 퀘스트 측정 시작시간 (millis)
    val endTime: Long = 0L,          // 퀘스트 종료시간 (millis)

    val createdDate: String = "",
    val progressMinutes: Int = 0,
    val status: String = "active"     // active / completed
)