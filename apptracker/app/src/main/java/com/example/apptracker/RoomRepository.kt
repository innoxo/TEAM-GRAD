package com.example.apptracker

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
                        val room = node.getValue(Room::class.java)
                        if (room != null) list.add(room)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                list.sortByDescending { it.roomId }
                onDataChanged(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 대기실용: 특정 방 하나만 실시간 감시
    fun observeRoomDetail(roomId: String, onUpdate: (Room?) -> Unit) {
        roomRef.child(roomId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(Room::class.java)
                onUpdate(room)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun createRoom(room: Room) {
        roomRef.child(room.roomId).setValue(room).await()
    }

    suspend fun joinRoom(roomId: String, nickname: String) {
        // 이미 있는지 확인 안 하고 그냥 덮어쓰면 점수 초기화될 수 있으니 주의
        // 여기서는 간단하게 처리
        val participant = Participant(nickname = nickname, isReady = false, currentMinutes = 0)
        roomRef.child(roomId).child("participants").child(nickname).setValue(participant).await()
    }

    suspend fun toggleReady(roomId: String, nickname: String, isReady: Boolean) {
        roomRef.child(roomId).child("participants").child(nickname).child("isReady").setValue(isReady).await()
    }

    suspend fun startGame(roomId: String) {
        roomRef.child(roomId).child("status").setValue("active").await()
        // 게임 시작 시간 기록 (동기화를 위해)
        roomRef.child(roomId).child("startTime").setValue(System.currentTimeMillis()).await()
    }

    // 🔥 [추가됨] 내 진행 상황(사용 시간) 업데이트
    suspend fun updateParticipantProgress(roomId: String, nickname: String, minutes: Int) {
        roomRef.child(roomId).child("participants").child(nickname).child("currentMinutes")
            .setValue(minutes).await()
    }
}