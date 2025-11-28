package com.example.apptracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun QuestCard(
    quest: QuestItem,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(quest.appName, color = Color.Black, style = MaterialTheme.typography.titleMedium)

            val goalText = if (quest.conditionType == "≤") "이하" else "이상"
            Text("목표: ${quest.goalMinutes}분 $goalText", color = Color.DarkGray)
            Text("현재: ${quest.progressMinutes}분", color = Color(0xFF00462A))

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(Color(0xFFF44336))
                ) { Text("취소", color = Color.White) }

                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
                ) { Text("완료", color = Color.White) }
            }
        }
    }
}

@Composable
fun CompletedQuestCard(
    quest: QuestItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD8FFD8)
        )
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(quest.appName, color = Color.Black, style = MaterialTheme.typography.titleMedium)
            Text("최종 달성: ${quest.progressMinutes}분", color = Color.DarkGray)
            Text("상태: 완료됨", color = Color(0xFF006400))

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(Color.White)
            ) {
                Text("기록 삭제", color = Color.Black)
            }
        }
    }
}