// 사용시간 수집 - firebase에 데이터 업로드

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class UsageWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        if (!hasUsagePermission()) return Result.failure()

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 // 최근 1시간

        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val usageMap = usageStatsList
            .filter { it.totalTimeInForeground > 0 }
            .associate {
                it.packageName to mapOf(
                    "usedTimeMillis" to it.totalTimeInForeground,
                    "lastTimeUsed" to it.lastTimeUsed
                )
            }

        val key = getTodayKey()
        Firebase.database.getReference("usageStats")
            .child(key)
            .setValue(usageMap)

        Log.d("WORKER_USAGE", "백그라운드 업로드 완료: $usageMap")
        return Result.success()
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = applicationContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            applicationContext.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getTodayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }
}
