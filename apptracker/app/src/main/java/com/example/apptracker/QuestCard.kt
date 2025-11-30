package com.example.apptracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

    // 달성률 계산
    val progress = if (quest.goalMinutes > 0) {
        (quest.progressMinutes.toFloat() / quest.goalMinutes).coerceIn(0f, 1f)
    } else 0f
    val percentage = (progress * 100).toInt()

    // ------------------------------------------------------------
    // 🔥 [핵심 로직] 보상받기 버튼을 누를 수 있는지(활성화) 판단
    // ------------------------------------------------------------
    val now = System.currentTimeMillis()
    val isLessType = (quest.conditionType == "≤") // 이하 퀘스트
    val isGoalMet = if (isLessType) {
        quest.progressMinutes <= quest.goalMinutes // 이하는 넘지 않아야 성공
    } else {
        quest.progressMinutes >= quest.goalMinutes // 이상은 넘어야 성공
    }

    val canClaim = if (isLessType) {
        // [이하 조건]: 목표도 지키고 + 시간도 '완전히 끝났을 때'만 가능
        isGoalMet && (now >= quest.endTime)
    } else {
        // [이상 조건]: 목표만 달성하면 시간 안 끝나도 즉시 가능
        isGoalMet
    }

    // 버튼 텍스트 (상태에 따라 다르게 표시)
    val buttonText = when {
        canClaim -> "보상 받기"
        !isGoalMet -> "진행 중" // 아직 목표 달성 못함
        isLessType && now < quest.endTime -> "시간 대기" // 목표는 지키고 있는데 시간이 남음
        else -> "진행 중"
    }
    // ------------------------------------------------------------

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
                Text("$startStr ~ $endStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val conditionText = if (quest.conditionType == "≤") "이하" else "이상"
                Text("목표: ${quest.goalMinutes}분 $conditionText", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                Text("${quest.progressMinutes}분 (${percentage}%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00462A))
            }
            Spacer(Modifier.height(8.dp))

            // 게이지 바 색상 로직 (이하 조건인데 초과하면 빨간색)
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

                // 🔥 [수정됨] 조건(canClaim)에 따라 버튼 활성화/비활성화
                Button(
                    onClick = onComplete,
                    enabled = canClaim, // 여기가 핵심! false면 회색으로 변하고 안 눌림
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00462A),
                        disabledContainerColor = Color.LightGray // 비활성화일 때 회색
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
    val isSuccess = quest.isSuccess
    val bgColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusText = if (isSuccess) "성공!" else "실패 (포기)"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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