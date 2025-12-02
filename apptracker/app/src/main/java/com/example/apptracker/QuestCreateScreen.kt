package com.example.apptracker

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class) // 드롭다운 사용을 위해 필요
@Composable
fun QuestCreateScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val vm: QuestCreateViewModel = viewModel(
        factory = QuestCreateViewModelFactory(app)
    )

    LaunchedEffect(Unit) { vm.loadInstalledApps() }

    val appList = vm.appList.collectAsState()
    val recommendedApps = vm.recommendedApps.collectAsState()
    val selected = vm.selectedApp.collectAsState()
    val condition = vm.conditionType.collectAsState()
    val minutes = vm.targetMinutes.collectAsState()

    val startHour = vm.startHour.collectAsState()
    val startMinute = vm.startMinute.collectAsState()
    val endHour = vm.endHour.collectAsState()
    val endMinute = vm.endMinute.collectAsState()

    val isLoading = vm.isLoading.collectAsState()

    // 드롭다운 메뉴 펼침 상태
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF00462A),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF00462A))
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.safeGestures)
            ) {
                Button(
                    onClick = {
                        vm.createQuest {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading.value
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text("퀘스트 만들기", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()) // 전체 화면 스크롤
        ) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("뒤로가기", color = Color.Black)
            }
            Spacer(Modifier.height(20.dp))
            Text("퀘스트 생성", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            // 1. AI 추천 섹션 (이건 가로 스크롤 유지 - 보기 좋음)
            if (recommendedApps.value.isNotEmpty()) {
                Text("🤖 AI 맞춤 추천", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recommendedApps.value) { recApp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            modifier = Modifier.clickable { vm.selectApp(recApp) }
                        ) {
                            Text(
                                text = recApp.appName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color(0xFF00462A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 2. 🔥 [핵심 수정] 앱 선택 (드롭다운 메뉴로 변경)
            Text("앱 선택", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selected.value?.appName ?: "앱을 선택해주세요",
                    onValueChange = {},
                    readOnly = true, // 입력 불가, 선택만 가능
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    appList.value.forEach { app ->
                        DropdownMenuItem(
                            text = { Text(app.appName) },
                            onClick = {
                                vm.selectApp(app)
                                isDropdownExpanded = false // 선택 후 닫기
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. 목표 시간 & 조건 (기존 유지)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(120.dp)) {
                    Text("목표 시간(분)", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minutes.value.toString(),
                        onValueChange = { vm.setTargetMinutes(it.toIntOrNull() ?: 0) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("성공 조건", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = condition.value == "≤", onClick = { vm.setCondition("≤") }, colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray))
                        Text("이하", color = Color.White)
                        Spacer(Modifier.width(4.dp))
                        RadioButton(selected = condition.value == "≥", onClick = { vm.setCondition("≥") }, colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray))
                        Text("이상", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 4. 시작 시간 (스크롤 피커)
            Text("퀘스트 시작 시간", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF003A20), RoundedCornerShape(12.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VerticalWheelPicker((0..23).toList(), startHour.value, { vm.setStartHour(it) }, "시")
                Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Gray))
                VerticalWheelPicker((0..55 step 5).toList(), startMinute.value, { vm.setStartMinute(it) }, "분")
            }

            // 5. 종료 시간 (스크롤 피커)
            Spacer(Modifier.height(24.dp))
            Text("퀘스트 종료 시간", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF003A20), RoundedCornerShape(12.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VerticalWheelPicker((0..23).toList(), endHour.value, { vm.setEndHour(it) }, "시")
                Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Gray))
                VerticalWheelPicker((0..55 step 5).toList(), endMinute.value, { vm.setEndMinute(it) }, "분")
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun VerticalWheelPicker(items: List<Int>, selectedItem: Int, onItemSelected: (Int) -> Unit, label: String) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val index = items.indexOf(selectedItem)
        if (index >= 0) listState.scrollToItem(index)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.LightGray, fontSize = MaterialTheme.typography.bodySmall.fontSize)
        Spacer(Modifier.height(4.dp))
        LazyColumn(state = listState, modifier = Modifier.width(60.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(vertical = 40.dp)) {
            items(items) { item ->
                val isSelected = (item == selectedItem)
                Box(modifier = Modifier.height(40.dp).fillMaxWidth().clickable { onItemSelected(item) }, contentAlignment = Alignment.Center) {
                    Text(text = if (item < 10) "0$item" else "$item", color = if (isSelected) Color.White else Color.Gray, fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}