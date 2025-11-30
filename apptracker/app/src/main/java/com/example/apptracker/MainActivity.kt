 package com.example.apptracker

import android.app.AppOpsManager
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

// 👇 [수정됨] 같은 패키지 안에 있는 파일들은 import가 필요 없어서 삭제했습니다.

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    startDestination = "nickname_setup"
                ) {
                    composable("nickname_setup") {
                        NicknameSetupScreen(navController)
                    }
                    composable("dashboard") {
                        DashboardScreen(navController)
                    }
                    composable("quest") {
                        QuestScreen(navController)
                    }
                    composable("ranking") {
                        RankingScreen(navController)
                    }
                    composable("quest_create") {
                        QuestCreateScreen(navController)
                    }
                }
            }
        }
    }

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