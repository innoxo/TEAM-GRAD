package com.example.apptracker

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuestSessionManager(private val context: Context) {

    private val usage = context.getSystemService(UsageStatsManager::class.java)

    suspend fun measureAppUsage(start: Long, end: Long, pkg: String): Int =
        withContext(Dispatchers.IO) {

            val stats = usage.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                start,
                end
            )

            val target = stats.firstOrNull { it.packageName == pkg }
            ((target?.totalTimeInForeground ?: 0L) / 60000L).toInt()
        }
}
