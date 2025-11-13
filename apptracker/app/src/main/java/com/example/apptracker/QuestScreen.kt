package com.example.apptracker

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.UUID
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/* ----------------------------------------------------
   공통 색상
----------------------------------------------------- */
object AppColors {
    val Background = Color(0xFF00462A)
    val Surface = Color.White
    val TextPrimary = Color.Black
    val TextSecondary = Color(0xFF444444)
    val Accent = Color(0xFF006644)
}

/* ----------------------------------------------------
   QuestScreen
----------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestScreen(
    navController: NavHostController,
    viewModel: QuestViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val activeQuests by remember { derivedStateOf { viewModel.activeQuests } }
    val completedQuests by remember { derivedStateOf { viewModel.completedQuests } }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("진행 중", "완료됨")

    LaunchedEffect(Unit) {
        viewModel.loadQuests(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(12.dp)
    ) {

        // 🔙 뒤로가기 버튼
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.Start),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Surface)
        ) {
            Text("뒤로가기", color = Color.Black)   // 텍스트 블랙
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "퀘스트 관리",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AppColors.Surface,
            contentColor = AppColors.TextPrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = AppColors.TextPrimary) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> QuestList(
                quests = activeQuests,
                onDelete = { quest -> viewModel.deleteQuest(context, quest) },
                onCompleteToggle = { quest -> viewModel.toggleComplete(context, quest) }
            )
            1 -> QuestList(
                quests = completedQuests,
                onDelete = { quest -> viewModel.deleteQuest(context, quest) },
                onCompleteToggle = {}
            )
        }
    }
}

/* ----------------------------------------------------
   Quest 리스트
----------------------------------------------------- */
@Composable
fun QuestList(
    quests: List<QuestItem>,
    onDelete: (QuestItem) -> Unit,
    onCompleteToggle: (QuestItem) -> Unit
) {
    if (quests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("퀘스트가 없습니다.", color = Color.White)
        }
    } else {
        LazyColumn {
            items(quests, key = { it.id }) { quest ->
                QuestCard(
                    quest = quest,
                    onDelete = { onDelete(quest) },
                    onCompleteToggle = { onCompleteToggle(quest) }
                )
            }
        }
    }
}

/* ----------------------------------------------------
   Quest Card
----------------------------------------------------- */
@Composable
fun QuestCard(
    quest: QuestItem,
    onDelete: () -> Unit,
    onCompleteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text("📱 ${quest.appName}", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text("목표: ${quest.goalType} ${quest.targetMinutes}분", color = AppColors.TextSecondary)
            Text("현재: ${quest.currentMinutes}분", color = AppColors.TextSecondary)
            Text("마감: ${quest.deadlineDate} ${quest.deadlineTime}", color = AppColors.TextSecondary)

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (quest.completed) "완료됨" else "진행중",
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    // 완료 처리 버튼
                    if (!quest.completed) {
                        Button(
                            onClick = onCompleteToggle,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                        ) {
                            Text("완료 처리", color = Color.Black)   // 블랙으로 변경
                        }
                    }

                    // 삭제 버튼
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = AppColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/* ----------------------------------------------------
   Quest Data
----------------------------------------------------- */
data class QuestItem(
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val targetMinutes: Int,
    val goalType: String,
    val deadlineDate: String,
    val deadlineTime: String,
    var currentMinutes: Int = 0,
    var completed: Boolean = false
)
