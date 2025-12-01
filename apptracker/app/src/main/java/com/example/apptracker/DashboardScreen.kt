package com.example.apptracker

import android.app.Application
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController) {

    val context = LocalContext.current
    val app = context.applicationContext as Application

    val viewModel: UsageViewModel = viewModel(factory = UsageViewModelFactory(app))

    LaunchedEffect(Unit) {
        viewModel.loadUsageData()
    }

    val categoryMinutes = viewModel.categoryMinutes
    val categoryApps = viewModel.categoryApps
    val totalUsage = viewModel.totalUsage

    // 두 가지 AI 멘트 가져오기
    val aiSummary = viewModel.dailySummary.value
    val aiQuestRec = viewModel.questRecommendation.value // 🔥 추가됨

    var showSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(ComposeColor(0xFF00462A)).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("AppTracker", color = ComposeColor.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // 1. 오늘의 한 줄 요약
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("🤖 AI 분석", color = ComposeColor(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(aiSummary, color = ComposeColor.Black, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 🔥 2. [추가됨] 퀘스트 추천 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFFFF3E0)), // 연한 주황색
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("🎯 다음 퀘스트 추천", color = ComposeColor(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(aiQuestRec, color = ComposeColor.Black, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("오늘 총 사용시간: ${totalUsage}분", color = ComposeColor.White, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            // 차트 (기존 유지)
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(200.dp), // 공간 확보를 위해 높이 조금 줄임
                factory = { ctx ->
                    PieChart(ctx).apply {
                        description.isEnabled = false
                        setHoleColor(Color.TRANSPARENT)
                        setEntryLabelColor(Color.WHITE)
                        legend.textColor = Color.WHITE
                        legend.isEnabled = false
                    }
                },
                update = { chart ->
                    if (categoryMinutes.isNotEmpty()) {
                        val entries = categoryMinutes.map { PieEntry(it.value.toFloat(), it.key) }
                        val dataSet = PieDataSet(entries, "").apply {
                            colors = listOf(Color.parseColor("#66BB6A"), Color.parseColor("#42A5F5"), Color.parseColor("#EF5350"), Color.parseColor("#FFCA28"), Color.parseColor("#BDBDBD"))
                            valueTextColor = Color.WHITE
                            valueTextSize = 14f
                        }
                        chart.data = PieData(dataSet)
                        chart.invalidate()
                        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                e?.let { selectedCategory = (it as PieEntry).label; showSheet = true }
                            }
                            override fun onNothingSelected() {}
                        })
                    }
                }
            )
        }

        // 하단 버튼들 (기존 유지)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navController.navigate("quest") }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White), shape = RoundedCornerShape(12.dp)) { Text("퀘스트", color = ComposeColor.Black, fontWeight = FontWeight.Bold) }
                Button(onClick = { navController.navigate("ranking") }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White), shape = RoundedCornerShape(12.dp)) { Text("랭킹", color = ComposeColor.Black, fontWeight = FontWeight.Bold) }
            }
            Button(onClick = { navController.navigate("multiplayer_lobby") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFFE8F5E9)), shape = RoundedCornerShape(12.dp)) { Text("🤝 멀티플레이 (협력/경쟁)", color = ComposeColor(0xFF2E7D32), fontWeight = FontWeight.Bold) }
        }
    }

    if (showSheet && selectedCategory != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState, containerColor = ComposeColor(0xFF00462A)) {
            CategoryDetailSheet(category = selectedCategory!!, apps = categoryApps[selectedCategory] ?: emptyList(), onClose = { showSheet = false })
        }
    }
}