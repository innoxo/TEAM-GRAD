package com.example.apptracker

import android.app.Application
import android.app.usage.UsageStatsManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase // 🔥 Firebase 추가
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow // 하루 한줄 요약용 추가
import kotlinx.coroutines.flow.asStateFlow     // 하루 한줄 요약용 추가

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val gpt = OpenAIService(application)

    // 🔥 점수 저장을 위한 DB 참조 추가
    private val db = FirebaseDatabase.getInstance(
        "https://apptrackerdemo-569ea-default-rtdb.firebaseio.com"
    ).reference

    var categoryMinutes: MutableMap<String, Int> = mutableMapOf()
        private set

    var categoryApps: MutableMap<String, MutableList<AppUsage>> = mutableMapOf()
        private set

    var totalUsage = 0
        private set

    // 추가된 부분: UI가 관찰할 요약 메시지 상태 변수
    private val _dailySummary = MutableStateFlow<String>("오늘의 분석을 기다리고 있어요...")
    val dailySummary = _dailySummary.asStateFlow()

    fun loadUsageData() {
        viewModelScope.launch {

            val context = getApplication<Application>()
            val pm = context.packageManager
            val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60 * 60 * 24

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime, endTime
            )

            val localCategoryMinutes = mutableMapOf<String, Int>()
            val localCategoryApps = mutableMapOf<String, MutableList<AppUsage>>()
            var total = 0

            withContext(Dispatchers.IO) {
                stats?.forEach { stat ->

                    val minutes = (stat.totalTimeInForeground / 60000L).toInt()
                    if (minutes < 1) return@forEach

                    val pkg = stat.packageName

                    val appName = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    } catch (e: Exception) {
                        pkg
                    }

                    val category = try {
                        gpt.classifyApp(pkg)
                    } catch (e: Exception) {
                        "기타"
                    }

                    localCategoryMinutes[category] =
                        (localCategoryMinutes[category] ?: 0) + minutes

                    if (!localCategoryApps.containsKey(category)) {
                        localCategoryApps[category] = mutableListOf()
                    }

                    localCategoryApps[category]!!.add(
                        AppUsage(pkg, appName, minutes)
                    )

                    total += minutes
                }
            }

            categoryMinutes = localCategoryMinutes
            categoryApps = localCategoryApps
            totalUsage = total

            // -------------------------------------------------------------
            // 🔥 [추가된 부분] 총 사용 시간이 계산되면 바로 Firebase 점수로 저장!
            // -------------------------------------------------------------
            val nickname = UserSession.nickname
            if (nickname.isNotBlank()) {
                // users -> 닉네임 -> score 경로에 totalUsage(분) 저장
                db.child("users").child(nickname).child("score").setValue(total)
            }

            // 추가된 부분: 데이터가 있으면 요약 요청
            if (localCategoryMinutes.isNotEmpty()) {
                // 백그라운드에서 GPT 호출
                val summary = try {
                    gpt.getDailySummary(localCategoryMinutes)
                } catch (e: Exception) {
                    "요약을 불러오지 못했습니다."
                }
                _dailySummary.value = summary
            } else {
                _dailySummary.value = "오늘 사용 기록이 없습니다."
            }
        }
    }
}

class UsageViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UsageViewModel(application) as T
    }
}