package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*
// 뒤로가기 버튼 디자인 개선을 위한 추가
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon

private val PrimaryColor = Color(0xFF00695C)
private val BackgroundColor = Color(0xFFF5F7F6)

@Composable
fun GameRoomScreen(
    navController: NavHostController,
    roomId: String,
    vm: GameViewModel = viewModel()
) {
    LaunchedEffect(roomId) { vm.joinAndObserve(roomId) }
    val room = vm.currentRoom.collectAsState().value

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.checkTimeAndRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (room == null) {
        Box(Modifier.fillMaxSize().background(BackgroundColor), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryColor)
        }
        return
    }

    val isHost = (room.creator == vm.myName)
    val myInfo = room.participants[vm.myName]
    val isReady = myInfo?.isReady ?: false
    val isGameActive = (room.status == "active")
    val isFinished = (room.status == "finished")
    val isFailed = (room.status == "failed")
    val now = System.currentTimeMillis()
    val isTimeStarted = now >= room.startTime

    val userColors = listOf(Color(0xFF42A5F5), Color(0xFFEF5350), Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFFFF7043))
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val timeRangeStr = "${timeFormat.format(Date(room.startTime))} ~ ${timeFormat.format(Date(room.endTime))}"
    val waitMinutes = ((room.startTime - now) / 60000).toInt()

    Scaffold(
        containerColor = BackgroundColor,
        bottomBar = {
            if (room.status == "waiting") {
                Box(modifier = Modifier.background(Color.White).padding(16.dp)) {
                    Button(
                        onClick = { if (isHost) vm.startGame() else vm.toggleReady() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isReady) Color.Gray else PrimaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val btnText = if (isHost) "게임 시작 (방 열기)" else if (isReady) "준비 완료!" else "준비 하기"
                        Text(btnText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.Gray
                    )
                }
                Spacer(Modifier.width(8.dp))
                val titleText = when {
                    isFinished -> "🎉 게임 종료!"
                    isFailed -> "💀 미션 실패..."
                    isGameActive && !isTimeStarted -> "⏳ 시작 대기 중"
                    isGameActive -> "🔥 게임 진행 중"
                    else -> room.title
                }
                Text(titleText, color = Color.Black, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            if (isFinished || isFailed) {
                // 🔥 [에러 해결] 함수가 아래에 정의되어 있습니다!
                val resultMessage = getResultMessage(room, vm.myName)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(if(isFailed) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(resultMessage, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                        if (canIClaimReward(room, vm.myName) && !isFailed) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { vm.claimReward() },
                                enabled = !(myInfo?.rewardClaimed ?: false),
                                colors = ButtonDefaults.buttonColors(PrimaryColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if(myInfo?.rewardClaimed == true) "보상 획득 완료" else "🎁 보상 받기", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${room.targetAppName} ${room.goalMinutes}분 ${if(room.condition=="≤")"이하" else "이상"}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(timeRangeStr, color = Color.Gray, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("모드: ${if(room.mode=="coop") "협력" else "경쟁"}", color = if(room.mode=="coop") Color(0xFF4CAF50) else Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        val statusStr = if(isGameActive) {
                            if(isTimeStarted) "진행 중" else "오픈 대기 (${waitMinutes}분 남음)"
                        } else "대기실"
                        Text(statusStr, color = PrimaryColor, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (isGameActive && isTimeStarted) {
                if (room.mode == "coop") {
                    CoopProgressBar(room.participants.values.toList(), room.goalMinutes, userColors)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(room.participants.values.toList().sortedByDescending { it.currentMinutes }) { p ->
                            InGamePlayerCard(p, room.goalMinutes, room.condition)
                        }
                    }
                }
            } else if (isGameActive && !isTimeStarted) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏳", fontSize = 60.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("게임 시작 대기 중...", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("설정된 시간이 되면\n자동으로 시작됩니다.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else if (!isFinished && !isFailed) {
                Text("참가자 (${room.participants.size}명)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(room.participants.values.toList()) { index, p ->
                        val color = if (room.mode == "coop") userColors[index % userColors.size] else Color.White
                        ParticipantCard(p, room.creator, color, room.mode == "coop")
                    }
                }
            }
        }
    }
}

// 🔥 [핵심 추가] 아래 함수들이 파일 내부에 있어야 합니다!

fun getResultMessage(room: Room, myName: String): String {
    if (room.status == "failed") return "💥 미션 실패! (목표 초과)"
    if (room.mode == "coop") return "🎉 협력 성공! 모두 고생하셨습니다."
    if (room.winner.isNotBlank()) {
        return if (room.winner == myName) "🏆 승리! 축하합니다!" else "😢 패배... 우승자: ${room.winner}"
    }
    val myInfo = room.participants[myName]
    val isSuccess = (myInfo?.currentMinutes ?: 0) <= room.goalMinutes
    return if (isSuccess) "🛡️ 생존 성공! (목표 달성)" else "💀 탈락! (목표 초과)"
}

fun canIClaimReward(room: Room, myName: String): Boolean {
    if (room.status == "failed") return false
    val myInfo = room.participants[myName] ?: return false
    if (room.mode == "coop") return true
    if (room.condition == "≥") return room.winner == myName
    return myInfo.currentMinutes <= room.goalMinutes
}

@Composable
fun CoopProgressBar(participants: List<Participant>, goalMinutes: Int, colors: List<Color>) {
    val totalUsed = participants.sumOf { it.currentMinutes }
    val safeGoal = if (goalMinutes > 0) goalMinutes else 1
    val progressPercent = ((totalUsed.toFloat() / safeGoal) * 100).toInt()

    Card(colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("팀 전체 달성도", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Text("$totalUsed / ${goalMinutes}분 ($progressPercent%)", fontWeight = FontWeight.Bold, color = PrimaryColor)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.LightGray)) {
                participants.forEachIndexed { index, p ->
                    if (p.currentMinutes > 0) {
                        Box(modifier = Modifier.weight(p.currentMinutes.toFloat()).fillMaxHeight().background(colors[index % colors.size]))
                    }
                }
                val remaining = safeGoal - totalUsed
                if (remaining > 0) Box(modifier = Modifier.weight(remaining.toFloat()).fillMaxHeight().background(Color(0xFFE0E0E0)))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                participants.forEachIndexed { index, p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(colors[index % colors.size], RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(4.dp))
                        Text("${p.nickname} (${p.currentMinutes}분)", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantCard(p: Participant, creatorName: String, color: Color, isCoop: Boolean) {
    val isHost = (p.nickname == creatorName)
    Card(colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCoop) { Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(color)); Spacer(Modifier.width(8.dp)) }
                if (isHost) Text("👑 ", fontSize = 20.sp)
                Text(p.nickname, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            if (!isHost) {
                val readyColor = if (p.isReady) Color(0xFF4CAF50) else Color.Gray
                Box(modifier = Modifier.background(readyColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(if (p.isReady) "READY" else "WAIT", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text("방장", color = PrimaryColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InGamePlayerCard(p: Participant, goal: Int, condition: String) {
    val progress = (p.currentMinutes.toFloat() / goal).coerceIn(0f, 1f)
    val isOver = p.currentMinutes > goal
    val barColor = if(condition == "≤" && isOver) Color.Red else PrimaryColor
    Card(colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(p.nickname, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("${p.currentMinutes} / ${goal}분", color = Color.DarkGray)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), color = barColor, trackColor = Color.LightGray)
        }
    }
}