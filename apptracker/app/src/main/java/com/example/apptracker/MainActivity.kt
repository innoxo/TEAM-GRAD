package com.example.apptracker

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FIREBASE_UPLOAD", "🔥 MainActivity 실행됨")

        if (!hasUsagePermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            uploadUsageStatsToFirebase()
        }
    }

    // 권한 확인
    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // Firebase로 사용 기록 업로드
    private fun uploadUsageStatsToFirebase() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 * 6 // 최근 6시간

        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val usageMap = mutableMapOf<String, Any>()

        for (usage in usageStatsList) {
            if (usage.totalTimeInForeground > 0) {
                val originalKey = usage.packageName
                val key = safeKey(originalKey)

                usageMap[key] = mapOf(
                    "usedTimeMillis" to usage.totalTimeInForeground,
                    "lastTimeUsed" to usage.lastTimeUsed
                )
            }
        }

        val todayKey = getTodayKey()
        val userID = getUserID()
        val firebaseRef = Firebase.database.getReference("usageStats")
            .child(todayKey)
            .child(userID) // ✅ 사용자 단위 저장

        Log.d("FIREBASE_UPLOAD", "📦 수집된 usageMap: $usageMap")
        firebaseRef.setValue(usageMap).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("FIREBASE_UPLOAD", "✅ 업로드 성공: $usageMap")
            } else {
                Log.e("FIREBASE_UPLOAD", "❌ 업로드 실패: ${it.exception}")
            }
        }
    }

    private fun getTodayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    // ✅ 기기 고유 ID를 유저 ID로 사용
    private fun getUserID(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_user"
    }

    // ❗ Firebase 키로 안전하게 변환
    private fun safeKey(rawKey: String): String {
        return rawKey
            .replace(".", "_")
            .replace("$", "_")
            .replace("#", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
    }
}


