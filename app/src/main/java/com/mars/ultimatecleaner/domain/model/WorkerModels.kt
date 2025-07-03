package com.mars.ultimatecleaner.domain.model

data class WorkerResult(
    val status: WorkerStatus,
    val message: String? = null,
    val operationId: String,
    val details: Map<String, Any>? = null,
    val error: String? = null
)

enum class WorkerStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILURE,
    CANCELLED
}

data class WorkerProgress(
    val percentage: Int,
    val message: String,
    val currentItem: String? = null,
    val itemsProcessed: Int = 0,
    val totalItems: Int = 0
)

data class ScheduledWorkInfo(
    val workName: String,
    val isScheduled: Boolean,
    val nextRunTime: Long? = null,
    val lastRunTime: Long? = null,
    val runCount: Int = 0,
    val state: androidx.work.WorkInfo.State,
    val tags: Set<String> = emptySet()
)

data class WorkerConfiguration(
    val isEnabled: Boolean = true,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
    val requiresBatteryNotLow: Boolean = true,
    val requiresStorageNotLow: Boolean = true,
    val networkType: androidx.work.NetworkType = androidx.work.NetworkType.NOT_REQUIRED,
    val backoffPolicy: androidx.work.BackoffPolicy = androidx.work.BackoffPolicy.EXPONENTIAL,
    val backoffDelayMillis: Long = 30000L,
    val maxRetryAttempts: Int = 3
)