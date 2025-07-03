package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.FileMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileMetadataDao {

    @Query("SELECT * FROM file_metadata WHERE path = :path")
    suspend fun getFileMetadata(path: String): FileMetadataEntity?

    @Query("SELECT * FROM file_metadata WHERE mime_type LIKE :mimeTypePattern ORDER BY last_modified DESC")
    fun getFilesByMimeType(mimeTypePattern: String): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE size >= :minSize ORDER BY size DESC")
    fun getLargeFiles(minSize: Long): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE md5_hash = :hash")
    suspend fun getFilesByHash(hash: String): List<FileMetadataEntity>

    @Query("SELECT * FROM file_metadata WHERE analysis_status = :status")
    fun getFilesByAnalysisStatus(status: String): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE name LIKE '%' || :query || '%' OR path LIKE '%' || :query || '%' ORDER BY last_modified DESC")
    fun searchFiles(query: String): Flow<List<FileMetadataEntity>>

    @Query("SELECT COUNT(*) FROM file_metadata WHERE mime_type LIKE :mimeTypePattern")
    fun getFileCountByMimeType(mimeTypePattern: String): Flow<Int>

    @Query("SELECT SUM(size) FROM file_metadata WHERE mime_type LIKE :mimeTypePattern")
    fun getTotalSizeByMimeType(mimeTypePattern: String): Flow<Long?>

    @Query("SELECT DISTINCT md5_hash FROM file_metadata WHERE md5_hash IS NOT NULL GROUP BY md5_hash HAVING COUNT(*) > 1")
    suspend fun getDuplicateHashes(): List<String>

    @Query("SELECT * FROM file_metadata WHERE is_media_file = 1 AND analysis_status = 'PENDING' LIMIT :limit")
    suspend fun getPendingMediaAnalysis(limit: Int = 100): List<FileMetadataEntity>

    @Query("UPDATE file_metadata SET analysis_status = :status, analysis_result = :result, updated_at = :updatedAt WHERE path = :path")
    suspend fun updateAnalysisResult(path: String, status: String, result: Map<String, Any>?, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileMetadata(fileMetadata: FileMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileMetadataList(fileMetadataList: List<FileMetadataEntity>)

    @Update
    suspend fun updateFileMetadata(fileMetadata: FileMetadataEntity)

    @Delete
    suspend fun deleteFileMetadata(fileMetadata: FileMetadataEntity)

    @Query("DELETE FROM file_metadata WHERE path = :path")
    suspend fun deleteFileMetadataByPath(path: String)

    @Query("DELETE FROM file_metadata WHERE last_modified < :cutoffTime")
    suspend fun deleteOldFileMetadata(cutoffTime: Long)

    @Query("DELETE FROM file_metadata")
    suspend fun deleteAllFileMetadata()

    @Transaction
    suspend fun upsertFileMetadata(fileMetadata: FileMetadataEntity) {
        val existing = getFileMetadata(fileMetadata.path)
        if (existing != null) {
            updateFileMetadata(fileMetadata.copy(updatedAt = System.currentTimeMillis()))
        } else {
            insertFileMetadata(fileMetadata)
        }
    }

    @Transaction
    suspend fun batchUpdateAnalysisStatus(paths: List<String>, status: String) {
        val currentTime = System.currentTimeMillis()
        paths.forEach { path ->
            updateAnalysisResult(path, status, null, currentTime)
        }
    }
}