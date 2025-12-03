package com.example.apptracker

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush // 🔥 그라데이션용
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase

private val PrimaryColor = Color(0xFF00695C)
private val BackgroundColor = Color(0xFF81B184) // 대시보드 배경톤과 맞춤

object UserSession {
    var nickname: String = ""
}

@Composable
fun NicknameSetupScreen(navController: NavController) {

    val context = LocalContext.current
    var nickname by remember { mutableStateOf(TextFieldValue("")) }

    // 자동 로그인
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("AppTrackerPrefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("nickname", null)
        if (!savedName.isNullOrBlank()) {
            UserSession.nickname = savedName
            navController.navigate("dashboard") {
                popUpTo("nickname_setup") { inclusive = true }
            }
        }
    }

    val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    // 🔥 로고용 그라데이션 브러시 (진한녹색 -> 연두색)
    // 흰색 카드 위에서 잘 보이게 색상을 조정했습니다.
    val LimeGreen = Color(0xFF32CD32)
    val logoBrush = Brush.horizontalGradient(
        colors = listOf(PrimaryColor, LimeGreen)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 🔥 [수정됨] 로고 텍스트 & 스타일 변경
                Text(
                    text = "Play&Focus", // 이름 변경
                    style = MaterialTheme.typography.displaySmall.copy( // 폰트 크기 키움
                        brush = logoBrush, // 그라데이션 적용
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.height(8.dp))
                Text("스마트한 습관의 시작", fontSize = 14.sp, color = Color.Gray)

                Spacer(Modifier.height(40.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("닉네임") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        focusedLabelColor = PrimaryColor,
                        cursorColor = PrimaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        val name = nickname.text.trim()
                        if (name.isBlank()) return@Button

                        UserSession.nickname = name
                        val prefs = context.getSharedPreferences("AppTrackerPrefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("nickname", name).apply()

                        db.child("users").child(name)
                            .child("lastLogin")
                            .setValue(System.currentTimeMillis())

                        navController.navigate("dashboard") {
                            popUpTo("nickname_setup") { inclusive = true }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("시작하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}