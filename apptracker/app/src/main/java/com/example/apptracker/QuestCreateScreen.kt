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
// 아이콘 사용을 위한 import 추가
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

private val PrimaryColor = Color(0xFF00695C)
private val BackgroundColor = Color(0xFFF5F7F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestCreateScreen(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: QuestCreateViewModel = viewModel(factory = QuestCreateViewModelFactory(app))

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

    var isDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundColor,
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = Color.White) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = { vm.createQuest { navController.popBackStack() } },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading.value
                    ) {
                        if (isLoading.value) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text("퀘스트 생성하기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            
            // 뒤로가기 버튼 아이콘화
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, 
                        contentDescription = "뒤로가기", 
                        tint = Color.Black
                    )
                }
                Text("새 퀘스트", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(Modifier.height(24.dp))

            // AI 추천
            if (recommendedApps.value.isNotEmpty()) {
                Text("✨ AI 추천", color = PrimaryColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recommendedApps.value) { recApp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.clickable { vm.selectApp(recApp) }
                        ) {
                            Text(recApp.appName, modifier = Modifier.padding(16.dp), color = PrimaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // 폼 영역
            Text("기본 설정", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(20.dp)) {
                    // 앱 선택 (드롭다운 유지)
                    Text("대상 앱", color = Color.Gray, fontSize = 12.sp)
                    ExposedDropdownMenuBox(expanded = isDropdownExpanded, onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }) {
                        OutlinedTextField(
                            value = selected.value?.appName ?: "앱을 선택해주세요",
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryColor, unfocusedBorderColor = Color.LightGray)
                        )
                        ExposedDropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                            appList.value.forEach { app ->
                                DropdownMenuItem(text = { Text(app.appName) }, onClick = { vm.selectApp(app); isDropdownExpanded = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // 목표
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = minutes.value.toString(),
                            onValueChange = { vm.setTargetMinutes(it.toIntOrNull() ?: 0) },
                            label = { Text("목표(분)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryColor)
                        )
                        Spacer(Modifier.width(12.dp))
                        Row {
                            FilterChip(selected = condition.value == "≤", onClick = { vm.setCondition("≤") }, label = { Text("이하") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryColor.copy(alpha=0.2f), selectedLabelColor = PrimaryColor))
                            Spacer(Modifier.width(8.dp))
                            FilterChip(selected = condition.value == "≥", onClick = { vm.setCondition("≥") }, label = { Text("이상") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryColor.copy(alpha=0.2f), selectedLabelColor = PrimaryColor))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // 시간 설정
            Text("시간 설정", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("시작 시간", fontWeight = FontWeight.Bold, color = PrimaryColor)
                    Row(Modifier.height(120.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        VerticalWheelPicker((0..23).toList(), startHour.value, { vm.setStartHour(it) }, "시")
                        VerticalWheelPicker((0..55 step 5).toList(), startMinute.value, { vm.setStartMinute(it) }, "분")
                    }
                    Divider(Modifier.padding(vertical = 10.dp))
                    Text("종료 시간", fontWeight = FontWeight.Bold, color = PrimaryColor)
                    Row(Modifier.height(120.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        VerticalWheelPicker((0..23).toList(), endHour.value, { vm.setEndHour(it) }, "시")
                        VerticalWheelPicker((0..55 step 5).toList(), endMinute.value, { vm.setEndMinute(it) }, "분")
                    }
                }
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
        Text(label, color = Color.Gray, fontSize = 12.sp)
        LazyColumn(state = listState, modifier = Modifier.width(50.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(vertical = 40.dp)) {
            items(items) { item ->
                val isSelected = (item == selectedItem)
                Box(modifier = Modifier.height(40.dp).fillMaxWidth().clickable { onItemSelected(item) }, contentAlignment = Alignment.Center) {
                    Text(
                        text = if (item < 10) "0$item" else "$item",
                        color = if (isSelected) PrimaryColor else Color.LightGray,
                        fontSize = if(isSelected) 24.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}