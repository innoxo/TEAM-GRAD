package com.example.apptracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class QuestSessionManager(private val context: Context) {

    private val usage = context.getSystemService(UsageStatsManager::class.java)

    suspend fun measureAppUsage(
        start: Long,
        end: Long,
        pkg: String,
        goalMinutes: Int = 0,
        condition: String = ""
    ): Int = withContext(Dispatchers.IO) {

        // 1. 오직 정밀 측정(이벤트 기반)만 사용합니다.
        // (하루 통계 가져오는 백업 로직 삭제함 -> 65분 뜨는 버그 해결)
        val totalMillis = calculateMillisFromEvents(start, end, pkg)

        var finalMinutes = (totalMillis / 60000L).toInt()

        // 2. '이하(≤)' 퀘스트 즉시 실패 로직 (유지)
        if (condition == "≤" || condition == "<=") {
            val goalMillis = goalMinutes * 60 * 1000L
            if (totalMillis > goalMillis) {
                if (finalMinutes <= goalMinutes) {
                    finalMinutes = goalMinutes + 1
                }
            }
        }

        finalMinutes
    }

    // 타임라인 스캔 방식 (정밀 측정)
    private fun calculateMillisFromEvents(start: Long, end: Long, pkg: String): Long {
        // 이미 켜져있는 앱을 잡기 위해 24시간 전부터 스캔
        val searchStart = start - (1000 * 60 * 60 * 24)
        val events = usage.queryEvents(searchStart, end)
        val event = UsageEvents.Event()

        var totalTime = 0L
        var lastEventTime = searchStart
        var currentForegroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            // 시간이 흘렀고, 직전까지 내 앱이 켜져 있었다면 시간 추가
            if (event.timeStamp > lastEventTime) {
                if (currentForegroundPackage == pkg) {
                    // 🔥 [핵심] '퀘스트 구간(start ~ end)'과 겹치는 시간만 잘라냅니다.
                    // 아침에 쓴 기록은 여기서 다 걸러집니다.
                    val duration = calculateOverlap(lastEventTime, event.timeStamp, start, end)
                    totalTime += duration
                }
            }

            // 앱 상태 갱신
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> currentForegroundPackage = event.packageName

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (event.packageName == currentForegroundPackage) currentForegroundPackage = null
                }
            }
            lastEventTime = event.timeStamp
        }

        // 현재 켜져있는 상태 처리
        if (currentForegroundPackage == pkg) {
            totalTime += calculateOverlap(lastEventTime, end, start, end)
        }

        return totalTime
    }

    // 겹치는 구간 계산기
    private fun calculateOverlap(blockStart: Long, blockEnd: Long, questStart: Long, questEnd: Long): Long {
        // 시작점: (앱 켠 시간) vs (퀘스트 시작 시간) 중 더 늦은 것
        val actualStart = max(blockStart, questStart)
        // 끝점: (앱 끈 시간) vs (퀘스트 종료/현재 시간) 중 더 빠른 것
        val actualEnd = min(blockEnd, questEnd)

        // 유효한 구간이면 반환
        return if (actualEnd > actualStart) actualEnd - actualStart else 0L
    }
}