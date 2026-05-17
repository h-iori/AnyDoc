package com.ioristudios.anydoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioristudios.anydoc.model.FileItem
import com.ioristudios.anydoc.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FilesState {
    object Loading : FilesState()
    data class Success(val files: List<FileItem>) : FilesState()
    data class Error(val message: String) : FilesState()
}

class FilesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FilesState>(FilesState.Loading)
    val uiState: StateFlow<FilesState> = _uiState.asStateFlow()

    init {
        loadFiles()
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

    fun deleteFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (FileManager.deleteFile(path)) {
                // Reload files after successful deletion
                val currentFiles = (_uiState.value as? FilesState.Success)?.files ?: emptyList()
                val updatedFiles = currentFiles.filterNot { it.path == path }
                _uiState.value = FilesState.Success(updatedFiles)
            } else {
                _uiState.value = FilesState.Error("Failed to delete file")
                loadFiles() // reload to reset state
            }
        }
    }
}
