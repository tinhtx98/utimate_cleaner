package com.mars.ultimatecleaner.domain.model

data class StorageInfoDomain(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val usagePercentage: Float,
    val junkFilesCount: Int,
    val junkFilesSize: Long,
    val largeFilesCount: Int,
    val largeFilesSize: Long,
    val duplicateFilesCount: Int,
    val duplicateFilesSize: Long
)
