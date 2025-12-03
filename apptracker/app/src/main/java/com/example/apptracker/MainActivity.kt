package com.example.apptracker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

        // 자정 정산 예약 실행
        scheduleDailySettlement(this)

        setContent {
            // 🔥 테마 관련 제거 → 기본 MaterialTheme만 유지
            MaterialTheme {

                val navController = rememberNavController()

                val items = listOf(
                    Screen.Dashboard,
                    Screen.Quest,
                    Screen.Multiplayer,
                    Screen.Ranking
                )

                Scaffold(
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        // 닉네임 설정 화면 제외
                        if (currentDestination?.route != "nickname_setup") {
                            NavigationBar {
                                items.forEach { screen ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = null) },
                                        label = { Text(screen.label) },
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = "nickname_setup",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("nickname_setup") { NicknameSetupScreen(navController) }
                        composable("dashboard") { DashboardScreen(navController) }
                        composable("quest") { QuestScreen(navController) }
                        composable("ranking") { RankingScreen(navController) }
                        composable("quest_create") { QuestCreateScreen(navController) }
                        composable("multiplayer_lobby") { MultiplayerLobbyScreen(navController) }
                        composable("game_room/{roomId}") { backStackEntry ->
                            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                            GameRoomScreen(navController, roomId)
                        }
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
        } catch (_: Exception) { true }
    }

    private fun scheduleDailySettlement(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val timeDiff = midnight.timeInMillis - now.timeInMillis

        val dailyRequest = PeriodicWorkRequestBuilder<DailySettleWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag("daily_settle_work")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailySettleWork",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "홈", Icons.Default.Home)
    object Quest : Screen("quest", "퀘스트", Icons.Default.List)
    object Multiplayer : Screen("multiplayer_lobby", "멀티", Icons.Default.Person)
    object Ranking : Screen("ranking", "랭킹", Icons.Default.Star)
}
