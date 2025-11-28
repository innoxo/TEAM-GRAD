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

    LaunchedEffect(true) {
        db.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<RankItem>()

                snapshot.children.forEach { userSnapshot ->
                    val name = userSnapshot.key ?: return
                    val points = userSnapshot.child("score").getValue(Int::class.java) ?: 0
                    tempList.add(RankItem(name, 0, points))
                }

                val sorted = tempList.sortedByDescending { it.points }
                val ranked = sorted.mapIndexed { index, item ->
                    item.copy(rank = index + 1)
                }

                rankingList = ranked
                myRank = ranked.find { it.username == currentNickname }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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
            text = "랭킹",
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