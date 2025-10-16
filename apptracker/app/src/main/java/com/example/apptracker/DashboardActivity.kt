package com.example.apptracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard) // 레이아웃 파일 연결

        val rankingButton: Button = findViewById(R.id.btn_ranking)

        // '랭킹 보기' 버튼 클릭 시 이벤트 처리
        rankingButton.setOnClickListener {
            val sharedPref = getSharedPreferences("AppTrackerPrefs", Context.MODE_PRIVATE)  // 누적 포인트 가져옴
            val totalPoints = sharedPref.getLong("totalPoints", 0L) // 저장된 값이 없으면 기본값 0L

            // 이동 Intent 생성
            val intent = Intent(this, RankingActivity::class.java)

            // 포인트 전달
            intent.putExtra("CURRENT_USER_POINTS", totalPoints)

            // RankingActivity 실행
            startActivity(intent)
        }
    }
}
