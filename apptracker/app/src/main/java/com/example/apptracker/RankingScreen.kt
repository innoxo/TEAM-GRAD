package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/* ----------------------------------------------------
   RankingScreen
----------------------------------------------------- */
@Composable
fun RankingScreen(navController: NavHostController) {

    val rankingList = listOf(
        RankItem("demo_user", 1, 350),
        RankItem("user_B", 2, 240),
        RankItem("user_C", 3, 120)
    )

    val myRank = rankingList.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp)
    ) {

        // 🔙 뒤로가기 버튼
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Surface),
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("뒤로가기", color = Color.Black)   // 버튼 텍스트 블랙
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "랭킹",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(20.dp))

        /* -------------------------------
           내 랭킹 카드
        -------------------------------- */
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("내 정보", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("이름: ${myRank.username}", color = AppColors.TextPrimary)
                Text("랭킹: ${myRank.rank}위", color = AppColors.TextPrimary)
                Text("포인트: ${myRank.points}점", color = AppColors.TextPrimary)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "전체 랭킹",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(8.dp))

        /* -------------------------------
           랭킹 리스트
        -------------------------------- */
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rankingList) { item ->
                RankCard(item)
            }
        }
    }
}

/* ----------------------------------------------------
   RankCard
----------------------------------------------------- */
@Composable
fun RankCard(item: RankItem) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${item.rank}위", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text(item.username, color = AppColors.TextSecondary)
            }
            Text("${item.points}점", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

/* ----------------------------------------------------
   RankItem 데이터
----------------------------------------------------- */
data class RankItem(
    val username: String,
    val rank: Int,
    val points: Int
)
