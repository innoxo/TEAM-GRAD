package com.example.apptracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class DailySettleWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 닉네임 불러옴 (SharedPreferences)
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val nickname = prefs.getString("saved_nickname", "") ?: ""

            if (nickname.isBlank()) {
                return Result.failure()
            }

            // 날짜 범위 설정 (00:00:00.000 ~ 23:59:59.999)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1) 

            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            val dateKey = sdf.format(Date(startOfDay))

            // 로그 누락 방지 - '정밀 이벤트 기반' 측정
            // ueryEvents 사용
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = usageStatsManager.queryEvents(startOfDay, endOfDay)
            
            val appUsageMap = mutableMapOf<String, Long>() // 패키지명 -> 누적 시간(ms)
            val startMap = mutableMapOf<String, Long>()    // 패키지명 -> 켜진 시간(ms)

            val event = UsageEvents.Event()

            // 모든 이벤트를 순회하며 시간을 직접 계산
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName

                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || 
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    // 앱이 켜짐: 시작 시간 기록
                    startMap[pkg] = event.timeStamp
                } 
                else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND || 
                         event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                    // 앱이 꺼짐: (꺼진 시간 - 켜진 시간)을 누적
                    val startTime = startMap[pkg]
                    if (startTime != null) {
                        val duration = event.timeStamp - startTime
                        if (duration > 0) {
                            appUsageMap[pkg] = appUsageMap.getOrDefault(pkg, 0L) + duration
                        }
                        startMap.remove(pkg) // 계산 완료 후 제거
                    }
                }
            }

            // (엣지 케이스 처리) 자정이 넘어가도록 앱을 켜두고 있었다면, 자정(endOfDay)까지의 시간을 더해줌
            startMap.forEach { (pkg, startTime) ->
                if (endOfDay > startTime) {
                    appUsageMap[pkg] = appUsageMap.getOrDefault(pkg, 0L) + (endOfDay - startTime)
                }
            }

            // 전체 사용 시간(분) 계산
            val totalMinutes = appUsageMap.values.sum() / 60000L

            // Firebase 저장
            val db = FirebaseDatabase.getInstance(
                "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
            ).reference

            val reportData = mapOf(
                "totalUsageMinutes" to totalMinutes,
                "date" to dateKey,
                "method" to "precise_event_tracking", // 정밀 측정임을 표시
                "processedAt" to System.currentTimeMillis()
            )

            db.child("users").child(nickname)
                .child("daily_reports").child(dateKey)
                .setValue(reportData).await()

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}