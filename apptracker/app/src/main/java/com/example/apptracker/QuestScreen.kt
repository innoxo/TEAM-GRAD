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
    LaunchedEffect(Unit) { vm.refresh() }

    LaunchedEffect(Unit) {
        while (true) {
            vm.updateProgress()
            kotlinx.coroutines.delay(2000)
        }
    }

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("진행 중", "완료됨")

    // 현재 접속 닉네임 확인용
    val currentNickname = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "demo_user"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00462A))
            .padding(16.dp)
    ) {
        // 🔥 [디버깅용] 현재 닉네임 표시 (이게 demo_user인지 님 닉네임인지 확인하세요!)
        Text(
            text = "현재 로그인: $currentNickname",
            color = Color.Yellow,
            style = MaterialTheme.typography.bodySmall
        )

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
            ActiveQuestList(vm)
        } else {
            CompletedQuestList(vm)
        }
    }
}

@Composable
fun ActiveQuestList(vm: QuestViewModel) {
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
}

@Composable
fun CompletedQuestList(vm: QuestViewModel) {
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