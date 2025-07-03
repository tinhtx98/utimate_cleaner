package com.mars.ultimatecleaner.domain.model

sealed class FileOperationResult {
    data class Success(val message: String) : FileOperationResult()
    data class Error(val message: String) : FileOperationResult()
    data class Progress(
        val current: Int,
        val total: Int,
        val message: String
    ) : FileOperationResult()
}
