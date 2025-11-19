package com.example.apptracker

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    private lateinit var usageStatsManager: UsageStatsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        if (!hasUsageAccess()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
            return
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {
                    composable("dashboard") {
                        DashboardScreen(navController)
                    }
                    composable("quest") {
                        QuestScreen(navController)
                    }
                    composable("ranking") {
                        RankingScreen(navController)
                    }

                    // 🔥 반드시 추가해야 하는 라우트 (여기 없어서 앱이 계속 죽은 것)
                    composable("quest_create") {
                        QuestCreateScreen(navController)
                    }
                }
            }
        }
    }

    /** 앱 사용량 권한 확인 */
    private fun hasUsageAccess(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            true
        }
    }
}
