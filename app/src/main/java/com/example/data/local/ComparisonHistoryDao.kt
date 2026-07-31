package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ComparisonHistoryDao {

    @Query("SELECT * FROM comparison_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ComparisonHistoryEntity>>

    @Query("SELECT * FROM comparison_history WHERE id = :id")
    suspend fun getHistoryById(id: String): ComparisonHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: ComparisonHistoryEntity)

    @Query("DELETE FROM comparison_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    @Query("DELETE FROM comparison_history")
    suspend fun clearAllHistory()
}
