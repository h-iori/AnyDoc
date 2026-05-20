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
import android.net.Uri
import android.provider.OpenableColumns
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

    private fun resolveExternalStorageUri(uri: Uri): String? {
        if (uri.authority != "com.android.externalstorage.documents") return null
        val path = uri.path ?: return null
        
        val docIndex = path.indexOf("/document/")
        val documentId = if (docIndex != -1) {
            path.substring(docIndex + "/document/".length)
        } else {
            val treeIndex = path.indexOf("/tree/")
            if (treeIndex != -1) {
                path.substring(treeIndex + "/tree/".length)
            } else {
                return null
            }
        }
        
        val decodedId = Uri.decode(documentId)
        val parts = decodedId.split(":", limit = 2)
        if (parts.size == 2) {
            val type = parts[0]
            val relativePath = parts[1]
            val baseDir = if ("primary".equals(type, ignoreCase = true)) {
                "/storage/emulated/0"
            } else {
                "/storage/$type"
            }
            val file = File(baseDir, relativePath)
            if (file.exists()) {
                return file.absolutePath
            }
        }
        return null
    }

    private fun createStubFileItem(path: String): FileItem {
        var name = ""
        var extension = ""
        var sizeStr = "Unknown size"
        var metadataStr = "Recent"

        if (path.startsWith("content://") || path.startsWith("file://")) {
            val uri = Uri.parse(path)
            if (uri.scheme == "file") {
                val filePath = uri.path ?: path
                val file = File(filePath)
                name = file.name
                extension = file.extension.lowercase()
                sizeStr = if (file.exists()) FileManager.formatFileSize(file.length()) else "Unknown size"
                metadataStr = if (file.exists()) FileManager.formatDate(file.lastModified()) else "Recent"
            } else if (uri.scheme == "content") {
                val resolvedPath = resolveExternalStorageUri(uri)
                val resolvedFile = resolvedPath?.let { File(it) }
                if (resolvedFile != null && resolvedFile.exists()) {
                    name = resolvedFile.name
                    extension = resolvedFile.extension.lowercase()
                    sizeStr = FileManager.formatFileSize(resolvedFile.length())
                    metadataStr = FileManager.formatDate(resolvedFile.lastModified())
                } else {
                    var displayName = ""
                    var sizeValue: Long = -1
                    var lastModifiedValue: Long = -1
                    
                    runCatching {
                        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            val lastModifiedIndex = cursor.getColumnIndex("last_modified")

                            if (cursor.moveToFirst()) {
                                if (nameIndex != -1) {
                                    displayName = cursor.getString(nameIndex) ?: ""
                                }
                                if (sizeIndex != -1) {
                                    sizeValue = cursor.getLong(sizeIndex)
                                }
                                if (lastModifiedIndex != -1) {
                                    lastModifiedValue = cursor.getLong(lastModifiedIndex)
                                }
                            }
                        }
                    }
                    
                    if (displayName.isNotBlank()) {
                        name = displayName
                        extension = displayName.substringAfterLast('.', "").lowercase()
                    } else {
                        val lastSegment = uri.lastPathSegment
                        name = if (!lastSegment.isNullOrBlank()) Uri.decode(lastSegment) else path
                        extension = name.substringAfterLast('.', "").lowercase()
                    }
                    
                    if (sizeValue > 0) {
                        sizeStr = FileManager.formatFileSize(sizeValue)
                    }
                    
                    if (lastModifiedValue > 0) {
                        metadataStr = FileManager.formatDate(lastModifiedValue)
                    }
                }
            }
        } else {
            val file = File(path)
            name = file.name
            extension = file.extension.lowercase()
            sizeStr = if (file.exists()) FileManager.formatFileSize(file.length()) else "Unknown size"
            metadataStr = if (file.exists()) FileManager.formatDate(file.lastModified()) else "Recent"
        }

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
            val isContentUri = path.startsWith("content://") || path.startsWith("file://")
            val deleted = if (isContentUri) false else FileManager.deleteFile(path)
            
            if (deleted) {
                val currentFiles = (_uiState.value as? FilesState.Success)?.files ?: emptyList()
                val updatedFiles = currentFiles.filterNot { it.path == path }
                _uiState.value = FilesState.Success(updatedFiles)
                RecentFilesManager.removeRecentFile(getApplication(), path)
            } else {
                RecentFilesManager.removeRecentFile(getApplication(), path)
                if (!isContentUri && File(path).exists()) {
                    _uiState.value = FilesState.Error("Failed to delete file")
                }
                loadFiles()
            }
        }
    }

    fun markFileAsOpened(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RecentFilesManager.addRecentFile(getApplication(), path)
        }
    }
}
