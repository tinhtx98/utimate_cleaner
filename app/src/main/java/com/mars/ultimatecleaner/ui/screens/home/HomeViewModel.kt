package com.mars.ultimatecleaner.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mars.ultimatecleaner.domain.repository.FileRepository
import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.repository.AnalyticsRepository
import com.mars.ultimatecleaner.domain.usecase.AnalyzeStorageUseCase
import com.mars.ultimatecleaner.domain.usecase.GetSmartSuggestionsUseCase
import com.mars.ultimatecleaner.domain.usecase.GetRecentActivityUseCase
import com.mars.ultimatecleaner.domain.usecase.GetSystemHealthUseCase
import com.mars.ultimatecleaner.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val cleaningRepository: CleaningRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val analyzeStorageUseCase: AnalyzeStorageUseCase,
    private val getSmartSuggestionsUseCase: GetSmartSuggestionsUseCase,
    private val getRecentActivityUseCase: GetRecentActivityUseCase,
    private val getSystemHealthUseCase: GetSystemHealthUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadHomeData()
        startPeriodicUpdates()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val storageInfo = analyzeStorageUseCase()
                val suggestions = getSmartSuggestionsUseCase()
                val recentActivity = getRecentActivityUseCase()
                val systemHealth = getSystemHealthUseCase()

                _uiState.value = _uiState.value.copy(
                    storageInfo = storageInfo,
                    smartSuggestions = suggestions,
                    recentActivity = recentActivity,
                    systemHealth = systemHealth,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshData() {
        loadHomeData()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onFeatureClicked(feature: String) {
        viewModelScope.launch {
            analyticsRepository.trackFeatureClick(feature)
        }
    }

    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            // Update storage info every 30 seconds
            kotlinx.coroutines.delay(30000)
            if (!_isRefreshing.value) {
                loadHomeData()
            }
        }
    }
}

data class HomeUiState(
    val storageInfo: StorageInfo? = null,
    val smartSuggestions: List<SmartSuggestion> = emptyList(),
    val recentActivity: List<RecentActivity> = emptyList(),
    val systemHealth: SystemHealth? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class StorageInfo(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val usagePercentage: Float,
    val categoryBreakdown: Map<String, Long>
)

data class SmartSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val potentialSavings: Long,
    val priority: SuggestionPriority,
    val actionType: SuggestionAction
)

data class RecentActivity(
    val id: String,
    val action: String,
    val timestamp: Long,
    val result: String,
    val spaceSaved: Long
)

data class SystemHealth(
    val storageHealth: Int,
    val memoryHealth: Int,
    val batteryHealth: Int,
    val performanceHealth: Int,
    val overallScore: Int
)

enum class SuggestionPriority { LOW, MEDIUM, HIGH, CRITICAL }
enum class SuggestionAction { CLEAN_CACHE, DELETE_DUPLICATES, REMOVE_LARGE_FILES, OPTIMIZE_PHOTOS }