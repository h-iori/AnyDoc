package com.ioristudios.anydoc.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioristudios.anydoc.model.BrowseItem
import com.ioristudios.anydoc.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FileBrowserState {
    object Loading : FileBrowserState()
    data class Success(val items: List<BrowseItem>) : FileBrowserState()
    data class Error(val message: String) : FileBrowserState()
}

class FileBrowserViewModel : ViewModel() {

    val rootPath: String = Environment.getExternalStorageDirectory().absolutePath

    private val _currentPath = MutableStateFlow(rootPath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _uiState = MutableStateFlow<FileBrowserState>(FileBrowserState.Loading)
    val uiState: StateFlow<FileBrowserState> = _uiState.asStateFlow()

    init {
        loadDirectory(rootPath)
    }

    fun navigateInto(folderPath: String) {
        _currentPath.value = folderPath
        loadDirectory(folderPath)
    }

    /**
     * Navigate up one level. Returns true if we went up,
     * false if we're already at root (caller should handle back nav).
     */
    fun navigateUp(): Boolean {
        val current = _currentPath.value
        if (current == rootPath) return false
        val parent = java.io.File(current).parentFile?.absolutePath ?: rootPath
        _currentPath.value = parent
        loadDirectory(parent)
        return true
    }

    fun isAtRoot(): Boolean = _currentPath.value == rootPath

    /**
     * Returns list of path segments for breadcrumb display.
     * e.g. "/storage/emulated/0/Documents/Work" → ["Internal Storage", "Documents", "Work"]
     */
    fun getBreadcrumbs(): List<Pair<String, String>> {
        val current = _currentPath.value
        if (current == rootPath) return listOf("Internal Storage" to rootPath)

        val relativePath = current.removePrefix(rootPath).trimStart('/')
        val segments = relativePath.split("/")
        val breadcrumbs = mutableListOf("Internal Storage" to rootPath)
        var accumulated = rootPath
        for (segment in segments) {
            accumulated = "$accumulated/$segment"
            breadcrumbs.add(segment to accumulated)
        }
        return breadcrumbs
    }

    private fun loadDirectory(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = FileBrowserState.Loading
            try {
                val items = FileManager.listDirectory(path)
                _uiState.value = FileBrowserState.Success(items)
            } catch (e: Exception) {
                _uiState.value = FileBrowserState.Error(e.localizedMessage ?: "Failed to read directory")
            }
        }
    }
}
