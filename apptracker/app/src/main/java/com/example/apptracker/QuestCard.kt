package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private val PrimaryColor = Color(0xFF00695C)

@Composable
fun QuestCard(
    quest: QuestItem,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val timeStr = "${timeFormat.format(Date(quest.startTime))} ~ ${timeFormat.format(Date(quest.endTime))}"
    val now = System.currentTimeMillis()
    val isTimeOver = (quest.endTime - now) <= 0

    val progress = if (quest.goalMinutes > 0) {
        (quest.progressMinutes.toFloat() / quest.goalMinutes).coerceIn(0f, 1f)
    } else 0f
    val percentage = (progress * 100).toInt()

    val isLessType = (quest.conditionType == "≤" || quest.conditionType == "<=")
    val isGoalMet = if (isLessType) quest.progressMinutes <= quest.goalMinutes else quest.progressMinutes >= quest.goalMinutes
    val canClaim = if (isLessType) isGoalMet && isTimeOver else isGoalMet

    val buttonText = when {
        canClaim -> "보상 받기"
        !isGoalMet -> "진행 중"
        isLessType && !isTimeOver -> "⏳ 대기 중"
        else -> "진행 중"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // 헤더
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(quest.appName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Text(timeStr, fontSize = 12.sp, color = Color.Gray)
                }
                // 뱃지
                val badgeColor = if(isTimeOver) Color.Red.copy(alpha=0.1f) else PrimaryColor.copy(alpha=0.1f)
                val badgeText = if(isTimeOver) "종료됨" else "진행 중"
                val badgeTextColor = if(isTimeOver) Color.Red else PrimaryColor

                Box(
                    modifier = Modifier.background(badgeColor, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(badgeText, color = badgeTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 진행률
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val goalText = if (isLessType) "이하" else "이상"
                Text("목표: ${quest.goalMinutes}분 $goalText", fontSize = 14.sp, color = Color.Gray)
                Text("${quest.progressMinutes}분 (${percentage}%)", fontWeight = FontWeight.Bold, color = PrimaryColor)
            }
            Spacer(Modifier.height(8.dp))

            val isFailed = isLessType && (quest.progressMinutes > quest.goalMinutes)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = if (isFailed) Color.Red else PrimaryColor,
                trackColor = Color(0xFFF0F0F0),
            )

            Spacer(Modifier.height(20.dp))

            // 버튼
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                ) { Text("포기", color = Color.Red) }

                Button(
                    onClick = onComplete,
                    enabled = canClaim,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryColor,
                        disabledContainerColor = Color.LightGray
                    )
                ) { Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun CompletedQuestCard(
    quest: QuestItem,
    onDelete: () -> Unit
) {
    val isSuccess = quest.success
    val bgColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val borderColor = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val statusText = if (isSuccess) "🎉 성공" else "💀 실패"

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, borderColor.copy(alpha=0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(quest.appName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(statusText, fontWeight = FontWeight.Bold, color = borderColor)
            }
            Spacer(Modifier.height(8.dp))
            Text("최종 기록: ${quest.progressMinutes}분 / 목표: ${quest.goalMinutes}분", fontSize = 14.sp, color = Color.DarkGray)
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
            ) { Text("기록 삭제") }
        }
    }
}