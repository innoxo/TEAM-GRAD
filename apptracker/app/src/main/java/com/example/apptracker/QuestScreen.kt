package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun QuestScreen(
    navController: NavHostController,
    vm: QuestViewModel = viewModel()
) {
    // 🔥 화면 켜지면 무조건 새로고침
    LaunchedEffect(Unit) { vm.refresh() }

    LaunchedEffect(Unit) {
        while (true) {
            vm.updateProgress()
            kotlinx.coroutines.delay(2000)
        }
    }

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("진행 중", "완료됨")

    val currentNickname = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "demo_user"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00462A))
            .padding(16.dp)
    ) {
        Text("현재 로그인: $currentNickname", color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        Row {
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(Color.White)
            ) { Text("뒤로가기", color = Color.Black) }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = { navController.navigate("quest_create") },
                colors = ButtonDefaults.buttonColors(Color.White)
            ) { Text("퀘스트 만들기", color = Color.Black) }
        }

        Spacer(Modifier.height(20.dp))

        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color(0xFF00462A),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, text ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(text, color = if (tabIndex == index) Color.White else Color.Gray) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (tabIndex == 0) {
            if (vm.activeQuests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("진행 중인 퀘스트가 없습니다.", color = Color.LightGray)
                }
            } else {
                vm.activeQuests.forEach { q ->
                    QuestCard(
                        quest = q,
                        onComplete = { vm.markCompleted(q) },
                        onCancel = { vm.cancelQuest(q) }
                    )
                }
            }
        } else {
            if (vm.completedQuests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("완료된 퀘스트가 없습니다.", color = Color.LightGray)
                }
            } else {
                vm.completedQuests.forEach { q ->
                    CompletedQuestCard(
                        quest = q,
                        onDelete = { vm.deleteCompleted(q.id) }
                    )
                }
            }
        }
    }
}