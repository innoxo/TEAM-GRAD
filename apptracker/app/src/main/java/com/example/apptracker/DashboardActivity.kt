package com.example.apptracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard) // ���̾ƿ� ���� ����

        val rankingButton: Button = findViewById(R.id.btn_ranking)

        // '��ŷ ����' ��ư Ŭ�� �� �̺�Ʈ ó��
        rankingButton.setOnClickListener {
            val sharedPref = getSharedPreferences("AppTrackerPrefs", Context.MODE_PRIVATE)  // ���� ����Ʈ ������
            val totalPoints = sharedPref.getLong("totalPoints", 0L) // ����� ���� ������ �⺻�� 0L

            // �̵� Intent ����
            val intent = Intent(this, RankingActivity::class.java)

            // ����Ʈ ����
            intent.putExtra("CURRENT_USER_POINTS", totalPoints)

            // RankingActivity ����
            startActivity(intent)
        }
    }
}
