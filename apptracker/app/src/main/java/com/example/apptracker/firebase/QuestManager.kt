// firebase와 연결하여 관리하는 코드 (퀘스트 저장 전담)
package com.example.apptracker.firebase

import com.example.apptracker.QuestItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object QuestManager {
    fun createAndSaveQuest(quest: QuestItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onFailure(Exception("로그인이 필요합니다."))
            return
        }
        Firebase.database.getReference("quests").child(uid).push().setValue(quest)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}