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

    // 로비용: 모든 방 목록 실시간 감시
    fun observeRooms(onDataChanged: (List<Room>) -> Unit) {
        roomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Room>()

                snapshot.children.forEach { node ->
                    try {
                        val roomId = node.child("roomId").getValue(String::class.java) ?: ""
                        val title = node.child("title").getValue(String::class.java) ?: "제목 없음"
                        val mode = node.child("mode").getValue(String::class.java) ?: "coop"
                        val targetAppName = node.child("targetAppName").getValue(String::class.java) ?: ""
                        val targetPackage = node.child("targetPackage").getValue(String::class.java) ?: ""
                        val condition = node.child("condition").getValue(String::class.java) ?: "≤"
                        val creator = node.child("creator").getValue(String::class.java) ?: ""
                        val status = node.child("status").getValue(String::class.java) ?: "waiting"
                        val winner = node.child("winner").getValue(String::class.java) ?: ""

                        val goalMinutes = (node.child("goalMinutes").value as? Number)?.toInt() ?: 30
                        val startTime = (node.child("startTime").value as? Number)?.toLong() ?: 0L
                        val endTime = (node.child("endTime").value as? Number)?.toLong() ?: 0L

                        val participantsMap = HashMap<String, Participant>()
                        node.child("participants").children.forEach { pNode ->
                            val pName = pNode.child("nickname").getValue(String::class.java) ?: ""
                            val pReady = pNode.child("isReady").getValue(Boolean::class.java) ?: false
                            val pMinutes = (pNode.child("currentMinutes").value as? Number)?.toInt() ?: 0
                            val pScore = (pNode.child("score").value as? Number)?.toInt() ?: 0
                            val pClaimed = pNode.child("rewardClaimed").getValue(Boolean::class.java) ?: false

                            if (pName.isNotBlank()) {
                                participantsMap[pName] = Participant(pName, pReady, pMinutes, pScore, pClaimed)
                            }
                        }

                        val room = Room(
                            roomId, title, mode, targetAppName, targetPackage,
                            condition, goalMinutes, creator, status,
                            startTime, endTime, winner,
                            participantsMap
                        )

                        // 🔥 [수정됨] 완료된 방도 리스트에 포함시킵니다! (필터 제거)
                        list.add(room)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                list.sortByDescending { it.roomId }
                onDataChanged(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 대기실용
    fun observeRoomDetail(roomId: String, onUpdate: (Room?) -> Unit) {
        roomRef.child(roomId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(node: DataSnapshot) {
                try {
                    if (!node.exists()) {
                        onUpdate(null)
                        return
                    }

                    val rId = node.child("roomId").getValue(String::class.java) ?: ""
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
                    node.child("participants").children.forEach { pNode ->
                        val pName = pNode.child("nickname").getValue(String::class.java) ?: ""
                        val pReady = pNode.child("isReady").getValue(Boolean::class.java) ?: false
                        val pMin = (pNode.child("currentMinutes").value as? Number)?.toInt() ?: 0
                        val pScore = (pNode.child("score").value as? Number)?.toInt() ?: 0
                        val pClaimed = pNode.child("rewardClaimed").getValue(Boolean::class.java) ?: false

                        if (pName.isNotBlank()) {
                            pMap[pName] = Participant(pName, pReady, pMin, pScore, pClaimed)
                        }
                    }

                    val room = Room(rId, title, mode, tName, tPkg, cond, goal, creator, status, start, end, winner, pMap)
                    onUpdate(room)

                } catch (e: Exception) {
                    e.printStackTrace()
                    onUpdate(null)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 저장/수정 함수들 (동일)
    suspend fun createRoom(room: Room) { roomRef.child(room.roomId).setValue(room).await() }

    suspend fun joinRoom(roomId: String, nickname: String) {
        val participant = Participant(nickname, false, 0, 0, false)
        roomRef.child(roomId).child("participants").child(nickname).setValue(participant).await()
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

    suspend fun finishGame(roomId: String, status: String, winner: String = "") {
        val updates = mapOf<String, Any>("status" to status, "winner" to winner)
        roomRef.child(roomId).updateChildren(updates).await()
    }

    suspend fun claimReward(roomId: String, nickname: String, points: Int) {
        db.child("users").child(nickname).child("score").setValue(com.google.firebase.database.ServerValue.increment(points.toLong())).await()
        roomRef.child(roomId).child("participants").child(nickname).child("rewardClaimed").setValue(true).await()
    }
}