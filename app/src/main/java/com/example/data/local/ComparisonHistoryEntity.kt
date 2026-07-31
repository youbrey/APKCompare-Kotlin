package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comparison_history")
data class ComparisonHistoryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val apk1Name: String,
    val apk2Name: String,
    val packageName1: String,
    val packageName2: String,
    val riskLevelStr: String,
    val riskScore: Int,
    val summaryText: String,
    val reportJsonData: String
)
