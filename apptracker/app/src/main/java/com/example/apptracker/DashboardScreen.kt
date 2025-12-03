package com.example.apptracker

import android.app.Application
import android.content.Context
import android.graphics.Color // 차트용 컬러
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
import androidx.compose.ui.graphics.Color as ComposeColor // UI용 컬러
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // 타이틀 그라데이션 색상
    val LimeGreen = ComposeColor(0xFF32CD32)
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(ComposeColor.White, LimeGreen)
    )

    // 🔥 [요청하신 배경색 적용]
    val BgColor = ComposeColor(0xFF81B184)

    Scaffold(
        containerColor = BgColor, // 🔥 배경색: 0xFF81B184
        bottomBar = {
            // 하단 메뉴 (MainActivity와 연결됨, 화면상 공간 확보용)
            Spacer(Modifier.height(0.dp))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // 1. 상단 헤더 (타이틀 + 로그아웃)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Play&Focus",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = gradientBrush,
                        fontWeight = FontWeight.Bold
                    )
                )

                // 🔥 [로그아웃 버튼]
                TextButton(
                    onClick = {
                        val prefs = context.getSharedPreferences("AppTrackerPrefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        UserSession.nickname = ""
                        navController.navigate("nickname_setup") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                ) {
                    Text("로그아웃", color = ComposeColor.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 🔥 [복구됨] 2. 오늘 총 사용 시간 카드
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

            // 3. AI 분석 카드
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

            // 4. 퀘스트 추천 카드
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

            // 5. 차트 영역
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
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    e?.let { selectedCategory = (it as PieEntry).label; showSheet = true }
                                }
                                override fun onNothingSelected() {}
                            })
                        }
                    }
                )
            }

            // 🔥 [확인] 중복 버튼 3개(퀘스트/멀티/랭킹)는 여기에서 삭제되었습니다.

            Spacer(Modifier.height(80.dp)) // 하단 바 공간 확보
        }
    }

    // 바텀시트
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