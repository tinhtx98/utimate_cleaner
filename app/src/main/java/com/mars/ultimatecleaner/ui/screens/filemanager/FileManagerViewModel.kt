package com.mars.ultimatecleaner.ui.screens.filemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mars.ultimatecleaner.domain.repository.FileRepository
import com.mars.ultimatecleaner.domain.usecase.GetFilesByCategoryUseCase
import com.mars.ultimatecleaner.domain.usecase.SearchFilesUseCase
import com.mars.ultimatecleaner.domain.usecase.FileOperationsUseCase
import com.mars.ultimatecleaner.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val getFilesByCategoryUseCase: GetFilesByCategoryUseCase,
    private val searchFilesUseCase: SearchFilesUseCase,
    private val fileOperationsUseCase: FileOperationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    init {
        loadFiles()
        setupSearch()
    }

    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        searchFiles(query)
                    } else {
                        loadFiles()
                    }
                }
        }
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val currentCategory = _uiState.value.selectedCategory
                val files = getFilesByCategoryUseCase(currentCategory)
                val categoryCounts = getCategoryCounts()

                _uiState.value = _uiState.value.copy(
                    files = files,
                    categoryCounts = categoryCounts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load files"
                )
            }
        }
    }

    fun selectCategory(category: FileCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        _selectedFiles.value = emptySet()
        loadFiles()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun searchFiles(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val searchResults = searchFilesUseCase(query, _uiState.value.selectedCategory)
                _uiState.value = _uiState.value.copy(
                    files = searchResults,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }

    fun toggleFileSelection(filePath: String) {
        val currentSelection = _selectedFiles.value
        _selectedFiles.value = if (currentSelection.contains(filePath)) {
            currentSelection - filePath
        } else {
            currentSelection + filePath
        }
    }

    fun selectAllFiles() {
        _selectedFiles.value = _uiState.value.files.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true)

            try {
                val selectedPaths = _selectedFiles.value.toList()
                val result = fileOperationsUseCase.deleteFiles(selectedPaths)

                if (result.isSuccess) {
                    _selectedFiles.value = emptySet()
                    loadFiles() // Refresh file list
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        operationResult = "Successfully deleted ${result.successCount} files"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        error = result.errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOperationInProgress = false,
                    error = e.message ?: "Delete operation failed"
                )
            }
        }
    }

    fun moveFiles(sourcePaths: List<String>, destinationPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true)

            try {
                val result = fileOperationsUseCase.moveFiles(sourcePaths, destinationPath)

                if (result.isSuccess) {
                    _selectedFiles.value = emptySet()
                    loadFiles()
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        operationResult = "Successfully moved ${result.successCount} files"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        error = result.errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOperationInProgress = false,
                    error = e.message ?: "Move operation failed"
                )
            }
        }
    }

    fun copyFiles(sourcePaths: List<String>, destinationPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true)

            try {
                val result = fileOperationsUseCase.copyFiles(sourcePaths, destinationPath)

                if (result.isSuccess) {
                    loadFiles()
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        operationResult = "Successfully copied ${result.successCount} files"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        error = result.errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOperationInProgress = false,
                    error = e.message ?: "Copy operation failed"
                )
            }
        }
    }

    fun renameFile(filePath: String, newName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true)

            try {
                val result = fileOperationsUseCase.renameFile(filePath, newName)

                if (result.isSuccess) {
                    loadFiles()
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        operationResult = "File renamed successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        error = result.errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOperationInProgress = false,
                    error = e.message ?: "Rename operation failed"
                )
            }
        }
    }

    fun createFolder(path: String, folderName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true)

            try {
                val result = fileOperationsUseCase.createFolder(path, folderName)

                if (result.isSuccess) {
                    loadFiles()
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        operationResult = "Folder created successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        error = result.errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOperationInProgress = false,
                    error = e.message ?: "Create folder failed"
                )
            }
        }
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            isGridView = !_uiState.value.isGridView
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissOperationResult() {
        _uiState.value = _uiState.value.copy(operationResult = null)
    }

    private suspend fun getCategoryCounts(): Map<FileCategory, Int> {
        return FileCategory.values().associateWith { category ->
            getFilesByCategoryUseCase(category).size
        }
    }
}

data class FileManagerUiState(
    val files: List<FileItem> = emptyList(),
    val selectedCategory: FileCategory = FileCategory.PHOTOS,
    val categoryCounts: Map<FileCategory, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = true,
    val isOperationInProgress: Boolean = false,
    val currentPath: String = "",
    val breadcrumbs: List<String> = emptyList(),
    val error: String? = null,
    val operationResult: String? = null
)

data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val isDirectory: Boolean = false,
    val thumbnailPath: String? = null
)

enum class FileCategory {
    PHOTOS, VIDEOS, DOCUMENTS, AUDIO, DOWNLOADS, ALL
}

data class FileOperationResult(
    val isSuccess: Boolean,
    val successCount: Int,
    val failedCount: Int,
    val errorMessage: String? = null
)