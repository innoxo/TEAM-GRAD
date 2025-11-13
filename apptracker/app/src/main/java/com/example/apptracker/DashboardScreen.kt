package com.example.apptracker

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun DashboardScreen(navController: NavHostController) {

    val context = LocalContext.current

    var categoryMinutes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalUsage by remember { mutableStateOf(0) }
    var totalPoints by remember { mutableStateOf(0) }

    // ✔ 테스트용 데이터
    LaunchedEffect(Unit) {
        categoryMinutes = mapOf(
            "공부" to 40,
            "SNS" to 15,
            "엔터테인먼트" to 17
        )
        totalUsage = 72
        totalPoints = 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF00462A))   // ← 배경색 변경
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "AppTracker",
                color = ComposeColor.White,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(8.dp))
            Text("오늘 총 사용시간: ${totalUsage}분", color = ComposeColor.LightGray)
            Text("포인트: ${totalPoints}점", color = ComposeColor.LightGray)

            Spacer(Modifier.height(30.dp))

            Text("카테고리 비율", color = ComposeColor.White)

            Spacer(Modifier.height(10.dp))

            // 원형 차트
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                factory = { ctx ->
                    PieChart(ctx).apply {
                        this.setUsePercentValues(false)
                        this.description.isEnabled = false
                        this.setHoleColor(Color.TRANSPARENT)
                        this.setEntryLabelColor(Color.WHITE)
                        this.legend.textColor = Color.WHITE
                    }
                },
                update = { chart ->
                    if (categoryMinutes.isNotEmpty()) {
                        val entries = categoryMinutes.map { (cat, value) ->
                            PieEntry(value.toFloat(), cat)
                        }

                        val dataSet = PieDataSet(entries, "").apply {
                            colors = listOf(
                                Color.parseColor("#4CAF50"),
                                Color.parseColor("#03A9F4"),
                                Color.parseColor("#F44336"),
                                Color.parseColor("#9C27B0"),
                                Color.parseColor("#FF9800")
                            )
                            valueTextColor = Color.WHITE
                            valueTextSize = 14f
                        }

                        chart.data = PieData(dataSet)
                        chart.invalidate()
                    }
                }
            )
        }

        // ───────────── 버튼 두 개 ─────────────
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Button(
                onClick = { navController.navigate("quest") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComposeColor.White    // ← 버튼 배경 흰색
                )
            ) {
                Text("퀘스트 보기", color = ComposeColor.Black)
            }

            Button(
                onClick = { navController.navigate("ranking") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComposeColor.White    // ← 버튼 배경 흰색
                )
            ) {
                Text("랭킹 보기", color = ComposeColor.Black)
            }
        }
    }
}
