package com.example.apptracker

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

        Spacer(Modifier.height(20.dp))

        Text("퀘스트 생성", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        Text("앱 선택", color = Color.White)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .height(180.dp)
                .background(Color(0xFF003A20))
                .padding(6.dp)
        ) {
            items(appList.value) { appItem ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .background(
                            if (selected.value == appItem) Color(0xFF1B5E20) else Color.Transparent
                        )
                        .padding(8.dp)
                        .clickable { vm.selectApp(appItem) }
                ) {
                    Text(appItem.appName, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("조건", color = Color.White)

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = condition.value == "≤",
                onClick = { vm.setCondition("≤") }
            )
            Text("이하", color = Color.White)

            Spacer(Modifier.width(20.dp))

            RadioButton(
                selected = condition.value == "≥",
                onClick = { vm.setCondition("≥") }
            )
            Text("이상", color = Color.White)
        }

        Spacer(Modifier.height(20.dp))

        Text("목표 시간 (분)", color = Color.White)
        OutlinedTextField(
            value = minutes.value.toString(),
            onValueChange = { vm.setTargetMinutes(it.toIntOrNull() ?: 0) },
            modifier = Modifier.fillMaxWidth().background(Color.White)
        )

        Spacer(Modifier.height(20.dp))

        Text("퀘스트 시작 시간 (0~24)", color = Color.White)
        OutlinedTextField(
            value = startHour.value.toString(),
            onValueChange = { vm.setStartHour(it.toIntOrNull() ?: 0) },
            modifier = Modifier.fillMaxWidth().background(Color.White)
        )

        Spacer(Modifier.height(16.dp))

        Text("퀘스트 종료 시간 (0~24)", color = Color.White)
        OutlinedTextField(
            value = endHour.value.toString(),
            onValueChange = { vm.setEndHour(it.toIntOrNull() ?: 0) },
            modifier = Modifier.fillMaxWidth().background(Color.White)
        )

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
