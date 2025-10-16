package com.example.apptracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RankingActivity : AppCompatActivity() {

    private lateinit var tvMyRank: TextView
    private lateinit var tvMyUsername: TextView
    private lateinit var tvMyPoints: TextView
    private lateinit var rankingContainer: LinearLayout
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        tvMyRank = findViewById(R.id.tv_my_rank)
        tvMyUsername = findViewById(R.id.tv_my_username)
        tvMyPoints = findViewById(R.id.tv_my_points)
        rankingContainer = findViewById(R.id.rv_ranking_list)
        btnBack = findViewById(R.id.btn_back)

        // ✅ 뒤로가기 버튼 클릭 시 메인으로 이동
        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // ✅ 현재 사용자 정보만 표시
        val currentUserName = "demo_user"
        val currentRank = 1
        val currentPoints = intent.getStringExtra("points") ?: "포인트: 0점"

        // ✅ TextView에 한글 정상 표시
        tvMyUsername.text = "이름: $currentUserName"
        tvMyRank.text = "나의 랭킹: ${currentRank}위"
        tvMyPoints.text = currentPoints

        // ✅ 리스트뷰에 나만 표시
        rankingContainer.removeAllViews()
        val tv = TextView(this)
        tv.text = "👤 $currentUserName : ${currentPoints.replace("포인트: ", "")}"
        tv.textSize = 18f
        tv.setTextColor(android.graphics.Color.WHITE)
        rankingContainer.addView(tv)
    }
}
