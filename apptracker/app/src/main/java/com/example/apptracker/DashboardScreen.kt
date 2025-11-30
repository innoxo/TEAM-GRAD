package com.example.apptracker

import android.app.Application
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
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

    // 뷰모델 생성
    val viewModel: UsageViewModel = viewModel(
        factory = UsageViewModelFactory(app)
    )

    // 화면 진입 시 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadUsageData()
    }

    // 뷰모델 상태 관찰
    val categoryMinutes = viewModel.categoryMinutes
    val categoryApps = viewModel.categoryApps
    val totalUsage = viewModel.totalUsage

    // [중요] State 객체의 값을 바로 가져옵니다. (collectAsState 필요 없음)
    val aiSummary = viewModel.dailySummary.value

    // 바텀시트 상태 관리
    var showSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF00462A)) // 짙은 녹색 배경
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Text("AppTracker", color = ComposeColor.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(16.dp))

            // 🔥 [수정됨] 중복 제거하고 하나만 남겼습니다! (AI 한줄평)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFE8F5E9)), // 연한 초록
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 AI 분석 (오늘의 한 줄)",
                        color = ComposeColor(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = aiSummary, // GPT가 말한 내용 표시
                        color = ComposeColor.Black,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("오늘 총 사용시간: ${totalUsage}분", color = ComposeColor.White, fontSize = 18.sp)

            Spacer(Modifier.height(12.dp))

            // -----------------------------
            // PIE CHART
            // -----------------------------
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp), // 차트 높이 조정
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
                        val entries = categoryMinutes.map { (cat, min) ->
                            PieEntry(min.toFloat(), cat)
                        }

                        val dataSet = PieDataSet(entries, "").apply {
                            colors = listOf(
                                Color.parseColor("#66BB6A"), // 연두
                                Color.parseColor("#42A5F5"), // 파랑
                                Color.parseColor("#EF5350"), // 빨강
                                Color.parseColor("#FFCA28"), // 노랑
                                Color.parseColor("#BDBDBD")  // 회색
                            )
                            valueTextColor = Color.WHITE
                            valueTextSize = 14f
                            sliceSpace = 2f
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
        // 하단 버튼들 (퀘스트 / 랭킹 / 멀티플레이)
        // ----------------------------
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { navController.navigate("quest") },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("퀘스트", color = ComposeColor.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { navController.navigate("ranking") },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("랭킹", color = ComposeColor.Black, fontWeight = FontWeight.Bold)
                }
            }

            // 멀티플레이 버튼
            Button(
                onClick = { navController.navigate("multiplayer_lobby") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🤝 멀티플레이 (협력/경쟁)", color = ComposeColor(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        }
    }

    // BottomSheet
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