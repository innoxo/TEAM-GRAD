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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
// 뒤로가기 버튼 디자인 개선을 위한 추가
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon

// 디자인 컬러
private val PrimaryColor = Color(0xFF00695C)
private val BackgroundColor = Color(0xFFF5F7F6)

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

    val myName = vm.myName

    Box(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 상단바
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.Gray
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("멀티플레이 로비", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(Modifier.height(20.dp))

            // 탭 메뉴
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
                        text = { Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            val currentList = if (tabIndex == 0) activeList.value else completedList.value

            if (currentList.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    val msg = if (tabIndex == 0) "진행 중인 방이 없습니다.\n우측 하단 버튼을 눌러 방을 만들어보세요!" else "완료된 방이 없습니다."
                    Text(msg, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(currentList) { room ->
                        Box(modifier = Modifier.clickable {
                            navController.navigate("game_room/${room.roomId}")
                        }) {
                            // 🔥 [에러 해결] 이 함수가 아래쪽에 정의되어 있어야 합니다!
                            RoomItemCard(room, myName)
                        }
                    }
                }
            }
        }

        if (tabIndex == 0) {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = PrimaryColor,
                contentColor = Color.White
            ) { Text("➕", fontSize = 24.sp) }
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

// 🔥 [추가됨] RoomItemCard 함수 정의
@Composable
fun RoomItemCard(room: Room, myName: String) {
    val modeColor = if (room.mode == "coop") Color(0xFF4CAF50) else Color(0xFFEF5350)
    val modeText = if (room.mode == "coop") "협력" else "경쟁"

    val statusText = when(room.status) {
        "active" -> "🔥 진행 중"
        "waiting" -> "⏳ 대기 중"
        "finished" -> "🏁 종료됨"
        "failed" -> "💀 실패"
        else -> ""
    }

    val myInfo = room.participants[myName]
    val isUnclaimed = if (room.status == "finished" && myInfo != null && !myInfo.rewardClaimed) {
        if (room.mode == "coop") true else room.winner == myName
    } else false

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if(isUnclaimed) androidx.compose.foundation.BorderStroke(2.dp, Color.Red) else null
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = modeColor.copy(alpha=0.1f), shape = RoundedCornerShape(6.dp)) {
                        Text(modeText, color = modeColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(room.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }

                if (isUnclaimed) {
                    Text("🎁 보상 미수령", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text(statusText, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("목표: ${room.targetAppName} ${room.goalMinutes}분 ${if(room.condition=="≤")"이하" else "이상"}", color = Color.DarkGray)
            Spacer(Modifier.height(4.dp))
            Text("참여 인원: ${room.participants.size}명", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

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
                Text("방 만들기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
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
                        value = goalMinutes, onValueChange = { if(it.all { c -> c.isDigit() }) goalMinutes = it },
                        label = { Text("목표(분)") }, modifier = Modifier.weight(1f)
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
                        colors = ButtonDefaults.buttonColors(PrimaryColor)
                    ) { Text("생성", color = Color.White) }
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