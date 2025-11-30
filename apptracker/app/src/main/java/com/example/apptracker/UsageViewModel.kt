package com.example.apptracker

import android.app.Application
import android.app.usage.UsageStatsManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val gpt = OpenAIService(application)

    var categoryMinutes: MutableMap<String, Int> = mutableMapOf()
        private set

    var categoryApps: MutableMap<String, MutableList<AppUsage>> = mutableMapOf()
        private set

    var totalUsage = 0
        private set

    // GPT 한줄평 저장 변수
    var dailySummary = mutableStateOf("오늘의 분석을 기다리는 중...")
        private set

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

            // 🔥 [수정 1] 먼저 같은 패키지명끼리 시간을 합칩니다 (Merge)
            val aggregatedStats = mutableMapOf<String, Long>()

            stats?.forEach { stat ->
                val pkg = stat.packageName
                val time = stat.totalTimeInForeground

                // 기존 값에 더하기
                val current = aggregatedStats.getOrDefault(pkg, 0L)
                aggregatedStats[pkg] = current + time
            }

            val localCategoryMinutes = mutableMapOf<String, Int>()
            val localCategoryApps = mutableMapOf<String, MutableList<AppUsage>>()
            var total = 0

            withContext(Dispatchers.IO) {
                // 🔥 [수정 2] 합쳐진 데이터를 가지고 분류 시작
                aggregatedStats.forEach { (pkg, totalTime) ->

                    val minutes = (totalTime / 60000L).toInt()
                    if (minutes < 1) return@forEach // 1분 미만은 무시

                    val appName = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    } catch (e: Exception) { pkg }

                    // 카테고리 분류
                    val category = try { gpt.classifyApp(pkg) } catch (e: Exception) { "기타" }

                    localCategoryMinutes[category] = (localCategoryMinutes[category] ?: 0) + minutes

                    if (!localCategoryApps.containsKey(category)) {
                        localCategoryApps[category] = mutableListOf()
                    }
                    localCategoryApps[category]!!.add(AppUsage(pkg, appName, minutes))

                    total += minutes
                }
            }

            // 데이터 갱신
            categoryMinutes = localCategoryMinutes
            categoryApps = localCategoryApps
            totalUsage = total

            // AI 한줄평 요청
            if (total > 0) {
                val aiComment = gpt.generateDailySummary(localCategoryMinutes)
                dailySummary.value = aiComment
            } else {
                dailySummary.value = "사용 기록이 없어요. 폰을 켜보세요!"
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