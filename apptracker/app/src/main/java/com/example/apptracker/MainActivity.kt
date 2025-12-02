package com.example.apptracker

import androidx.work.*  // 스케줄링 반영을 위해 추가
import java.util.concurrent.TimeUnit
import java.util.Calendar
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

        // 추가: 앱이 켜질 때 "자정 정산" 예약됨.
        scheduleDailySettlement(this)

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

    // 추가: 매일 자정에 실행되도록 예약하는 함
    private fun scheduleDailySettlement(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 조건: 네트워크가 연결되어 있을 때만 실행 (Firebase 저장을 위해)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 자정까지 남은 시간 계산하는 파트
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // 다음 날 00:00
        }
        val timeDiff = midnight.timeInMillis - now.timeInMillis

        // 24시간마다 반복되는 작업 생성 (자정 이후 진행됨)
        val dailyRequest = PeriodicWorkRequestBuilder<DailySettleWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag("daily_settle_work") // 태그 생성
            .build()

        // 예약 등록 (UniqueWork: 이미 예약돼 있으면 덮어쓰지 않고 유지함 -> 중복 실행 방지)
        workManager.enqueueUniquePeriodicWork(
            "DailySettleWork",           // 고유 이름
            ExistingPeriodicWorkPolicy.KEEP, // 이미 있으면 유지(KEEP)
            dailyRequest
        )
    }
}