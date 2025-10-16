package com.example.apptracker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RankingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking) // activity_ranking.xml ������

        // DashboardActivity���� ����Ʈ ������
        val currentUserPoints = intent.getLongExtra("CURRENT_USER_POINTS", 0L)

        // ȭ����� TextView ������
        val myRankTextView: TextView = findViewById(R.id.tv_my_rank)
        val myNameTextView: TextView = findViewById(R.id.tv_my_username) // ����� �̸� TextView �߰� (���̾ƿ��� ID�� tv_my_username���� �Ǿ� �ִٰ� ����)
        val myPointsTextView: TextView = findViewById(R.id.tv_my_points)

        // ���� ��ŷ �� �����Ͱ� �ϳ��� �ֱ� ������ 1���� ���� �����ص���
        myRankTextView.text = "�� ����: 1��"
        myNameTextView.text = "��" // ����� �̸��� ���Ǵ�� "��"��� ������
        myPointsTextView.text = "�� ����: ${currentUserPoints}��"
    }
}
