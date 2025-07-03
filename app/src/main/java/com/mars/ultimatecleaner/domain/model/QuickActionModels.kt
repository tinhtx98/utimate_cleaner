package com.mars.ultimatecleaner.domain.model

sealed class QuickAction {
    abstract val title: String
    abstract val description: String

    data class QuickClean(
        override val title: String,
        override val description: String,
        val estimatedSpaceSaved: Long
    ) : QuickAction()

    data class ScanJunkFiles(
        override val title: String,
        override val description: String
    ) : QuickAction()

    data class FindDuplicates(
        override val title: String,
        override val description: String
    ) : QuickAction()

    data class ClearCache(
        override val title: String,
        override val description: String
    ) : QuickAction()
}

data class CleaningResult(
    val type: CleaningResultType,
    val filesDeleted: Int = 0,
    val spaceSaved: Long = 0L,
    val message: String = "",
    val filesFound: Int = 0,
    val totalSize: Long = 0L,
    val files: List<String> = emptyList()
) {
    companion object {
        fun InProgress(message: String) = CleaningResult(
            type = CleaningResultType.IN_PROGRESS,
            message = message
        )

        fun Success(filesDeleted: Int, spaceSaved: Long, message: String) = CleaningResult(
            type = CleaningResultType.SUCCESS,
            filesDeleted = filesDeleted,
            spaceSaved = spaceSaved,
            message = message
        )

        fun ScanComplete(filesFound: Int, totalSize: Long, files: List<String>) = CleaningResult(
            type = CleaningResultType.SCAN_COMPLETE,
            filesFound = filesFound,
            totalSize = totalSize,
            files = files
        )

        fun Error(message: String) = CleaningResult(
            type = CleaningResultType.ERROR,
            message = message
        )
    }
}

enum class CleaningResultType {
    IN_PROGRESS,
    SUCCESS,
    SCAN_COMPLETE,
    ERROR
}
