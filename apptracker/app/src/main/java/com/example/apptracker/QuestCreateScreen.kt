package com.example.apptracker

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun QuestCreateScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val vm: QuestCreateViewModel = viewModel(
        factory = QuestCreateViewModelFactory(app)
    )

    LaunchedEffect(Unit) { vm.loadInstalledApps() }

    val appList = vm.appList.collectAsState()
    val recommendedApps = vm.recommendedApps.collectAsState() // ✨ 추천 앱 상태
    val selected = vm.selectedApp.collectAsState()
    val condition = vm.conditionType.collectAsState()
    val minutes = vm.targetMinutes.collectAsState()
    val startHour = vm.startHour.collectAsState()
    val endHour = vm.endHour.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00462A))
            .padding(16.dp)
    ) {
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text("뒤로가기", color = Color.Black)
        }

        Spacer(Modifier.height(10.dp))
        Text("퀘스트 생성", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        // ✨ [추가] AI 추천 섹션
        if (recommendedApps.value.isNotEmpty()) {
            Text("🤖 AI 맞춤 추천 (최근 활동 기반)", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recommendedApps.value) { recApp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.clickable { vm.selectApp(recApp) }
                    ) {
                        Text(
                            text = recApp.appName,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color(0xFF00462A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Text("전체 앱 선택", color = Color.White)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .height(150.dp) // 높이 약간 조절
                .background(Color(0xFF003A20))
                .padding(6.dp)
        ) {
            items(appList.value) { appItem ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(2.dp)
                        .background(
                            if (selected.value == appItem) Color(0xFF1B5E20) else Color.Transparent
                        )
                        .padding(8.dp)
                        .clickable { vm.selectApp(appItem) }
                ) {
                    Text(appItem.appName, color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // ... 이하 기존 UI 코드 유지 ...
        Spacer(Modifier.height(20.dp))
        Text("조건", color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = condition.value == "≤", onClick = { vm.setCondition("≤") })
            Text("이하", color = Color.White)
            Spacer(Modifier.width(20.dp))
            RadioButton(selected = condition.value == "≥", onClick = { vm.setCondition("≥") })
            Text("이상", color = Color.White)
        }

        Spacer(Modifier.height(10.dp))
        Text("목표 시간 (분)", color = Color.White)
        OutlinedTextField(
            value = minutes.value.toString(),
            onValueChange = { vm.setTargetMinutes(it.toIntOrNull() ?: 0) },
            modifier = Modifier.fillMaxWidth().background(Color.White)
        )

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("시작 (0~23)", color = Color.White)
                OutlinedTextField(
                    value = startHour.value.toString(),
                    onValueChange = { vm.setStartHour(it.toIntOrNull() ?: 0) },
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("종료 (0~23)", color = Color.White)
                OutlinedTextField(
                    value = endHour.value.toString(),
                    onValueChange = { vm.setEndHour(it.toIntOrNull() ?: 0) },
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                vm.createQuest()
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text("퀘스트 생성", color = Color.Black)
        }
    }
}