package com.example.apptracker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RankingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking) // activity_ranking.xml 페이지

        // DashboardActivity에서 포인트 가져옴
        val currentUserPoints = intent.getLongExtra("CURRENT_USER_POINTS", 0L)

        // 화면상의 TextView 가져옴
        val myRankTextView: TextView = findViewById(R.id.tv_my_rank)
        val myNameTextView: TextView = findViewById(R.id.tv_my_username) // 사용자 이름 TextView 추가 (레이아웃에 ID가 tv_my_username으로 되어 있다고 가정)
        val myPointsTextView: TextView = findViewById(R.id.tv_my_points)

        // 현재 랭킹 상 데이터가 하나만 있기 때문에 1위로 고정 설정해뒀음
        myRankTextView.text = "내 순위: 1위"
        myNameTextView.text = "나" // 사용자 이름은 임의대로 "나"라고 설정함
        myPointsTextView.text = "내 점수: ${currentUserPoints}점"
    }
}
