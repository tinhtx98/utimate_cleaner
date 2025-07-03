package com.mars.ultimatecleaner.ui.screens.cleaner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.repository.FileRepository
import com.mars.ultimatecleaner.domain.usecase.ScanJunkFilesUseCase
import com.mars.ultimatecleaner.domain.usecase.CleanFilesUseCase
import com.mars.ultimatecleaner.domain.usecase.DetectLargeFilesUseCase
import com.mars.ultimatecleaner.domain.usecase.FindEmptyFoldersUseCase
import com.mars.ultimatecleaner.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CleanerViewModel @Inject constructor(
    private val cleaningRepository: CleaningRepository,
    private val fileRepository: FileRepository,
    private val scanJunkFilesUseCase: ScanJunkFilesUseCase,
    private val cleanFilesUseCase: CleanFilesUseCase,
    private val detectLargeFilesUseCase: DetectLargeFilesUseCase,
    private val findEmptyFoldersUseCase: FindEmptyFoldersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _cleaningProgress = MutableStateFlow(0f)
    val cleaningProgress: StateFlow<Float> = _cleaningProgress.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanResults = null,
                error = null
            )

            try {
                scanJunkFilesUseCase()
                    .onEach { progress ->
                        _scanProgress.value = progress.percentage
                        _uiState.value = _uiState.value.copy(
                            currentScanCategory = progress.currentCategory,
                            scannedFiles = progress.scannedFiles
                        )
                    }
                    .collect { scanResult ->
                        if (scanResult.isComplete) {
                            _uiState.value = _uiState.value.copy(
                                isScanning = false,
                                scanResults = scanResult,
                                junkCategories = scanResult.junkCategories,
                                totalJunkSize = scanResult.totalSize
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = e.message ?: "Scan failed"
                )
            }
        }
    }

    fun cleanSelectedFiles(selectedCategories: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCleaning = true,
                error = null
            )

            try {
                cleanFilesUseCase(selectedCategories)
                    .onEach { progress ->
                        _cleaningProgress.value = progress.percentage
                        _uiState.value = _uiState.value.copy(
                            currentCleaningCategory = progress.currentCategory,
                            cleanedFiles = progress.cleanedFiles,
                            spaceSaved = progress.spaceSaved
                        )
                    }
                    .collect { cleaningResult ->
                        if (cleaningResult.isComplete) {
                            _uiState.value = _uiState.value.copy(
                                isCleaning = false,
                                cleaningResults = cleaningResult,
                                totalSpaceSaved = cleaningResult.totalSpaceSaved
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCleaning = false,
                    error = e.message ?: "Cleaning failed"
                )
            }
        }
    }

    fun toggleCategorySelection(categoryId: String) {
        val currentState = _uiState.value
        val updatedCategories = currentState.junkCategories.map { category ->
            if (category.id == categoryId) {
                category.copy(isSelected = !category.isSelected)
            } else {
                category
            }
        }

        _uiState.value = currentState.copy(
            junkCategories = updatedCategories,
            selectedSize = updatedCategories.filter { it.isSelected }.sumOf { it.size }
        )
    }

    fun setLargeFileThreshold(thresholdMB: Int) {
        viewModelScope.launch {
            cleaningRepository.setLargeFileThreshold(thresholdMB)
            // Trigger rescan if results are already available
            if (_uiState.value.scanResults != null) {
                startScan()
            }
        }
    }

    fun clearResults() {
        _uiState.value = CleanerUiState()
        _scanProgress.value = 0f
        _cleaningProgress.value = 0f
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CleanerUiState(
    val isScanning: Boolean = false,
    val isCleaning: Boolean = false,
    val scanResults: ScanResult? = null,
    val cleaningResults: CleaningResult? = null,
    val junkCategories: List<JunkCategoryCleanerViewModel> = emptyList(),
    val currentScanCategory: String = "",
    val currentCleaningCategory: String = "",
    val scannedFiles: Int = 0,
    val cleanedFiles: Int = 0,
    val totalJunkSize: Long = 0,
    val selectedSize: Long = 0,
    val spaceSaved: Long = 0,
    val totalSpaceSaved: Long = 0,
    val largeFileThreshold: Int = 100, // MB
    val error: String? = null
)

data class ScanResult(
    val junkCategories: List<JunkCategoryCleanerViewModel>,
    val totalSize: Long,
    val totalFiles: Int,
    val scanDuration: Long,
    val isComplete: Boolean
)

data class CleaningResult(
    val cleanedCategories: List<String>,
    val totalSpaceSaved: Long,
    val cleanedFiles: Int,
    val failedFiles: Int,
    val cleaningDuration: Long,
    val isComplete: Boolean
)

data class JunkCategoryCleanerViewModel(
    val id: String,
    val name: String,
    val description: String,
    val size: Long,
    val fileCount: Int,
    val isSelected: Boolean = true,
    val subCategories: List<JunkSubCategory> = emptyList()
)

data class JunkSubCategory(
    val name: String,
    val size: Long,
    val fileCount: Int,
    val files: List<JunkFile> = emptyList()
)

data class JunkFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val canDelete: Boolean = true
)

data class ScanProgress(
    val percentage: Float,
    val currentCategory: String,
    val scannedFiles: Int
)

data class CleaningProgress(
    val percentage: Float,
    val currentCategory: String,
    val cleanedFiles: Int,
    val spaceSaved: Long
)