package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.CleaningHistoryEntity
import kotlinx.coroutines.flow.Flow

data class OperationTypeStat(
    @ColumnInfo(name = "operation_type") val operationType: String,
    @ColumnInfo(name = "count") val count: Int
)

@Dao
interface CleaningHistoryDao {

    @Query("SELECT * FROM cleaning_history ORDER BY timestamp DESC")
    fun getAllCleaningHistory(): Flow<List<CleaningHistoryEntity>>

    @Query("SELECT * FROM cleaning_history WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getCleaningHistoryByDateRange(startTime: Long, endTime: Long): Flow<List<CleaningHistoryEntity>>

    @Query("SELECT * FROM cleaning_history WHERE operation_type = :operationType ORDER BY timestamp DESC")
    fun getCleaningHistoryByType(operationType: String): Flow<List<CleaningHistoryEntity>>

    @Query("SELECT * FROM cleaning_history WHERE status = :status ORDER BY timestamp DESC")
    fun getCleaningHistoryByStatus(status: String): Flow<List<CleaningHistoryEntity>>

    @Query("SELECT SUM(space_saved) FROM cleaning_history WHERE timestamp >= :startTime")
    fun getTotalSpaceSavedSince(startTime: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM cleaning_history WHERE timestamp >= :startTime")
    fun getCleaningCountSince(startTime: Long): Flow<Int>

    @Query("SELECT AVG(space_saved) FROM cleaning_history WHERE timestamp >= :startTime")
    fun getAverageSpaceSaved(startTime: Long): Flow<Double?>

    @Query("SELECT * FROM cleaning_history WHERE id = :id")
    suspend fun getCleaningHistoryById(id: String): CleaningHistoryEntity?

    @Query("SELECT operation_type, COUNT(*) as count FROM cleaning_history GROUP BY operation_type")
    fun getOperationTypeStats(): Flow<List<OperationTypeStat>>

    @Query("SELECT * FROM cleaning_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCleaningHistory(limit: Int = 10): Flow<List<CleaningHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleaningHistory(cleaningHistory: CleaningHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleaningHistoryList(cleaningHistoryList: List<CleaningHistoryEntity>)

    @Update
    suspend fun updateCleaningHistory(cleaningHistory: CleaningHistoryEntity)

    @Delete
    suspend fun deleteCleaningHistory(cleaningHistory: CleaningHistoryEntity)

    @Query("DELETE FROM cleaning_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldCleaningHistory(cutoffTime: Long)

    @Query("DELETE FROM cleaning_history")
    suspend fun deleteAllCleaningHistory()

    @Transaction
    suspend fun insertOrUpdateCleaningHistory(cleaningHistory: CleaningHistoryEntity) {
        val existing = getCleaningHistoryById(cleaningHistory.id)
        if (existing != null) {
            updateCleaningHistory(cleaningHistory.copy(updatedAt = System.currentTimeMillis()))
        } else {
            insertCleaningHistory(cleaningHistory)
        }
    }
}