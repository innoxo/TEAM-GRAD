package com.example.apptracker

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class QuestRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val uid = "demo_user"

    suspend fun loadAllQuests(): List<QuestItem> {
        val snap = db.child("quests").child(uid).get().await()
        val result = mutableListOf<QuestItem>()

        snap.children.forEach { dateNode ->
            dateNode.children.forEach { questNode ->
                questNode.getValue(QuestItem::class.java)?.let {
                    result.add(it)
                }
            }
        }
        return result
    }

    suspend fun saveQuest(date: String, quest: QuestItem) {
        db.child("quests")
            .child(uid)
            .child(date)
            .child(quest.id)
            .setValue(quest)
    }

    suspend fun deleteQuest(date: String, id: String) {
        db.child("quests")
            .child(uid)
            .child(date)
            .child(id)
            .removeValue()
    }
}
