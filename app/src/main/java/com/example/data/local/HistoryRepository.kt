package com.example.data.local

import com.example.data.model.ApkComparisonReport
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dao: ComparisonHistoryDao) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(ApkComparisonReport::class.java)

    val historyList: Flow<List<ApkComparisonReport>> = dao.getAllHistory().map { list ->
        list.mapNotNull { entity ->
            try {
                adapter.fromJson(entity.reportJsonData)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveReport(report: ApkComparisonReport) {
        val json = adapter.toJson(report)
        val entity = ComparisonHistoryEntity(
            id = report.id,
            timestamp = report.timestamp,
            apk1Name = report.apk1.fileName,
            apk2Name = report.apk2.fileName,
            packageName1 = report.apk1.packageName,
            packageName2 = report.apk2.packageName,
            riskLevelStr = report.overallRiskLevel.name,
            riskScore = report.riskScore,
            summaryText = report.summaryText,
            reportJsonData = json
        )
        dao.insertHistory(entity)
    }

    suspend fun deleteReport(id: String) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }
}
