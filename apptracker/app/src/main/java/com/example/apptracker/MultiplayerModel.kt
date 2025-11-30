package com.example.apptracker

// 방 정보
data class Room(
    val roomId: String = "",
    val title: String = "",
    val mode: String = "coop",
    val targetAppName: String = "",
    val targetPackage: String = "",
    val condition: String = "≤",
    val goalMinutes: Int = 30,
    val creator: String = "",
    val status: String = "waiting",
    val startTime: Long = 0L,        // 🔥 [추가] 게임 시작된 시간
    val participants: Map<String, Participant> = emptyMap()
)

// 참가자 정보
data class Participant(
    val nickname: String = "",
    val isReady: Boolean = false,
    val currentMinutes: Int = 0,     // 현재 사용량
    val score: Int = 0
)