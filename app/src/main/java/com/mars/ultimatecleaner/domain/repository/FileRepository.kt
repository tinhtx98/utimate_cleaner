package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface FileRepository {

    // File System Operations
    suspend fun scanFiles(directory: File): Flow<FileScanProgress>
    suspend fun getFilesByCategory(category: FileCategoryDomain): List<FileInfo>
    suspend fun getFilesByType(mimeType: String): List<FileInfo>
    suspend fun searchFiles(query: String, criteria: SearchCriteria): List<FileInfo>
    suspend fun getFileInfo(filePath: String): FileInfo?

    // Metadata Extraction
    suspend fun extractMetadata(filePath: String): FileMetadata
    suspend fun extractExifData(imagePath: String): ExifData?
    suspend fun analyzeFileContent(filePath: String): ContentAnalysis
    suspend fun getFileSignature(filePath: String): String

    // File Organization
    suspend fun categorizeFiles(files: List<FileInfo>): Map<FileCategoryDomain, List<FileInfo>>
    suspend fun suggestFileOrganization(directory: File): OrganizationSuggestion
    suspend fun organizeFiles(organizationPlan: OrganizationPlan): Flow<OrganizationProgress>
    suspend fun createDirectoryStructure(structure: DirectoryStructure)

    // Duplicate Detection
    suspend fun findDuplicates(directory: File): Flow<DuplicateDetectionProgress>
    suspend fun findDuplicatesByHash(): List<DuplicateGroup>
    suspend fun findDuplicatesByName(): List<DuplicateGroup>
    suspend fun findDuplicatesBySize(): List<DuplicateGroup>
    suspend fun compareSimilarFiles(file1: String, file2: String): SimilarityScore

    // File Operations
    suspend fun moveFile(sourcePath: String, destinationPath: String): FileOperationResult
    suspend fun copyFile(sourcePath: String, destinationPath: String): FileOperationResult
    suspend fun deleteFile(filePath: String): FileOperationResult
    suspend fun renameFile(oldPath: String, newPath: String): FileOperationResult

    // File Integrity
    suspend fun validateFileIntegrity(filePath: String): FileIntegrityResult
    suspend fun calculateFileChecksum(filePath: String, algorithm: String = "SHA-256"): String
    suspend fun verifyFileSignature(filePath: String): SignatureVerificationResult

    // File Recovery
    suspend fun scanForDeletedFiles(): List<DeletedFileInfo>
    suspend fun recoverDeletedFile(fileInfo: DeletedFileInfo): FileRecoveryResult
    suspend fun getRecoveryProbability(fileInfo: DeletedFileInfo): Float

    // File Statistics
    suspend fun getDirectoryStats(directory: File): DirectoryStats
    suspend fun getFileTypeDistribution(): Map<String, FileTypeStats>
    suspend fun getLargestFiles(limit: Int = 100): List<FileInfo>
    suspend fun getNewestFiles(limit: Int = 100): List<FileInfo>
    suspend fun getOldestFiles(limit: Int = 100): List<FileInfo>
}