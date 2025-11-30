package com.example.apptracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QuestCard(
    quest: QuestItem,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val startStr = timeFormat.format(Date(quest.startTime))
    val endStr = timeFormat.format(Date(quest.endTime))

    val now = System.currentTimeMillis()
    val isTimeOver = now >= quest.endTime

    val progress = if (quest.goalMinutes > 0) {
        (quest.progressMinutes.toFloat() / quest.goalMinutes).coerceIn(0f, 1f)
    } else 0f
    val percentage = (progress * 100).toInt()

    val isLessType = (quest.conditionType == "≤" || quest.conditionType == "<=")
    val isGoalMet = if (isLessType) {
        quest.progressMinutes <= quest.goalMinutes
    } else {
        quest.progressMinutes >= quest.goalMinutes
    }

    val canClaim = if (isLessType) {
        isGoalMet && isTimeOver
    } else {
        isGoalMet
    }

    val buttonText = when {
        canClaim -> "보상 받기"
        !isGoalMet -> "진행 중"
        isLessType && !isTimeOver -> "⏳ 시간 대기"
        else -> "진행 중"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(quest.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                if (isTimeOver) {
                    Text("종료됨 ($startStr ~ $endStr)", style = MaterialTheme.typography.bodySmall, color = Color.Red, fontWeight = FontWeight.Bold)
                } else {
                    Text("진행 중 ($startStr ~ $endStr)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00462A))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val conditionText = if (isLessType) "이하" else "이상"
                Text("목표: ${quest.goalMinutes}분 $conditionText", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                Text("${quest.progressMinutes}분 (${percentage}%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00462A))
            }
            Spacer(Modifier.height(8.dp))
            val isFailed = isLessType && (quest.progressMinutes > quest.goalMinutes)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (isFailed) Color.Red else Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(Color(0xFFFFEBEE)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("포기", color = Color(0xFFD32F2F)) }

                Button(
                    onClick = onComplete,
                    enabled = canClaim,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00462A),
                        disabledContainerColor = Color.LightGray
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(buttonText, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CompletedQuestCard(
    quest: QuestItem,
    onDelete: () -> Unit
) {
    // 🔥 [수정됨] quest.success 값을 확인해서 성공/실패 판별
    val isSuccess = quest.success

    val bgColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusText = if (isSuccess) "성공!" else "실패 (포기)"

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(quest.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(statusText, fontWeight = FontWeight.Bold, color = textColor)
            }
            Spacer(Modifier.height(4.dp))
            Text("최종 기록: ${quest.progressMinutes}분", color = Color.DarkGray)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(Color.White),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Text("기록 삭제", color = Color.Black)
            }
        }
    }
}