package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun GameRoomScreen(
    navController: NavHostController,
    roomId: String,
    vm: GameViewModel = viewModel()
) {
    LaunchedEffect(roomId) { vm.joinAndObserve(roomId) }
    val room = vm.currentRoom.collectAsState().value

    if (room == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF00462A)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val isHost = (room.creator == vm.myName)
    val myInfo = room.participants[vm.myName]
    val isReady = myInfo?.isReady ?: false
    val isGameActive = (room.status == "active")

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF00462A)).padding(16.dp)
    ) {
        // 상단바
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { navController.popBackStack() }, colors = ButtonDefaults.buttonColors(Color.White)) { Text("나가기", color = Color.Black) }
            Spacer(Modifier.width(12.dp))
            Text(if(isGameActive) "🔥 게임 중!" else room.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(20.dp))

        // 방 정보
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF003A20))) {
            Column(Modifier.padding(16.dp)) {
                Text("목표: ${room.targetAppName} ${room.goalMinutes}분 ${if(room.condition=="≤")"이하" else "이상"}", color = Color.White)
                Text("모드: ${if(room.mode=="coop") "협력" else "경쟁"}", color = if(room.mode=="coop") Color(0xFF81C784) else Color(0xFFEF5350))
            }
        }

        Spacer(Modifier.height(20.dp))

        // 🔥 게임 상태에 따라 UI 분기
        if (isGameActive) {
            Text("실시간 진행 현황", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // 참가자들 게이지 바 표시
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(room.participants.values.toList().sortedByDescending { it.currentMinutes }) { p ->
                    InGamePlayerCard(p, room.goalMinutes, room.condition)
                }
            }
        } else {
            // 대기실 UI
            Text("참가자 대기 중 (${room.participants.size}명)", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(room.participants.values.toList()) { p ->
                    ParticipantCard(p, room.creator)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // 버튼 (게임 중엔 숨김)
        if (!isGameActive) {
            Button(
                onClick = { if (isHost) vm.startGame() else vm.toggleReady() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isReady) Color.Gray else Color.White)
            ) {
                val btnText = if (isHost) "게임 시작" else if (isReady) "준비 완료!" else "준비 하기"
                Text(btnText, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 대기실 카드
@Composable
fun ParticipantCard(p: Participant, creatorName: String) {
    val isHost = (p.nickname == creatorName)
    val readyColor = if (p.isReady) Color(0xFF4CAF50) else Color.Gray
    Card(colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isHost) Text("👑 ", fontSize = 20.sp)
                Text(p.nickname, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            if (!isHost) {
                Box(modifier = Modifier.background(readyColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(if (p.isReady) "READY" else "WAIT", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text("방장", color = Color(0xFF00462A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 🔥 게임 중 카드 (게이지 바)
@Composable
fun InGamePlayerCard(p: Participant, goal: Int, condition: String) {
    val progress = (p.currentMinutes.toFloat() / goal).coerceIn(0f, 1f)
    val isOver = p.currentMinutes > goal
    val barColor = if(condition == "≤" && isOver) Color.Red else Color(0xFF4CAF50) // 이하 조건인데 넘으면 빨강

    Card(colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(p.nickname, fontWeight = FontWeight.Bold)
                Text("${p.currentMinutes} / ${goal}분", color = Color.DarkGray)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = barColor,
                trackColor = Color.LightGray
            )
        }
    }
}