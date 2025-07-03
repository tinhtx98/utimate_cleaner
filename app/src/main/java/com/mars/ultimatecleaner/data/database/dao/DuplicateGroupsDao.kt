package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.DuplicateGroupsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DuplicateGroupsDao {

    @Query("SELECT * FROM duplicate_groups WHERE is_resolved = 0 ORDER BY total_size DESC")
    fun getUnresolvedDuplicateGroups(): Flow<List<DuplicateGroupsEntity>>

    @Query("SELECT * FROM duplicate_groups WHERE is_resolved = 1 ORDER BY resolved_date DESC")
    fun getResolvedDuplicateGroups(): Flow<List<DuplicateGroupsEntity>>

    @Query("SELECT * FROM duplicate_groups WHERE file_hash = :hash")
    suspend fun getDuplicateGroupByHash(hash: String): DuplicateGroupsEntity?

    @Query("SELECT * FROM duplicate_groups WHERE file_type = :fileType AND is_resolved = 0 ORDER BY total_size DESC")
    fun getDuplicateGroupsByFileType(fileType: String): Flow<List<DuplicateGroupsEntity>>

    @Query("SELECT SUM(potential_savings) FROM duplicate_groups WHERE is_resolved = 0")
    fun getTotalPotentialSavings(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM duplicate_groups WHERE is_resolved = 0")
    fun getUnresolvedGroupCount(): Flow<Int>

    @Query("SELECT SUM(potential_savings) FROM duplicate_groups WHERE is_resolved = 1")
    fun getTotalResolvedSavings(): Flow<Long?>

    @Query("SELECT * FROM duplicate_groups WHERE analysis_date >= :startDate ORDER BY analysis_date DESC")
    fun getDuplicateGroupsSince(startDate: Long): Flow<List<DuplicateGroupsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuplicateGroup(duplicateGroup: DuplicateGroupsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuplicateGroups(duplicateGroups: List<DuplicateGroupsEntity>)

    @Update
    suspend fun updateDuplicateGroup(duplicateGroup: DuplicateGroupsEntity)

    @Query("UPDATE duplicate_groups SET is_resolved = 1, resolved_date = :resolvedDate, resolution_action = :action, updated_at = :updatedAt WHERE id = :id")
    suspend fun markAsResolved(id: String, resolvedDate: Long, action: String, updatedAt: Long)

    @Query("UPDATE duplicate_groups SET keep_file_path = :keepFilePath, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateKeepFile(id: String, keepFilePath: String, updatedAt: Long)

    @Delete
    suspend fun deleteDuplicateGroup(duplicateGroup: DuplicateGroupsEntity)

    @Query("DELETE FROM duplicate_groups WHERE id = :id")
    suspend fun deleteDuplicateGroupById(id: String)

    @Query("DELETE FROM duplicate_groups WHERE analysis_date < :cutoffDate")
    suspend fun deleteOldDuplicateGroups(cutoffDate: Long)

    @Query("DELETE FROM duplicate_groups WHERE is_resolved = 1")
    suspend fun deleteResolvedGroups()

    @Query("DELETE FROM duplicate_groups")
    suspend fun deleteAllDuplicateGroups()

    @Transaction
    suspend fun resolveDuplicateGroup(id: String, action: String, keepFilePath: String? = null) {
        val currentTime = System.currentTimeMillis()
        markAsResolved(id, currentTime, action, currentTime)
        keepFilePath?.let {
            updateKeepFile(id, it, currentTime)
        }
    }
}