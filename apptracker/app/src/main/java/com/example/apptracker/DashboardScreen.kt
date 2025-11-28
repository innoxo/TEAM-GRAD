package com.example.apptracker

import android.app.Application
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController) {

    val context = LocalContext.current
    val app = context.applicationContext as Application

    val viewModel: UsageViewModel = viewModel(
        factory = UsageViewModelFactory(app)
    )

    LaunchedEffect(Unit) {
        viewModel.loadUsageData()
    }

    val categoryMinutes = viewModel.categoryMinutes
    val categoryApps = viewModel.categoryApps
    val totalUsage = viewModel.totalUsage

    var showSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF00462A))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Text("AppTracker", color = ComposeColor.White, style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(10.dp))
            Text("오늘 총 사용시간: ${totalUsage}분", color = ComposeColor.White)

            Spacer(Modifier.height(20.dp))
            Text("카테고리 비율", color = ComposeColor.White)

            Spacer(Modifier.height(12.dp))

            // -----------------------------
            // PIE CHART
            // -----------------------------
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                factory = { ctx ->
                    PieChart(ctx).apply {
                        description.isEnabled = false
                        setHoleColor(Color.TRANSPARENT)
                        setEntryLabelColor(Color.WHITE)
                        legend.textColor = Color.WHITE
                    }
                },
                update = { chart ->
                    if (categoryMinutes.isNotEmpty()) {
                        val entries = categoryMinutes.map { (cat, min) ->
                            PieEntry(min.toFloat(), cat)
                        }

                        val dataSet = PieDataSet(entries, "").apply {
                            colors = listOf(
                                Color.parseColor("#4CAF50"), // 공부
                                Color.parseColor("#03A9F4"), // SNS
                                Color.parseColor("#F44336"), // 엔터테인먼트
                                Color.parseColor("#FFC107"), // 생산
                                Color.parseColor("#9E9E9E")  // 기타
                            )
                            valueTextColor = Color.WHITE
                            valueTextSize = 14f
                        }

                        chart.data = PieData(dataSet)
                        chart.invalidate()

                        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                val pie = e as? PieEntry ?: return
                                selectedCategory = pie.label
                                showSheet = true
                            }
                            override fun onNothingSelected() {}
                        })
                    }
                }
            )
        }

        // ----------------------------
        // 버튼 두 개
        // ----------------------------
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { navController.navigate("quest") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White)
            ) {
                Text("퀘스트 보기", color = ComposeColor.Black)
            }

            Button(
                onClick = { navController.navigate("ranking") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White)
            ) {
                Text("랭킹 보기", color = ComposeColor.Black)
            }
        }
    }

    // ----------------------------
    // BottomSheet (카테고리 상세)
    // ----------------------------
    if (showSheet && selectedCategory != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = ComposeColor(0xFF00462A)
        ) {
            CategoryDetailSheet(
                category = selectedCategory!!,
                apps = categoryApps[selectedCategory] ?: emptyList(),
                onClose = { showSheet = false }
            )
        }
    }
}