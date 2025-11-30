package com.example.apptracker

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

class QuestRepository {

    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    // 현재 닉네임 (없으면 demo_user)
    private val uid get() = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "demo_user"

    // 🔥 [핵심] v3로 경로 변경 + 날짜 폴더 제거
    private val questRef get() = db.child("quests_v3").child(uid)

    // 실시간 감시
    fun observeQuests(onDataChanged: (List<QuestItem>) -> Unit) {
        questRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableListOf<QuestItem>()
                try {
                    // 🔥 [수정됨] 날짜 폴더 없이 바로 퀘스트들을 가져옵니다. (단순화)
                    snapshot.children.forEach { questNode ->
                        val item = questNode.getValue(QuestItem::class.java)
                        if (item != null) {
                            result.add(item)
                        }
                    }
                    onDataChanged(result)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun saveQuest(quest: QuestItem) {
        // 날짜 폴더 없이 ID로 바로 저장
        questRef.child(quest.id).setValue(quest).await()
    }

    suspend fun deleteQuest(id: String) {
        questRef.child(id).removeValue().await()
    }
}