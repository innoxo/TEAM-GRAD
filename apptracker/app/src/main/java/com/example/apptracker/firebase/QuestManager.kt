// firebase�� �����Ͽ� �����ϴ� �ڵ� (����Ʈ ���� ����)
package com.example.apptracker.firebase

import com.example.apptracker.QuestItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object QuestManager {
    fun createAndSaveQuest(quest: QuestItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onFailure(Exception("�α����� �ʿ��մϴ�."))
            return
        }
        Firebase.database.getReference("quests").child(uid).push().setValue(quest)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}