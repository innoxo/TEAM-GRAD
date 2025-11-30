package com.example.apptracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class QuestSessionManager(private val context: Context) {

    private val usage = context.getSystemService(UsageStatsManager::class.java)

    suspend fun measureAppUsage(start: Long, end: Long, pkg: String): Int =
        withContext(Dispatchers.IO) {

            // 1. 정밀 측정 시도 (이벤트 기반)
            var result = calculateFromEvents(start, end, pkg)

            // 2. 만약 0분이 나왔다면? -> 대시보드 값(오늘 하루 총량)을 확인해본다. (백업 로직)
            if (result == 0) {
                val dailyUsage = calculateDailyTotal(pkg)

                // 대시보드에는 기록이 있고(0보다 크고), 퀘스트가 '오늘' 시작된 거라면?
                // -> 0분 대신 대시보드 값을 쓴다! (동기화)
                if (dailyUsage > 0 && isQuestStartedToday(start)) {
                    result = dailyUsage
                }
            }

            result
        }

    // 🕵️‍♂️ 정밀 측정 (타임라인 스캔 방식)
    private fun calculateFromEvents(start: Long, end: Long, pkg: String): Int {
        val searchStart = start - (1000 * 60 * 60 * 24) // 24시간 전부터 조회
        val events = usage.queryEvents(searchStart, end)
        val event = UsageEvents.Event()

        var totalTime = 0L
        var lastEventTime = searchStart
        var currentForegroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.timeStamp > lastEventTime) {
                if (currentForegroundPackage == pkg) {
                    val duration = calculateOverlap(lastEventTime, event.timeStamp, start, end)
                    totalTime += duration
                }
            }

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

        if (currentForegroundPackage == pkg) {
            totalTime += calculateOverlap(lastEventTime, end, start, end)
        }

        return (totalTime / 60000L).toInt()
    }

    // 📊 하루 총 사용량 가져오기 (대시보드와 동일한 방식)
    private fun calculateDailyTotal(pkg: String): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = System.currentTimeMillis()

        val stats = usage.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            endOfDay
        )

        if (stats != null) {
            val totalMillis = stats
                .filter { it.packageName == pkg }
                .sumOf { it.totalTimeInForeground }

            return (totalMillis / 60000L).toInt()
        }

        return 0
    }

    private fun calculateOverlap(blockStart: Long, blockEnd: Long, questStart: Long, questEnd: Long): Long {
        val actualStart = max(blockStart, questStart)
        val actualEnd = min(blockEnd, questEnd)
        return if (actualEnd > actualStart) actualEnd - actualStart else 0L
    }

    private fun isQuestStartedToday(startTime: Long): Boolean {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = startTime
        return (calendar.get(Calendar.YEAR) == todayYear && calendar.get(Calendar.DAY_OF_YEAR) == todayDay)
    }
}
