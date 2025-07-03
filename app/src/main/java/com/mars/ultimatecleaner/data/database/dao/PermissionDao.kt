package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.permission.PermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionDao {

    @Query("SELECT * FROM permission_history ORDER BY timestamp DESC")
    fun getAllPermissionHistory(): Flow<List<PermissionEntity>>

    @Query("SELECT * FROM permission_history WHERE permission_type = :permissionType ORDER BY timestamp DESC")
    fun getPermissionHistoryByType(permissionType: String): Flow<List<PermissionEntity>>

    @Query("SELECT * FROM permission_history WHERE is_granted = :isGranted ORDER BY timestamp DESC")
    fun getPermissionHistoryByStatus(isGranted: Boolean): Flow<List<PermissionEntity>>

    @Query("SELECT * FROM permission_history WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getPermissionHistoryByDateRange(startTime: Long, endTime: Long): Flow<List<PermissionEntity>>

    @Query("SELECT * FROM permission_history WHERE id = :id")
    suspend fun getPermissionById(id: String): PermissionEntity?

    @Query("SELECT COUNT(*) FROM permission_history WHERE permission_type = :permissionType AND is_granted = :isGranted")
    suspend fun getPermissionCount(permissionType: String, isGranted: Boolean): Int

    @Query("SELECT * FROM permission_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPermissionHistory(limit: Int = 10): Flow<List<PermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissionHistory(permission: PermissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissionHistoryList(permissions: List<PermissionEntity>)

    @Update
    suspend fun updatePermissionHistory(permission: PermissionEntity)

    @Delete
    suspend fun deletePermissionHistory(permission: PermissionEntity)

    @Query("DELETE FROM permission_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldPermissionHistory(cutoffTime: Long)

    @Query("DELETE FROM permission_history")
    suspend fun deleteAllPermissionHistory()

    @Transaction
    suspend fun insertOrUpdatePermissionHistory(permission: PermissionEntity) {
        val existing = getPermissionById(permission.id)
        if (existing != null) {
            updatePermissionHistory(permission.copy(updatedAt = System.currentTimeMillis()))
        } else {
            insertPermissionHistory(permission)
        }
    }
}
