package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.database.*
// 뒤로가기 버튼 디자인 개선을 위해 추가
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon

private val PrimaryColor = Color(0xFF00695C)
private val BackgroundColor = Color(0xFFF5F7F6)

@Composable
fun RankingScreen(navController: NavHostController) {

    val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    var rankingList by remember { mutableStateOf(listOf<RankItem>()) }
    var myRank by remember { mutableStateOf<RankItem?>(null) }
    val currentNickname = UserSession.nickname

    LaunchedEffect(true) {
        db.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<RankItem>()
                snapshot.children.forEach { userSnapshot ->
                    val name = userSnapshot.key ?: return@forEach
                    val points = (userSnapshot.child("score").value as? Number)?.toInt() ?: 0
                    tempList.add(RankItem(name, 0, points))
                }
                val sorted = tempList.sortedByDescending { it.points }
                val ranked = sorted.mapIndexed { index, item -> item.copy(rank = index + 1) }
                rankingList = ranked
                myRank = ranked.find { it.username == currentNickname }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
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
            Text("명예의 전당", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(Modifier.height(20.dp))

        // 내 랭킹 (강조)
        if (myRank != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("나의 순위", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("${myRank!!.rank}위", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${myRank!!.points} P", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("전체 순위", color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // 리스트
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rankingList) { item ->
                RankCard(item)
            }
        }
    }
}

@Composable
fun RankCard(item: RankItem) {
    // 등수별 색상 및 아이콘
    val rankColor = when (item.rank) {
        1 -> Color(0xFFFFD700) // 금
        2 -> Color(0xFFC0C0C0) // 은
        3 -> Color(0xFFCD7F32) // 동
        else -> PrimaryColor
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${item.rank}", color = rankColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.width(30.dp))
                Text(item.username, color = Color.Black, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }
            Text("${item.points} P", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

data class RankItem(
    val username: String,
    val rank: Int,
    val points: Int
)