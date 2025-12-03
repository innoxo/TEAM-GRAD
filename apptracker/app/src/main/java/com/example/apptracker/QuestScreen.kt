package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // 🔥 [추가됨] 둥근 모서리
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset // 🔥 [추가됨] 탭 표시기
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // 🔥 [추가됨] 폰트 크기 단위
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay // 딜레이 함수 사용을 위해 필요할 수 있음
// 뒤로가기 버튼 디자인 개선을 위한 추가
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon

// 디자인 테마 색상
private val PrimaryColor = Color(0xFF00695C)
private val BackgroundColor = Color(0xFFF5F7F6)

@Composable
fun QuestScreen(
    navController: NavHostController,
    vm: QuestViewModel = viewModel()
) {
    // 화면 진입 시 데이터 새로고침
    LaunchedEffect(Unit) { vm.refresh() }

    // 2초마다 진행률 갱신
    LaunchedEffect(Unit) {
        while (true) {
            vm.updateProgress()
            kotlinx.coroutines.delay(2000)
        }
    }

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("진행 중", "완료됨")
    val currentNickname = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "Guest"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor) // 밝은 배경
            .padding(16.dp)
    ) {
        // 1. 상단바
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.Gray
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("나의 퀘스트", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("로그인: $currentNickname", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { navController.navigate("quest_create") },
                colors = ButtonDefaults.buttonColors(PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) { Text("+ 만들기", color = Color.White) }
        }

        Spacer(Modifier.height(20.dp))

        // 2. 탭 메뉴
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = BackgroundColor,
            contentColor = PrimaryColor,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = PrimaryColor
                )
            }
        ) {
            tabs.forEachIndexed { index, text ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = {
                        Text(
                            text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 3. 리스트 표시
        if (tabIndex == 0) {
            if (vm.activeQuests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("진행 중인 퀘스트가 없습니다.\n새로운 도전을 시작해보세요!", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                // 스크롤 가능하도록 LazyColumn 사용
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vm.activeQuests.size) { index ->
                        val q = vm.activeQuests[index]
                        QuestCard(
                            quest = q,
                            onComplete = { vm.markCompleted(q) },
                            onCancel = { vm.cancelQuest(q) }
                        )
                    }
                }
            }
        } else {
            if (vm.completedQuests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("완료된 퀘스트가 없습니다.", color = Color.Gray)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vm.completedQuests.size) { index ->
                        val q = vm.completedQuests[index]
                        CompletedQuestCard(quest = q, onDelete = { vm.deleteCompleted(q.id) })
                    }
                }
            }
        }
    }
}