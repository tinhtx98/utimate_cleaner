package com.mars.ultimatecleaner.ui.screens.optimizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.repository.*
import com.mars.ultimatecleaner.domain.usecase.*
import com.mars.ultimatecleaner.data.worker.manager.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OptimizerViewModel @Inject constructor(
    private val optimizerRepository: OptimizerRepository,
    private val storageRepository: StorageRepository,
    private val systemHealthRepository: SystemHealthRepository,
    private val cleaningRepository: CleaningRepository,
    private val getDeviceHealthUseCase: GetDeviceHealthUseCase,
    private val optimizePerformanceUseCase: OptimizePerformanceUseCase,
    private val analyzeStorageUseCase: AnalyzeStorageUseCase,
    private val findDuplicatesUseCase: FindDuplicatesUseCase,
    private val workerScheduler: WorkerScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizerUiState())
    val uiState: StateFlow<OptimizerUiState> = _uiState.asStateFlow()

    private val _optimizationProgress = MutableStateFlow<OptimizationProgress?>(null)
    val optimizationProgress: StateFlow<OptimizationProgress?> = _optimizationProgress.asStateFlow()

    init {
        loadInitialData()
        observeOptimizationTasks()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val deviceHealth = getDeviceHealthUseCase()
                val storageAnalysis = analyzeStorageUseCase()
                val optimizationHistory = optimizerRepository.getOptimizationHistory(limit = 10)
                val recommendations = generateRecommendations(deviceHealth, storageAnalysis)

                _uiState.value = _uiState.value.copy(
                    deviceHealth = deviceHealth,
                    storageAnalysis = storageAnalysis,
                    recommendations = recommendations,
                    optimizationHistory = optimizationHistory,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun performQuickOptimization() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isOptimizing = true)
                _optimizationProgress.value = OptimizationProgress(
                    currentStep = "Initializing quick optimization...",
                    progress = 0,
                    totalSteps = 5
                )

                // Step 1: Clear cache
                _optimizationProgress.value = _optimizationProgress.value?.copy(
                    currentStep = "Clearing cache files...",
                    progress = 20
                )
                val cacheResult = cleaningRepository.clearCache()

                // Step 2: Clean junk files
                _optimizationProgress.value = _optimizationProgress.value?.copy(
                    currentStep = "Removing junk files...",
                    progress = 40
                )
                val junkResult = cleaningRepository.cleanJunkFiles()

                // Step 3: Optimize memory
                _optimizationProgress.value = _optimizationProgress.value?.copy(
                    currentStep = "Optimizing memory...",
                    progress = 60
                )
                val memoryResult = optimizePerformanceUseCase(OptimizationType.MEMORY)

                // Step 4: Clean temporary files
                _optimizationProgress.value = _optimizationProgress.value?.copy(
                    currentStep = "Cleaning temporary files...",
                    progress = 80
                )
                val tempResult = cleaningRepository.cleanTempFiles()

                // Step 5: Complete
                _optimizationProgress.value = _optimizationProgress.value?.copy(
                    currentStep = "Optimization complete!",
                    progress = 100
                )

                val totalSpaceSaved = cacheResult.spaceSaved + junkResult.spaceSaved + tempResult.spaceSaved
                val totalFilesRemoved = cacheResult.filesDeleted + junkResult.filesDeleted + tempResult.filesDeleted

                val optimizationResult = OptimizationResult(
                    type = OptimizationType.QUICK,
                    spaceSaved = totalSpaceSaved,
                    filesProcessed = totalFilesRemoved,
                    duration = System.currentTimeMillis() - (_optimizationProgress.value?.startTime ?: 0),
                    improvements = listOf(
                        "Freed ${formatFileSize(totalSpaceSaved)} of storage",
                        "Removed $totalFilesRemoved unnecessary files",
                        "Improved system responsiveness"
                    )
                )

                // Save optimization result
                optimizerRepository.saveOptimizationResult(optimizationResult)

                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    lastOptimizationResult = optimizationResult
                )

                // Refresh data after optimization
                loadInitialData()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    error = e.message
                )
                _optimizationProgress.value = null
            }
        }
    }

    fun performDeepOptimization() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isOptimizing = true)
                _optimizationProgress.value = OptimizationProgress(
                    currentStep = "Starting comprehensive optimization...",
                    progress = 0,
                    totalSteps = 10
                )

                var totalSpaceSaved = 0L
                var totalFilesProcessed = 0
                val improvements = mutableListOf<String>()

                // Step 1: Analyze storage
                updateProgress("Analyzing storage usage...", 10)
                val storageAnalysis = analyzeStorageUseCase()

                // Step 2: Find duplicates
                updateProgress("Scanning for duplicate files...", 20)
                val duplicates = findDuplicatesUseCase()
                if (duplicates.isNotEmpty()) {
                    improvements.add("Found ${duplicates.size} duplicate groups")
                }

                // Step 3: Clear cache
                updateProgress("Clearing application cache...", 30)
                val cacheResult = cleaningRepository.clearCache()
                totalSpaceSaved += cacheResult.spaceSaved
                totalFilesProcessed += cacheResult.filesDeleted

                // Step 4: Clean junk files
                updateProgress("Removing junk and temporary files...", 40)
                val junkResult = cleaningRepository.cleanJunkFiles()
                totalSpaceSaved += junkResult.spaceSaved
                totalFilesProcessed += junkResult.filesDeleted

                // Step 5: Optimize photos
                updateProgress("Analyzing photo quality...", 50)
                val photoOptimization = optimizerRepository.optimizePhotos()
                improvements.addAll(photoOptimization.suggestions)

                // Step 6: Clean app data
                updateProgress("Cleaning unused app data...", 60)
                val appDataResult = cleaningRepository.cleanUnusedAppData()
                totalSpaceSaved += appDataResult.spaceSaved

                // Step 7: Optimize performance
                updateProgress("Optimizing system performance...", 70)
                val performanceResult = optimizePerformanceUseCase(OptimizationType.COMPREHENSIVE)
                improvements.addAll(performanceResult.improvements)

                // Step 8: Clean large files
                updateProgress("Analyzing large files...", 80)
                val largeFiles = optimizerRepository.findLargeFiles()
                if (largeFiles.isNotEmpty()) {
                    improvements.add("Found ${largeFiles.size} large files for review")
                }

                // Step 9: Battery optimization
                updateProgress("Optimizing battery usage...", 90)
                val batteryOptimization = optimizerRepository.optimizeBattery()
                improvements.addAll(batteryOptimization.recommendations)

                // Step 10: Complete
                updateProgress("Deep optimization complete!", 100)

                val optimizationResult = OptimizationResult(
                    type = OptimizationType.DEEP,
                    spaceSaved = totalSpaceSaved,
                    filesProcessed = totalFilesProcessed,
                    duration = System.currentTimeMillis() - (_optimizationProgress.value?.startTime ?: 0),
                    improvements = improvements
                )

                optimizerRepository.saveOptimizationResult(optimizationResult)

                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    lastOptimizationResult = optimizationResult
                )

                loadInitialData()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    error = e.message
                )
                _optimizationProgress.value = null
            }
        }
    }

    fun scheduleOptimization(schedule: OptimizationSchedule) {
        viewModelScope.launch {
            try {
                when (schedule.type) {
                    OptimizationType.QUICK -> {
                        workerScheduler.scheduleAutoCleaningDaily()
                    }
                    OptimizationType.DEEP -> {
                        // Schedule weekly deep optimization
                        workerScheduler.scheduleWeeklyCacheCleanup()
                    }
                    else -> {
                        // Custom scheduling logic
                    }
                }

                optimizerRepository.saveOptimizationSchedule(schedule)

                _uiState.value = _uiState.value.copy(
                    scheduledOptimizations = _uiState.value.scheduledOptimizations + schedule
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun findDuplicates() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isAnalyzing = true)

                val duplicates = findDuplicatesUseCase()

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    duplicateGroups = duplicates
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = e.message
                )
            }
        }
    }

    fun analyzePhotos() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isAnalyzing = true)

                val photoAnalysis = optimizerRepository.analyzePhotos()

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    photoAnalysis = photoAnalysis
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = e.message
                )
            }
        }
    }

    fun optimizeBattery() {
        viewModelScope.launch {
            try {
                val batteryOptimization = optimizerRepository.optimizeBattery()

                _uiState.value = _uiState.value.copy(
                    batteryOptimization = batteryOptimization
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun dismissOptimizationResult() {
        _uiState.value = _uiState.value.copy(lastOptimizationResult = null)
        _optimizationProgress.value = null
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun updateProgress(step: String, progress: Int) {
        _optimizationProgress.value = _optimizationProgress.value?.copy(
            currentStep = step,
            progress = progress
        )
    }

    private fun observeOptimizationTasks() {
        // Observe background optimization tasks
        viewModelScope.launch {
            // Implementation for observing WorkManager tasks
        }
    }

    private fun generateRecommendations(
        deviceHealth: DeviceHealth,
        storageAnalysis: StorageAnalysis
    ): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()

        // Storage recommendations
        if (storageAnalysis.usagePercentage > 80) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.STORAGE_CLEANUP,
                    title = "Free Up Storage Space",
                    description = "Your storage is ${storageAnalysis.usagePercentage.toInt()}% full. Clean up unnecessary files.",
                    estimatedBenefit = "Free up to ${formatFileSize(storageAnalysis.cleanableSpace)}",
                    priority = if (storageAnalysis.usagePercentage > 90) Priority.HIGH else Priority.MEDIUM
                )
            )
        }

        // Performance recommendations
        if (deviceHealth.overallScore < 70) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.PERFORMANCE_BOOST,
                    title = "Boost Performance",
                    description = "Your device performance can be improved. Run optimization.",
                    estimatedBenefit = "Improve responsiveness by up to 40%",
                    priority = if (deviceHealth.overallScore < 50) Priority.HIGH else Priority.MEDIUM
                )
            )
        }

        // Battery recommendations
        if (deviceHealth.batteryHealth < 80) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.BATTERY_OPTIMIZATION,
                    title = "Optimize Battery Usage",
                    description = "Reduce battery drain by optimizing background apps.",
                    estimatedBenefit = "Extend battery life by 20-30%",
                    priority = Priority.MEDIUM
                )
            )
        }

        return recommendations
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }
}