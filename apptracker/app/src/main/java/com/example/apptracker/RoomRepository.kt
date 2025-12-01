package com.example.apptracker

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

class RoomRepository {

    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    private val roomRef = db.child("rooms")

    fun observeRooms(onDataChanged: (List<Room>) -> Unit) {
        roomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Room>()
                snapshot.children.forEach { node ->
                    try {
                        val room = parseRoom(node)
                        // 종료된 방은 목록에서 숨김 (선택 사항)
                        if (room.status == "waiting" || room.status == "active") {
                            list.add(room)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                list.sortByDescending { it.roomId }
                onDataChanged(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun observeRoomDetail(roomId: String, onUpdate: (Room?) -> Unit) {
        roomRef.child(roomId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(node: DataSnapshot) {
                try {
                    if (!node.exists()) { onUpdate(null); return }
                    onUpdate(parseRoom(node))
                } catch (e: Exception) { e.printStackTrace(); onUpdate(null) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔥 수동 파싱 함수 (중복 제거용)
    private fun parseRoom(node: DataSnapshot): Room {
        val roomId = node.child("roomId").getValue(String::class.java) ?: ""
        val title = node.child("title").getValue(String::class.java) ?: ""
        val mode = node.child("mode").getValue(String::class.java) ?: "coop"
        val tName = node.child("targetAppName").getValue(String::class.java) ?: ""
        val tPkg = node.child("targetPackage").getValue(String::class.java) ?: ""
        val cond = node.child("condition").getValue(String::class.java) ?: "≤"
        val creator = node.child("creator").getValue(String::class.java) ?: ""
        val status = node.child("status").getValue(String::class.java) ?: "waiting"
        val winner = node.child("winner").getValue(String::class.java) ?: ""

        val goal = (node.child("goalMinutes").value as? Number)?.toInt() ?: 30
        val start = (node.child("startTime").value as? Number)?.toLong() ?: 0L
        val end = (node.child("endTime").value as? Number)?.toLong() ?: 0L

        val pMap = HashMap<String, Participant>()
        node.child("participants").children.forEach { p ->
            val name = p.child("nickname").getValue(String::class.java) ?: ""
            val ready = p.child("isReady").getValue(Boolean::class.java) ?: false
            val mins = (p.child("currentMinutes").value as? Number)?.toInt() ?: 0
            val claimed = p.child("rewardClaimed").getValue(Boolean::class.java) ?: false
            if (name.isNotBlank()) pMap[name] = Participant(name, ready, mins, 0, claimed)
        }
        return Room(roomId, title, mode, tName, tPkg, cond, goal, creator, status, start, end, winner, pMap)
    }

    suspend fun createRoom(room: Room) { roomRef.child(room.roomId).setValue(room).await() }
    suspend fun joinRoom(roomId: String, nickname: String) {
        val p = Participant(nickname, false, 0, 0, false)
        roomRef.child(roomId).child("participants").child(nickname).setValue(p).await()
    }
    suspend fun toggleReady(roomId: String, nickname: String, isReady: Boolean) {
        roomRef.child(roomId).child("participants").child(nickname).child("isReady").setValue(isReady).await()
    }

    suspend fun startGame(roomId: String) {
        roomRef.child(roomId).child("status").setValue("active").await()
    }

    suspend fun updateParticipantProgress(roomId: String, nickname: String, minutes: Int) {
        roomRef.child(roomId).child("participants").child(nickname).child("currentMinutes").setValue(minutes).await()
    }

    // 🔥 [추가] 게임 종료 처리 (성공/실패/승자)
    suspend fun finishGame(roomId: String, status: String, winner: String = "") {
        val updates = mapOf<String, Any>(
            "status" to status,
            "winner" to winner
        )
        roomRef.child(roomId).updateChildren(updates).await()
    }

    // 🔥 [추가] 보상 지급 (내 점수 올리기 + 보상받음 표시)
    suspend fun claimReward(roomId: String, nickname: String, points: Int) {
        // 1. 내 점수 올리기
        db.child("users").child(nickname).child("score").setValue(com.google.firebase.database.ServerValue.increment(points.toLong())).await()
        // 2. 보상 받음 표시 (중복 수령 방지)
        roomRef.child(roomId).child("participants").child(nickname).child("rewardClaimed").setValue(true).await()
    }
}