// 🔥 [수정됨] ai 폴더 안에 있으므로 패키지명을 .ai까지 붙여줘야 합니다.
package com.example.apptracker.ai

import com.example.apptracker.App
import com.example.apptracker.QuestItem
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

    private const val CATEGORY_COUNT = 6

    fun getRecommendedApps(
        installedApps: List<App>,
        historyQuests: List<QuestItem>
    ): List<App> {
        if (historyQuests.isEmpty()) return emptyList()

        val documents = installedApps.map {
            AppDocument(it.appName, it.packageName)
        }

        val vocabulary = buildVocabulary(documents)
        calculateTfIdf(documents, vocabulary)

        val k = CATEGORY_COUNT.coerceAtMost(documents.size)
        runKMeans(documents, k)

        val lastQuestPkg = historyQuests.maxByOrNull { it.startTime }?.targetPackage ?: return emptyList()
        val targetDoc = documents.find { it.packageName == lastQuestPkg } ?: return emptyList()

        val sameClusterApps = documents.filter { it.clusterIndex == targetDoc.clusterIndex }
        val newAppsInCluster = sameClusterApps.filter { it.packageName != lastQuestPkg }

        return if (newAppsInCluster.isNotEmpty()) {
            newAppsInCluster.shuffled().take(3).map { App(it.appName, it.packageName) }
        } else {
            listOf(App(targetDoc.appName, targetDoc.packageName))
        }
    }

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