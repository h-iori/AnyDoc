package com.ioristudios.anydoc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ioristudios.anydoc.model.FileItem
import com.ioristudios.anydoc.util.FileManager
import com.ioristudios.anydoc.util.RecentFilesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

sealed class FilesState {
    object Loading : FilesState()
    data class Success(val files: List<FileItem>) : FilesState()
    data class Error(val message: String) : FilesState()
}

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<FilesState>(FilesState.Loading)
    val uiState: StateFlow<FilesState> = _uiState.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()

    init {
        loadFiles()
        observeRecentFiles()
    }

    fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = FilesState.Loading
            try {
                val files = FileManager.scanFiles()
                _uiState.value = FilesState.Success(files)
            } catch (e: Exception) {
                _uiState.value = FilesState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    private fun observeRecentFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            RecentFilesManager.getRecentFiles(getApplication()).collectLatest { recentPaths ->
                val currentFiles = (_uiState.value as? FilesState.Success)?.files
                val recentItems = recentPaths.map { path ->
                    currentFiles?.find { it.path == path } ?: createStubFileItem(path)
                }
                _recentFiles.value = recentItems
            }
        }
    }

    private fun createStubFileItem(path: String): FileItem {
        val file = File(path)
        val name = file.name
        val extension = file.extension.lowercase()
        
        val sizeStr = if (file.exists()) FileManager.formatFileSize(file.length()) else "Unknown size"
        val metadataStr = if (file.exists()) FileManager.formatDate(file.lastModified()) else "Recent"

        return FileItem(
            id = path,
            name = name,
            extension = extension,
            size = sizeStr,
            metadata = metadataStr,
            path = path
        )
    }

    fun deleteFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (FileManager.deleteFile(path)) {
                // Reload files after successful deletion
                val currentFiles = (_uiState.value as? FilesState.Success)?.files ?: emptyList()
                val updatedFiles = currentFiles.filterNot { it.path == path }
                _uiState.value = FilesState.Success(updatedFiles)
                RecentFilesManager.removeRecentFile(getApplication(), path)
            } else {
                _uiState.value = FilesState.Error("Failed to delete file")
                loadFiles() // reload to reset state
            }
        }
    }

    fun markFileAsOpened(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RecentFilesManager.addRecentFile(getApplication(), path)
        }
    }
}
