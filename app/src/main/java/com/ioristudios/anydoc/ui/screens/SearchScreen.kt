package com.ioristudios.anydoc.ui.screens

import kotlin.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioristudios.anydoc.model.FileItem
import com.ioristudios.anydoc.ui.components.BottomNavBar
import com.ioristudios.anydoc.ui.components.FileListItem
import com.ioristudios.anydoc.ui.components.FilterChip
import com.ioristudios.anydoc.ui.components.SearchBar
import com.ioristudios.anydoc.ui.components.SelectionTopAppBar
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import com.ioristudios.anydoc.viewmodel.FilesState
import com.ioristudios.anydoc.viewmodel.FilesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    initialFilter: String = "All",
    onNavigate: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    viewModel: FilesViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    val filters = listOf("All", "PDF", "Word", "Excel", "PPT", "TXT", "Code")
    val spacing = rememberAppSpacing()
    var contentVisible by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItemIds by remember { mutableStateOf(setOf<String>()) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    val allFiles = (uiState as? FilesState.Success)?.files ?: emptyList()
    
    val filteredFiles = remember(allFiles, query, selectedFilter) {
        allFiles.filter { file ->
            val matchesQuery = file.name.contains(query, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "PDF" -> file.extension == "pdf"
                "Word" -> file.extension in listOf("doc", "docx")
                "Excel" -> file.extension in listOf("xls", "xlsx", "csv")
                "PPT" -> file.extension in listOf("ppt", "pptx")
                "TXT" -> file.extension in listOf("txt", "rtf")
                "Code" -> file.extension in listOf("md", "xml", "log", "html", "htm", "py", "kt", "java", "json", "cpp", "c", "h", "js", "css")
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LaunchedEffect(Unit) { contentVisible = true }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedItemIds.size,
                    totalCount = filteredFiles.size,
                    onSelectAllChange = { selectAll ->
                        selectedItemIds = if (selectAll) {
                            filteredFiles.map { it.id }.toSet()
                        } else {
                            emptySet()
                        }
                    },
                    onShare = { /* Dummy action */ },
                    onDelete = {
                        val count = selectedItemIds.size
                        val pathsToDelete = filteredFiles.filter { selectedItemIds.contains(it.id) }.map { it.path }
                        pathsToDelete.forEach { viewModel.deleteFile(it) }
                        selectedItemIds = emptySet()
                        isSelectionMode = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted $count files")
                        }
                    },
                    onCloseSelection = {
                        selectedItemIds = emptySet()
                        isSelectionMode = false
                    }
                )
            }
        },
        bottomBar = { BottomNavBar("search", onNavigate) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
        ) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.SurfaceBase.copy(alpha = 0.96f))
                        .statusBarsPadding()
                        .padding(horizontal = spacing.screenPadding, vertical = spacing.itemGap),
                    verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
                ) {
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.chipGap)) {
                        items(filters) { filter ->
                            FilterChip(
                                label = filter,
                                isSelected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }
                }
            }

            when (uiState) {
                is FilesState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is FilesState.Error -> {
                    item {
                        Text(
                            text = "Error loading files: ${(uiState as FilesState.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is FilesState.Success -> {
                    item {
                        AnimatedVisibility(
                            visible = contentVisible,
                            enter = fadeIn(tween(AppMotion.Normal, easing = AppMotion.StandardEasing)) +
                                slideInVertically(
                                    initialOffsetY = { it / 5 },
                                    animationSpec = tween(AppMotion.Normal, easing = AppMotion.DecelerateEasing)
                                )
                        ) {
                            Text(
                                text = "Matches Found (${filteredFiles.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(
                                    horizontal = spacing.screenPadding,
                                    vertical = spacing.itemGap
                                )
                            )
                        }
                    }

                    items(
                        items = filteredFiles,
                        key = { it.id }
                    ) { file ->
                        Column(modifier = Modifier
                            .padding(horizontal = spacing.screenPadding)
                            .animateItemPlacement(animationSpec = tween(AppMotion.Normal))
                        ) {
                            FileListItem(
                                fileItem = file,
                                index = filteredFiles.indexOf(file),
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItemIds.contains(file.id),
                                onSelectionChange = { selected ->
                                    selectedItemIds = if (selected) {
                                        selectedItemIds + file.id
                                    } else {
                                        selectedItemIds - file.id
                                    }
                                },
                                onLongClick = {
                                    isSelectionMode = true
                                    selectedItemIds = selectedItemIds + file.id
                                },
                                onClick = { onOpenFile(file.name) },
                                onDelete = { 
                                    viewModel.deleteFile(file.path)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("[Deleted] ${file.name}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
