package com.example.apptracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuestSessionManager(private val context: Context) {

    private val usage = context.getSystemService(UsageStatsManager::class.java)

    /*
     * 정확한 앱 사용 시간 측정 로직 -> queryEvents 사용
     * 실제 화면이 켜진(Foreground) 시간만 계산 (단순하게 시간만 불러오던 부분을 수정)
     */
    suspend fun measureAppUsage(start: Long, end: Long, pkg: String): Int =
        withContext(Dispatchers.IO) {
            
            // 1. 범위 설정: 퀘스트 시작 시간 ~ 현재 시간
            val events = usage.queryEvents(start, end)
            val event = UsageEvents.Event()
            
            var totalTime = 0L
            var lastStartTime = 0L

            // 2. 이벤트 순회하며 시간 누적
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                
                // 앱의 이벤트만 필터링
                if (event.packageName != pkg) continue

                when (event.eventType) {
                    // 앱이 화면 위로 올라왔을 때 (사용 시작)
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        lastStartTime = event.timeStamp
                    }
                    
                    // 앱이 화면 뒤로 숨었을 때 (사용 종료)
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (lastStartTime != 0L) {
                            // 종료 - 시작 = 사용 시간
                            totalTime += (event.timeStamp - lastStartTime)
                            lastStartTime = 0L // 초기화
                        }
                    }
                }
            }

            // 3. 엣지 케이스 처리: 아직 앱을 사용 중인 경우 (Background 이벤트가 안 찍힘)
            // 마지막 이벤트가 Foreground였고 아직 Background가 안 왔다면, 현재 시점까지를 사용 시간으로 더함
            if (lastStartTime != 0L) {
                totalTime += (end - lastStartTime)
            }

            // 밀리초(ms) -> 분(min) 단위로 변환하여 반환
            (totalTime / 60000L).toInt()
        }
}