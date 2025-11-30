package com.example.apptracker

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

data class AppDocument(
    val appName: String,
    val packageName: String,
    var tfIdfVector: DoubleArray = doubleArrayOf(),
    var clusterIndex: Int = -1
)

object AppClusteringEngine {

    // OpenAIService.kt의 카테고리 수와 동일하게 설정 - 6개
    private const val CATEGORY_COUNT = 6

    /**
     * 추천 함수
     * @param installedApps 설치된 앱 목록 (전체 데이터셋)
     * @param historyQuests 과거 퀘스트 기록 (사용자 선호도 파악용)
     */
    fun getRecommendedApps(
        installedApps: List<App>,
        historyQuests: List<QuestItem>
    ): List<App> {
        // 1. 과거 기록 없으면 추천 안 함
        if (historyQuests.isEmpty()) return emptyList()

        // 2. 문서화 (앱 이름 + 패키지명 토큰화)
        val documents = installedApps.map {
            AppDocument(it.appName, it.packageName)
        }

        // 3. TF-IDF 벡터
        val vocabulary = buildVocabulary(documents)
        calculateTfIdf(documents, vocabulary)

        // 4. K-Means 군집화
        // 앱 개수가 카테고리 수보다 적을 경우, 앱 개수만큼만 클러스터 생성
        val k = CATEGORY_COUNT.coerceAtMost(documents.size)
        runKMeans(documents, k)

        // 5. 추천 로직
        // 가장 최근에 수행한 퀘스트의 앱을 찾음
        val lastQuestPkg = historyQuests.maxByOrNull { it.startTime }?.targetPackage ?: return emptyList()
        val targetDoc = documents.find { it.packageName == lastQuestPkg } ?: return emptyList()

        // 같은 클러스터에 속한 앱들 필터링
        val sameClusterApps = documents.filter { it.clusterIndex == targetDoc.clusterIndex }

        // 1순위: 같은 클러스터 내에서 아직 퀘스트를 수행하지 않은 다른 앱
        val newAppsInCluster = sameClusterApps.filter { it.packageName != lastQuestPkg }

        return if (newAppsInCluster.isNotEmpty()) {
            // 추천할 다른 앱이 있다면 그 중에서 랜덤 3개 추천
            newAppsInCluster.shuffled().take(3).map { App(it.appName, it.packageName) }
        } else {
            // 2순위: 다른 앱이 없다면(이미 다 했거나 해당 군집에 앱이 하나뿐인 경우), 동일한 앱이라도 추천
            // (사용자가 해당 앱을 집중적으로 관리하고 싶어하는 니즈 반영)
            listOf(App(targetDoc.appName, targetDoc.packageName))
        }
    }

    // 내부 알고리즘 (TF-IDF & K-Means)

    private fun buildVocabulary(docs: List<AppDocument>): List<String> {
        return docs.flatMap { tokenize(it) }.distinct().sorted()
    }

    private fun tokenize(doc: AppDocument): List<String> {
        val text = "${doc.appName} ${doc.packageName}"
        return text.split(Regex("[^a-zA-Z0-9가-힣]"))
            .filter { it.length > 1 }
            .map { it.lowercase() }
    }

    private fun calculateTfIdf(docs: List<AppDocument>, vocab: List<String>) {
        val idf = vocab.map { term ->
            val count = docs.count { tokenize(it).contains(term) }
            ln(docs.size.toDouble() / (count + 1))
        }

        docs.forEach { doc ->
            val tokens = tokenize(doc)
            val vector = DoubleArray(vocab.size)
            vocab.forEachIndexed { i, term ->
                val tf = tokens.count { it == term }.toDouble() / tokens.size
                vector[i] = tf * idf[i]
            }
            doc.tfIdfVector = vector
        }
    }

    private fun runKMeans(docs: List<AppDocument>, k: Int) {
        if (docs.isEmpty()) return

        val centroids = Array(k) { docs[Random.nextInt(docs.size)].tfIdfVector.clone() }
        var changed = true
        var iter = 0

        while (changed && iter < 20) {
            changed = false
            iter++

            docs.forEach { doc ->
                var bestCluster = 0
                var maxSim = -1.0
                centroids.forEachIndexed { i, centroid ->
                    val sim = cosineSimilarity(doc.tfIdfVector, centroid)
                    if (sim > maxSim) {
                        maxSim = sim
                        bestCluster = i
                    }
                }
                if (doc.clusterIndex != bestCluster) {
                    doc.clusterIndex = bestCluster
                    changed = true
                }
            }

            for (i in 0 until k) {
                val members = docs.filter { it.clusterIndex == i }
                if (members.isNotEmpty()) {
                    val newCentroid = DoubleArray(centroids[0].size)
                    for (j in newCentroid.indices) {
                        newCentroid[j] = members.sumOf { it.tfIdfVector[j] } / members.size
                    }
                    centroids[i] = newCentroid
                }
            }
        }
    }

    private fun cosineSimilarity(v1: DoubleArray, v2: DoubleArray): Double {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            normA += v1[i].pow(2)
            normB += v2[i].pow(2)
        }
        return if (normA > 0 && normB > 0) dot / (sqrt(normA) * sqrt(normB)) else 0.0
    }
}