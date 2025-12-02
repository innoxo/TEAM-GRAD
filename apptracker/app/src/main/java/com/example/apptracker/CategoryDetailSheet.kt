package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 메인 테마 색상 (필요시 사용)
private val PrimaryColor = Color(0xFF00695C)

@Composable
fun CategoryDetailSheet(
    category: String,
    apps: List<AppUsage>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White) // 배경색을 흰색으로 변경
            .padding(16.dp)
    ) {

        Text(
            text = "$category 앱 목록",
            color = Color.Black, // 제목 글씨를 검은색으로 변경
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        if (apps.isEmpty()) {
            Text("사용 기록이 없습니다.", color = Color.Gray)
        } else {
            LazyColumn {
                items(apps) { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // 앱 이름 글씨를 검은색으로 변경
                        Text(app.appName, color = Color.Black, fontWeight = FontWeight.Medium)
                        // 시간 글씨를 회색으로 변경 (잘 보이게)
                        Text("${app.minutes}분 사용", color = Color.Gray)
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f)) // 구분선 추가 (선택 사항)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 닫기 버튼
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor // 버튼 배경을 초록색으로 (흰 배경에 잘 보이게)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text("닫기", color = Color.White) // 버튼 글씨는 흰색
        }
    }
}