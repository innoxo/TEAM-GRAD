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

    suspend fun measureAppUsage(start: Long, end: Long, pkg: String): Int =
        withContext(Dispatchers.IO) {

            // 1. 조회 범위: 24시간 전부터 (이미 켜져있는 앱 감지용)
            val searchStart = start - (1000 * 60 * 60 * 24)
            val events = usage.queryEvents(searchStart, end)
            val event = UsageEvents.Event()

            var totalTime = 0L
            var lastEventTime = searchStart
            var currentForegroundPackage: String? = null

            // 2. 타임라인 스캔 (시간 순서대로 훑기)
            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                // 시간이 흘렀고, 직전까지 '내 앱'이 켜져 있었다면?
                if (event.timeStamp > lastEventTime) {
                    if (currentForegroundPackage == pkg) {
                        // 🔥 [핵심] '이벤트 발생 시간'과 '퀘스트 범위'가 겹치는 부분만 잘라냄
                        val duration = calculateOverlap(
                            blockStart = lastEventTime,
                            blockEnd = event.timeStamp,
                            questStart = start, // 20:01
                            questEnd = end      // 22:00
                        )
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

            // 3. [현재 진행 중] 아직 안 끄고 보고 있는 시간 계산
            if (currentForegroundPackage == pkg) {
                val duration = calculateOverlap(
                    blockStart = lastEventTime,
                    blockEnd = System.currentTimeMillis(), // 현재 시간까지
                    questStart = start, // 20:01
                    questEnd = end      // 22:00
                )
                totalTime += duration
            }

            // 분 단위 반환
            (totalTime / 60000L).toInt()
        }

    // 🔥 겹치는 시간 계산기 (수학적으로 겹치는 부분만 남김)
    private fun calculateOverlap(blockStart: Long, blockEnd: Long, questStart: Long, questEnd: Long): Long {
        // 시작점: (앱 켠 시간) vs (퀘스트 시작 시간) 중 더 늦은 거
        val actualStart = max(blockStart, questStart)
        // 끝점: (앱 끈 시간) vs (퀘스트 종료 시간) 중 더 빠른 거
        val actualEnd = min(blockEnd, questEnd)

        // 유효한 구간(양수)이면 반환, 아니면 0
        return if (actualEnd > actualStart) {
            actualEnd - actualStart
        } else {
            0L
        }
    }
}