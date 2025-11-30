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

    private val uid get() = if(UserSession.nickname.isNotBlank()) UserSession.nickname else "demo_user"

    // v3 경로 사용
    private val questRef get() = db.child("quests_v3").child(uid)

    // 1. 실시간 감시 (화면 표시용)
    fun observeQuests(onDataChanged: (List<QuestItem>) -> Unit) {
        questRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableListOf<QuestItem>()
                try {
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

    // 🔥 [복구됨] 2. 한 번만 불러오기 (추천 알고리즘 분석용)
    // 이 함수가 없어서 에러가 났던 겁니다!
    suspend fun loadAllQuests(): List<QuestItem> {
        val result = mutableListOf<QuestItem>()
        try {
            // get().await()를 써서 딱 한 번만 가져옵니다.
            val snap = questRef.get().await()
            snap.children.forEach { questNode ->
                val item = questNode.getValue(QuestItem::class.java)
                if (item != null) {
                    result.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    suspend fun saveQuest(quest: QuestItem) {
        questRef.child(quest.id).setValue(quest).await()
    }

    suspend fun deleteQuest(id: String) {
        questRef.child(id).removeValue().await()
    }
}