// MainActivity.kt  -> 수정본 (firebase에서 데이터를 받아와서 보여주는 코드)
// api 키의 보안성 문제와 이중으로 api를 불러 중복 결제, 앱이 무거워지는 것을 방지하기 위해 따로 만듦
// 기존 버전의 MainActivity.kt 파일은 동일 경로의 txt파일로 존재
package com.example.apptracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalUsage: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvPoints: TextView
    private lateinit var chart: PieChart
    private lateinit var btnQuest: Button
    
    // ❌ GPT 및 포인트 계산 관련 로직 모두 삭제

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvTotalUsage = findViewById(R.id.tv_total_usage)
        tvSummary = findViewById(R.id.tv_summary)
        tvPoints = findViewById(R.id.tv_points)
        chart = findViewById(R.id.pieChart)
        btnQuest = findViewById(R.id.btnQuest)

        btnQuest.setOnClickListener {
            startActivity(Intent(this, QuestActivity::class.java))
        }

        // ✅ Firebase에서 서버가 분석한 결과를 실시간으로 받아오는 리스너 설정
        setupFirebaseListeners()
    }

    private fun setupFirebaseListeners() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = Firebase.database.reference

        // 1. 포인트 정보 리스너
        db.child("users").child(uid).child("points").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val points = snapshot.getValue(Long::class.java) ?: 0L
                tvPoints.text = "포인트: ${points}점"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. 대시보드 정보 리스너
        db.child("dashboard").child(uid).child(today).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val totalMinutes = snapshot.child("totalMinutes").getValue(Long::class.java) ?: 0L
                val summary = snapshot.child("dailySummary").getValue(String::class.java) ?: "오늘의 활동 요약이 없습니다."
                
                tvTotalUsage.text = "오늘 총 사용시간: ${totalMinutes}분"
                tvSummary.text = summary
                
                // 제네릭 타입 문제 해결을 위해 명시적 캐스팅
                @Suppress("UNCHECKED_CAST")
                val categoryMinutes = snapshot.child("categoryMinutes").value as? Map<String, Long> ?: emptyMap()
                updatePieChart(categoryMinutes)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    private fun updatePieChart(categoryMinutes: Map<String, Long>) {
        val entries = categoryMinutes.filter { it.value > 0 }
            .map { (cat, minutes) -> PieEntry(minutes.toFloat(), cat) }

        if (entries.isEmpty()) {
            chart.clear()
            return
        }

        val dataSet = PieDataSet(entries, "오늘의 앱 사용 비율")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"), Color.parseColor("#03A9F4"),
            Color.parseColor("#9C27B0"), Color.parseColor("#FF9800"),
            Color.parseColor("#F44336"), Color.parseColor("#607D8B")
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        chart.data = PieData(dataSet)
        chart.setEntryLabelColor(Color.WHITE)
        chart.legend.textColor = Color.WHITE
        chart.setHoleColor(Color.TRANSPARENT)
        chart.description.isEnabled = false
        chart.invalidate()
    }
}