package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.*

object UserSession {
    var nickname: String = ""
}

@Composable
fun NicknameSetupScreen(navController: NavController) {

    var nickname by remember { mutableStateOf(TextFieldValue("")) }

    val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00462A))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("닉네임을 입력해주세요", color = Color.White)

        Spacer(Modifier.height(24.dp))

        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임", color = Color.White) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x22000000),
                unfocusedContainerColor = Color(0x22000000),
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.LightGray,
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val name = nickname.text.trim()
                if (name.isBlank()) return@Button

                UserSession.nickname = name

                // 추가: 기기 내부에 영구 저장 (Worker가 꺼내 쓸 수 있게 하도록)
                val prefs = navController.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("saved_nickname", name).apply()

                db.child("users").child(name)
                    .child("lastLogin")
                    .setValue(System.currentTimeMillis())

                navController.navigate("dashboard") {
                    popUpTo("nickname_setup") { inclusive = true }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("시작하기", color = Color(0xFF00462A))
        }
    }
}
