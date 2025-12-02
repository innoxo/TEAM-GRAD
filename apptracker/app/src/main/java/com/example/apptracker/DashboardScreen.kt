package com.example.apptracker

import android.app.Application
import android.graphics.Color // 🔥 차트용 컬러 (안드로이드 기본)
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor // 🔥 UI용 컬러 (Compose, 별칭 사용)
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
// 🔥 차트 데이터 관련 import 필수!
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
    val viewModel: UsageViewModel = viewModel(factory = UsageViewModelFactory(app))

    LaunchedEffect(Unit) { viewModel.loadUsageData() }

    val categoryMinutes = viewModel.categoryMinutes
    val categoryApps = viewModel.categoryApps
    val totalUsage = viewModel.totalUsage
    val aiSummary = viewModel.dailySummary.value
    val aiQuestRec = viewModel.questRecommendation.value

    var showSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    // UI 디자인용 색상
    val LimeGreen = ComposeColor(0xFF32CD32)
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(ComposeColor.White, LimeGreen)
    )

    Scaffold(
        containerColor = ComposeColor(0xFF00462A),
        bottomBar = {
            // 하단 메뉴
            Surface(
                shadowElevation = 16.dp,
                color = ComposeColor.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("quest") },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFFF0F4F3)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("🔥 퀘스트", color = ComposeColor(0xFF00695C), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { navController.navigate("multiplayer_lobby") },
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF00695C)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("🤝 멀티플레이", color = ComposeColor.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { navController.navigate("ranking") },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFFF0F4F3)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("🏆 랭킹", color = ComposeColor(0xFF00695C), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // 타이틀
            Text(
                text = "AppTracker",
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = gradientBrush,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(24.dp))

            // 1. 총 사용 시간
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("오늘 총 사용 시간", color = ComposeColor.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    val hours = totalUsage / 60
                    val mins = totalUsage % 60
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$hours", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = ComposeColor(0xFF191C1B))
                        Text("시간", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ComposeColor(0xFF191C1B), modifier = Modifier.padding(bottom = 8.dp, start = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("$mins", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = ComposeColor(0xFF191C1B))
                        Text("분", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ComposeColor(0xFF191C1B), modifier = Modifier.padding(bottom = 8.dp, start = 2.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. AI 분석
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFE8F5E9)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("🤖 AI 분석", color = ComposeColor(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(aiSummary, color = ComposeColor.Black, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 3. 퀘스트 추천
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFFFF3E0)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("🎯 다음 퀘스트 추천", color = ComposeColor(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(aiQuestRec, color = ComposeColor.Black, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 4. 차트
            Text("카테고리별 비율", color = ComposeColor.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(300.dp).shadow(2.dp, RoundedCornerShape(24.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    factory = { ctx ->
                        PieChart(ctx).apply {
                            description.isEnabled = false
                            // 🔥 Color.TRANSPARENT 등은 android.graphics.Color를 씁니다.
                            setHoleColor(Color.TRANSPARENT)
                            setEntryLabelColor(Color.DKGRAY)
                            setEntryLabelTextSize(10f)
                            legend.isEnabled = false
                            animateY(1000)
                        }
                    },
                    update = { chart ->
                        if (categoryMinutes.isNotEmpty()) {
                            val entries = categoryMinutes.filter { it.value > 0 }.map { PieEntry(it.value.toFloat(), it.key) }
                            val dataSet = PieDataSet(entries, "").apply {
                                // 🔥 parseColor도 android.graphics.Color 기능입니다.
                                colors = listOf(
                                    Color.parseColor("#26A69A"), Color.parseColor("#42A5F5"),
                                    Color.parseColor("#FFA726"), Color.parseColor("#EF5350"), Color.parseColor("#78909C")
                                )
                                valueTextColor = Color.WHITE
                                valueTextSize = 14f
                                sliceSpace = 2f
                            }
                            chart.data = PieData(dataSet)
                            chart.invalidate()

                            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                // 🔥 여기서 e는 Entry? 타입입니다.
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    val pieEntry = e as? PieEntry ?: return
                                    selectedCategory = pieEntry.label
                                    showSheet = true
                                }
                                override fun onNothingSelected() {}
                            })
                        }
                    }
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showSheet && selectedCategory != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = ComposeColor.White
        ) {
            CategoryDetailSheet(
                category = selectedCategory!!,
                apps = categoryApps[selectedCategory] ?: emptyList(),
                onClose = { showSheet = false }
            )
        }
    }
}