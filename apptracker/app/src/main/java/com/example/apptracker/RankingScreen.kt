package com.example.apptracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.database.*

private val BgColor = Color(0xFF00462A)
private val SurfaceColor = Color.White
private val TextPrimary = Color.Black
private val TextSecondary = Color(0xFF444444)

@Composable
fun RankingScreen(navController: NavHostController) {

    val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    var rankingList by remember { mutableStateOf(listOf<RankItem>()) }
    var myRank by remember { mutableStateOf<RankItem?>(null) }
    val currentNickname = UserSession.nickname

    // 실시간 감시 & Firebase 서버 정렬 사용
    DisposableEffect(Unit) {
        // 점수("score") 기준으로 정렬하고, 상위 100명(limitToLast)만 가져옴 - 부하 방지
        // 파이어베이스는 오름차순 정렬만 지원, 나중에 뒤집어야(reverse) 1등이 위로 옴
        val query = db.child("users").orderByChild("score").limitToLast(100)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<RankItem>()

                // 데이터 파싱
                snapshot.children.forEach { userSnapshot ->
                    val name = userSnapshot.key ?: return@forEach
                    // score가 없는 경우 0으로 처리
                    val points = userSnapshot.child("score").getValue(Int::class.java) ?: 0
                    tempList.add(RankItem(name, 0, points)) // 등수는 나중에 매김
                }

                // Firebase는 오름차순(점수 낮은 순)으로 주므로 뒤집어야 내림차순(1등부터)이 됨
                // 서버 정렬 기반
                val sortedList = tempList.reversed()

                // 등수 생성
                val rankedList = sortedList.mapIndexed { index, item ->
                    item.copy(rank = index + 1)
                }

                rankingList = rankedList

                // 내 등수 찾기 (상위 100위에 없으면 별도 처리 필요하지만, 여기선 리스트 내에서 찾음)
                myRank = rankedList.find { it.username == currentNickname }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        query.addValueEventListener(listener)

        // 화면을 나갈 때 리스너 제거 (메모리 누수 방지)
        onDispose {
            query.removeEventListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
    ) {
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("뒤로가기", color = TextPrimary)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "실시간 랭킹 (TOP 100)", // 텍스트 변경
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(20.dp))

        if (myRank != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("내 정보", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("이름: ${myRank!!.username}", color = TextPrimary)
                    Text("랭킹: ${myRank!!.rank}위", color = TextPrimary)
                    Text("포인트: ${myRank!!.points}점", color = TextPrimary)
                }
            }
        } else {
            // 랭킹에 없을 경우 (신규 유저 등)
            Text(
                "아직 랭킹 데이터가 없거나 100위 밖입니다.",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "전체 랭킹",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rankingList) { item ->
                RankCard(item)
            }
        }
    }
}

@Composable
fun RankCard(item: RankItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${item.rank}위", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(item.username, color = TextSecondary)
            }
            Text("${item.points}점", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

data class RankItem(
    val username: String,
    val rank: Int,
    val points: Int
)