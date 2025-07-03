package com.mars.ultimatecleaner.domain.model

data class JunkFile(
    val path: String,
    val name: String,
    val size: Long,
    val category: String,
    val lastModified: Long
)

data class LargeFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String
)

data class EmptyFolder(
    val path: String,
    val name: String,
    val lastModified: Long
)

data class ApkFile(
    val path: String,
    val name: String,
    val size: Long,
    val packageName: String,
    val versionName: String,
    val isInstalled: Boolean,
    val lastModified: Long
)

data class CacheFile(
    val path: String,
    val name: String,
    val size: Long,
    val category: String,
    val lastModified: Long
)

data class TempFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long
)

data class ResidualFile(
    val path: String,
    val name: String,
    val size: Long,
    val packageName: String,
    val lastModified: Long
)
