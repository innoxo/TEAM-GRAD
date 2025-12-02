package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun MultiplayerLobbyScreen(
    navController: NavHostController,
    vm: MultiplayerViewModel = viewModel()
) {
    val activeList = vm.activeRooms.collectAsState()
    val completedList = vm.completedRooms.collectAsState()
    val installedApps = vm.installedApps.collectAsState()

    val startHour = vm.startHour.collectAsState()
    val startMinute = vm.startMinute.collectAsState()
    val endHour = vm.endHour.collectAsState()
    val endMinute = vm.endMinute.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("진행 중", "완료됨")

    // 🔥 내 이름 가져오기
    val myName = vm.myName

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00462A))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { navController.popBackStack() }, colors = ButtonDefaults.buttonColors(Color.White)) { Text("뒤로가기", color = Color.Black) }
                Spacer(Modifier.width(12.dp))
                Text("멀티플레이 로비", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            TabRow(selectedTabIndex = tabIndex, containerColor = Color(0xFF00462A), contentColor = Color.White) {
                tabs.forEachIndexed { index, text ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(text, fontWeight = FontWeight.Bold, color = if (tabIndex == index) Color.White else Color.Gray) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            val currentList = if (tabIndex == 0) activeList.value else completedList.value

            if (currentList.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    val emptyText = if (tabIndex == 0) "진행 중인 방이 없습니다.\n방을 만들어보세요!" else "완료된 방이 없습니다."
                    Text(emptyText, color = Color.LightGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(currentList) { room ->
                        Box(modifier = Modifier.clickable { navController.navigate("game_room/${room.roomId}") }) {
                            // 🔥 내 이름(myName)도 같이 전달!
                            RoomItemCard(room, myName)
                        }
                    }
                }
            }
        }

        if (tabIndex == 0) {
            FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp), containerColor = Color.White) { Text("➕", style = MaterialTheme.typography.titleLarge) }
        }
    }

    if (showDialog) {
        CreateRoomDialog(
            appList = installedApps.value,
            startHour = startHour.value, startMinute = startMinute.value,
            endHour = endHour.value, endMinute = endMinute.value,
            onStartHourChange = { vm.setStartHour(it) },
            onStartMinuteChange = { vm.setStartMinute(it) },
            onEndHourChange = { vm.setEndHour(it) },
            onEndMinuteChange = { vm.setEndMinute(it) },
            onDismiss = { showDialog = false },
            onCreate = { title, mode, app, mins, cond ->
                vm.createRoom(title, mode, app, mins, cond)
                showDialog = false
            }
        )
    }
}

@Composable
fun RoomItemCard(room: Room, myName: String) {
    val modeColor = if (room.mode == "coop") Color(0xFF4CAF50) else Color(0xFFF44336)
    val modeText = if (room.mode == "coop") "협력" else "경쟁"

    val statusText = when(room.status) {
        "active" -> "🔥 진행 중"
        "waiting" -> "⏳ 대기 중"
        "finished" -> "🏁 종료됨"
        "failed" -> "💀 실패"
        else -> ""
    }

    // 🔥 [핵심] 보상 미수령 체크
    val myInfo = room.participants[myName]
    val isUnclaimed = if (room.status == "finished" && myInfo != null && !myInfo.rewardClaimed) {
        if (room.mode == "coop") true // 협력 성공이면 무조건 보상 있음
        else room.winner == myName // 경쟁이면 승자만 보상 있음
    } else false

    Card(
        colors = CardDefaults.cardColors(Color.White),
        modifier = Modifier.fillMaxWidth(),
        // 보상 안 받았으면 빨간 테두리로 강조
        border = if(isUnclaimed) androidx.compose.foundation.BorderStroke(2.dp, Color.Red) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    Text("[$modeText]", color = modeColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(room.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                // 🔥 보상 안 받았으면 텍스트 띄우기
                if (isUnclaimed) {
                    Text("🎁 보상 미수령!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text(statusText, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("목표: ${room.targetAppName} ${room.goalMinutes}분 ${if(room.condition=="≤")"이하" else "이상"}")
            Text("참여: ${room.participants.size}명")
        }
    }
}

// ... (나머지 CreateRoomDialog, LobbyWheelPicker는 기존 코드와 동일) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomDialog(
    appList: List<App>,
    startHour: Int, startMinute: Int,
    endHour: Int, endMinute: Int,
    onStartHourChange: (Int) -> Unit, onStartMinuteChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit, onEndMinuteChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (String, String, App, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("coop") }
    var selectedApp by remember { mutableStateOf<App?>(null) }
    var goalMinutes by remember { mutableStateOf("30") }
    var condition by remember { mutableStateOf("≤") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("방 만들기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("방 제목") }, singleLine = true)
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedApp?.appName ?: "앱 선택", onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        appList.forEach { app ->
                            DropdownMenuItem(text = { Text(app.appName) }, onClick = { selectedApp = app; expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = goalMinutes, 
                        onValueChange = { newValue ->
                            // c: Char 라고 타입을 명시
                            if (newValue.all { c: Char -> c.isDigit() }) {
                                goalMinutes = newValue
                            }
                        },
                        label = { Text("목표(분)") }, 
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = condition == "≤", onClick = { condition = "≤" })
                            Text("이하")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = condition == "≥", onClick = { condition = "≥" })
                            Text("이상")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text("시작 시간", fontWeight = FontWeight.Bold)
                Row(Modifier.height(100.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LobbyWheelPicker((0..23).toList(), startHour, onStartHourChange, "시")
                    LobbyWheelPicker((0..55 step 5).toList(), startMinute, onStartMinuteChange, "분")
                }

                Text("종료 시간", fontWeight = FontWeight.Bold)
                Row(Modifier.height(100.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LobbyWheelPicker((0..23).toList(), endHour, onEndHourChange, "시")
                    LobbyWheelPicker((0..55 step 5).toList(), endMinute, onEndMinuteChange, "분")
                }

                Spacer(Modifier.height(12.dp))
                Text("모드 선택", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedMode == "coop", onClick = { selectedMode = "coop" })
                    Text("협력")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = selectedMode == "vs", onClick = { selectedMode = "vs" })
                    Text("경쟁")
                }

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
                    Button(
                        onClick = {
                            if(title.isNotBlank() && selectedApp != null) {
                                onCreate(title, selectedMode, selectedApp!!, goalMinutes.toIntOrNull() ?: 10, condition)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(Color(0xFF00462A))
                    ) { Text("생성") }
                }
            }
        }
    }
}

@Composable
fun LobbyWheelPicker(items: List<Int>, selectedItem: Int, onItemSelected: (Int) -> Unit, label: String) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val index = items.indexOf(selectedItem)
        if (index >= 0) listState.scrollToItem(index)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.LightGray, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.width(50.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 30.dp)
        ) {
            items(items) { item ->
                val isSelected = (item == selectedItem)
                Box(
                    modifier = Modifier.height(30.dp).fillMaxWidth().clickable { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item < 10) "0$item" else "$item",
                        color = if (isSelected) Color.Black else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}