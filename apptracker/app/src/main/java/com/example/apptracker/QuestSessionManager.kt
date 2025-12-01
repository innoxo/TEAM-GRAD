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

            // 1. 조회 범위는 넓게 잡습니다. (이미 켜져있는 앱을 감지하기 위해)
            // 퀘스트 시작 시간보다 24시간 전부터 조회를 시작합니다.
            val searchStart = start - (1000 * 60 * 60 * 24)
            val events = usage.queryEvents(searchStart, end)
            val event = UsageEvents.Event()

            var totalTime = 0L
            var lastStartTime = 0L // 앱이 켜진 시점

            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                if (event.packageName == pkg) {

                    // 앱이 켜졌을 때 (또는 상호작용 중일 때)
                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                        event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastStartTime = event.timeStamp
                    }

                    // 앱이 꺼졌을 때
                    else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                        event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {

                        if (lastStartTime > 0) {
                            // 🔥 [핵심 로직]
                            // 앱 켜진 시간(lastStartTime)과 퀘스트 시작 시간(start) 중 **더 늦은 것**을 기준으로 삼습니다.
                            // 즉, 아침 9시에 켰어도 퀘스트가 19시에 시작했으면 19시부터 계산합니다.
                            val activeStart = max(lastStartTime, start)
                            val activeEnd = min(event.timeStamp, end)

                            // 유효한 구간(퀘스트 범위 내)이 있다면 더하기
                            if (activeEnd > activeStart) {
                                totalTime += (activeEnd - activeStart)
                            }
                            lastStartTime = 0
                        }
                    }
                }
            }

            // 2. [현재 진행 중] 아직 앱을 안 끄고 보고 있는 경우 처리
            if (lastStartTime > 0) {
                // 마찬가지로 퀘스트 시작 시간 이후만 계산
                val activeStart = max(lastStartTime, start)
                val activeEnd = min(end, System.currentTimeMillis())

                if (activeEnd > activeStart) {
                    totalTime += (activeEnd - activeStart)
                }
            }

            // 밀리초 -> 분 변환
            (totalTime / 60000L).toInt()
        }
}