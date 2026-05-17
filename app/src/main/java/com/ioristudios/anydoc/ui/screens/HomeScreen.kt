package com.ioristudios.anydoc.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioristudios.anydoc.ui.components.BottomNavBar
import com.ioristudios.anydoc.ui.components.FileListItem
import com.ioristudios.anydoc.ui.components.FileTypeCard
import com.ioristudios.anydoc.ui.components.TopAppBar
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import com.ioristudios.anydoc.viewmodel.FilesState
import com.ioristudios.anydoc.viewmodel.FilesViewModel

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onSearchWithFilter: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onMenuClick: () -> Unit = {},
    viewModel: FilesViewModel = viewModel()
) {
    val spacing = rememberAppSpacing()
    var contentVisible by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val allFiles = (uiState as? FilesState.Success)?.files ?: emptyList()
    val fileCounts = remember(allFiles, uiState) {
        if (allFiles.isEmpty() && uiState is FilesState.Loading) null
        else {
            val counts = mutableMapOf<String, Int>()
            counts["All Files"] = allFiles.size
            counts["PDF"] = allFiles.count { it.extension == "pdf" }
            counts["Word"] = allFiles.count { it.extension in listOf("doc", "docx") }
            counts["Excel"] = allFiles.count { it.extension in listOf("xls", "xlsx", "csv") }
            counts["PPT"] = allFiles.count { it.extension in listOf("ppt", "pptx") }
            counts["TXT"] = allFiles.count { it.extension in listOf("txt", "rtf") }
            counts["Code"] = allFiles.count { it.extension in listOf("md", "xml", "log", "html", "htm", "py", "kt", "java", "json", "cpp", "c", "h", "js", "css", "ts", "tsx", "jsx", "cs", "go", "rs", "swift", "php", "rb", "scala", "yaml", "yml", "toml", "ini", "gradle", "sql", "sh", "bat", "ps1", "r", "lua", "dart", "vue", "svelte", "env", "cfg", "conf", "properties", "makefile") }
            counts
        }
    }

    LaunchedEffect(Unit) {
        contentVisible = true
        viewModel.loadFiles()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(onMenuClick = onMenuClick)
        },
        bottomBar = { BottomNavBar("home", onNavigate) }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.screenPadding),
            contentPadding = PaddingValues(top = spacing.itemGap, bottom = spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
        ) {
            item {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(AppMotion.Normal, easing = AppMotion.StandardEasing)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(AppMotion.Normal, easing = AppMotion.DecelerateEasing)
                        )
                ) {
                    FileTypeGrid(onSearchWithFilter, fileCounts)
                }
            }
            item { Spacer(modifier = Modifier.size(spacing.sectionGap)) }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, contentDescription = "Recent", tint = AppColors.BrandStrong)
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(0.96f)
                            .padding(start = spacing.itemGap)
                    )
                }
            }

            val recentFiles = allFiles.take(5)
            if (uiState is FilesState.Loading) {
                item {
                    Text(
                        text = "Loading recent documents...",
                        modifier = Modifier.padding(top = 16.dp).alpha(0.6f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (recentFiles.isEmpty()) {
                item {
                    Text(
                        text = "No recent documents found",
                        modifier = Modifier.padding(top = 16.dp).alpha(0.6f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                items(recentFiles) { file ->
                    FileListItem(
                        fileItem = file,
                        index = recentFiles.indexOf(file),
                        isSelectionMode = false,
                        isSelected = false,
                        onSelectionChange = {},
                        onLongClick = {},
                        onClick = { onOpenFile(file.name) },
                        onDelete = { viewModel.deleteFile(file.path) }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FileTypeGrid(
    onTypeClick: (String) -> Unit,
    fileCounts: Map<String, Int>?
) {
    val spacing = rememberAppSpacing()
    val types = listOf(
        Pair("All Files", "folder"),
        Pair("PDF", "sample.pdf"),
        Pair("Word", "sample.docx"),
        Pair("Excel", "sample.xlsx"),
        Pair("PPT", "sample.pptx"),
        Pair("TXT", "sample.txt"),
        Pair("Code", "sample.kt")
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
        Text(
            text = "Browse by Type",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // All Files Card (Full Width)
        val allFiles = types.first()
        FileTypeCard(
            title = allFiles.first,
            fileNameForIcon = allFiles.second,
            modifier = Modifier.fillMaxWidth(),
            fileCount = fileCounts?.get(allFiles.first),
            horizontalAlignment = Alignment.CenterHorizontally,
            onClick = { onTypeClick("All") }
        )

        // Other Cards (2 per row)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
            maxItemsInEachRow = 2
        ) {
            types.drop(1).forEach { entry ->
                FileTypeCard(
                    title = entry.first,
                    fileNameForIcon = entry.second,
                    modifier = Modifier.fillMaxWidth(0.485f),
                    fileCount = fileCounts?.get(entry.first),
                    onClick = { onTypeClick(entry.first) }
                )
            }
        }
    }
}
