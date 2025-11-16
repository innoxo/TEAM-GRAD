package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp

@Composable
fun CategoryDetailSheet(
    category: String,
    apps: List<AppUsage>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeColor(0xFF00462A))
            .padding(16.dp)
    ) {

        Text(
            text = "$category 앱 목록",
            color = ComposeColor.White,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(apps) { app ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(app.appName, color = ComposeColor.White)
                    Text("${app.minutes}분 사용", color = ComposeColor.LightGray)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ComposeColor.White
            )
        ) {
            Text("닫기", color = ComposeColor.Black)
        }
    }
}
