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
    private val questRepo = QuestRepository()

    var categoryMinutes: MutableMap<String, Int> = mutableMapOf()
        private set
    var categoryApps: MutableMap<String, MutableList<AppUsage>> = mutableMapOf()
        private set
    var totalUsage = 0
        private set

    var dailySummary = mutableStateOf("분석 중...")
        private set

    var questRecommendation = mutableStateOf("기록을 분석하고 있어...")
        private set

    fun loadUsageData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60 * 60 * 24
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

            val aggregatedStats = mutableMapOf<String, Long>()
            stats?.forEach {
                val current = aggregatedStats.getOrDefault(it.packageName, 0L)
                aggregatedStats[it.packageName] = current + it.totalTimeInForeground
            }

            val localCategoryMinutes = mutableMapOf<String, Int>()
            val localCategoryApps = mutableMapOf<String, MutableList<AppUsage>>()
            var total = 0
            val myUsedAppNames = mutableListOf<String>()

            withContext(Dispatchers.IO) {
                aggregatedStats.forEach { (pkg, time) ->
                    if (pkg == context.packageName) return@forEach

                    val minutes = (time / 60000L).toInt()
                    val appName = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }

                    myUsedAppNames.add(appName) // 사용한 앱 이름 수집

                    if (minutes < 1) return@forEach
                    val category = try { gpt.classifyApp(pkg) } catch (e: Exception) { "기타" }

                    localCategoryMinutes[category] = (localCategoryMinutes[category] ?: 0) + minutes
                    if (!localCategoryApps.containsKey(category)) localCategoryApps[category] = mutableListOf()
                    localCategoryApps[category]!!.add(AppUsage(pkg, appName, minutes))
                    total += minutes
                }
            }

            categoryMinutes = localCategoryMinutes
            categoryApps = localCategoryApps
            totalUsage = total

            // 1. 하루 요약 (비율 기반)
            dailySummary.value = gpt.generateDailySummary(localCategoryMinutes)

            // 2. 🔥 퀘스트 추천 (과거 기록 기반)
            val allQuests = questRepo.loadAllQuests()
            // 완료된(성공 or 실패) 퀘스트만 골라냄
            val history = allQuests.filter { it.status == "completed" }

            // 기록과 앱 목록을 함께 보냄
            questRecommendation.value = gpt.recommendQuestFromHistory(history, myUsedAppNames)
        }
    }
}

class UsageViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UsageViewModel(application) as T
    }
}