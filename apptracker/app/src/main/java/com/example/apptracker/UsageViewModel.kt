package com.example.apptracker

import android.app.Application
import android.app.usage.UsageStatsManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val gpt = OpenAIService(application)

    // Compose에서 관찰 가능한 상태 변수들 (StateFlow 패턴 통일)
    var categoryMinutes: MutableMap<String, Int> = mutableMapOf()
        private set

    var categoryApps: MutableMap<String, MutableList<AppUsage>> = mutableMapOf()
        private set

    var totalUsage = 0
        private set

    // UI가 관찰할 요약 메시지 상태 변수 (StateFlow 사용)
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

            // 데이터 분석 요청 로직
            if (localCategoryMinutes.isNotEmpty()) {
                val summary = try {
                    gpt.getDailySummary(localCategoryMinutes)
                } catch (e: Exception) {
                    "요약을 불러오지 못했습니다."
                }
                _dailySummary.value = summary
            } else {
                // '사용 기록 없음' 메시지 반영
                _dailySummary.value = "오늘 스마트폰 사용 기록이 없네요. 폰을 켜보세요!"
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