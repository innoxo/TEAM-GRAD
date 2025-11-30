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

// 👇 같은 패키지 안에 있는 파일들은 import가 필요 없어서 삭제했습니다.

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 권한 체크: 앱 사용 기록 접근 권한이 없으면 설정 화면으로 이동
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
                    // 1. 닉네임 설정 화면
                    composable("nickname_setup") {
                        NicknameSetupScreen(navController)
                    }

                    // 2. 대시보드 (메인)
                    composable("dashboard") {
                        DashboardScreen(navController)
                    }

                    // 3. 퀘스트 목록
                    composable("quest") {
                        QuestScreen(navController)
                    }

                    // 4. 랭킹 화면
                    composable("ranking") {
                        RankingScreen(navController)
                    }

                    // 5. 퀘스트 생성
                    composable("quest_create") {
                        QuestCreateScreen(navController)
                    }

                    // 🔥 [추가됨] 6. 멀티플레이 로비
                    composable("multiplayer_lobby") {
                        MultiplayerLobbyScreen(navController)
                    }

                    // 🔥 [추가됨] 7. 멀티플레이 게임방 (대기실)
                    composable("game_room/{roomId}") { backStackEntry ->
                        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                        GameRoomScreen(navController, roomId)
                    }
                }
            }
        }
    }

    // 앱 사용 기록 접근 권한이 있는지 확인하는 함수
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