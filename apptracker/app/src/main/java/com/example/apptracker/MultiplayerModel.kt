package com.example.apptracker

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Room(
    val roomId: String = "",
    val title: String = "",
    val mode: String = "coop",
    val targetAppName: String = "",
    val targetPackage: String = "",
    val condition: String = "≤",
    val goalMinutes: Int = 30,
    val creator: String = "",

    // status: waiting(대기) -> active(진행) -> finished(종료) -> failed(실패)
    val status: String = "waiting",

    val startTime: Long = 0L,
    val endTime: Long = 0L,

    // 🔥 [추가] 승자 이름 (PvP용)
    val winner: String = "",

    val participants: Map<String, Participant> = emptyMap()
)

@IgnoreExtraProperties
data class Participant(
    val nickname: String = "",
    val isReady: Boolean = false,
    val currentMinutes: Int = 0,
    val score: Int = 0,
    // 🔥 [추가] 보상 받았는지 체크
    val rewardClaimed: Boolean = false
)