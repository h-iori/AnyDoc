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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.ioristudios.anydoc.model.DummyRecentFiles
import com.ioristudios.anydoc.ui.components.BottomNavBar
import com.ioristudios.anydoc.ui.components.FileListItem
import com.ioristudios.anydoc.ui.components.FileTypeCard
import com.ioristudios.anydoc.ui.components.SelectionTopAppBar
import com.ioristudios.anydoc.ui.components.TopAppBar
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onSearchWithFilter: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val spacing = rememberAppSpacing()
    var contentVisible by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItemIds by remember { mutableStateOf(setOf<String>()) }
    
    LaunchedEffect(Unit) { contentVisible = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedItemIds.size,
                    totalCount = DummyRecentFiles.size,
                    onSelectAllChange = { selectAll ->
                        selectedItemIds = if (selectAll) {
                            DummyRecentFiles.map { it.id }.toSet()
                        } else {
                            emptySet()
                        }
                    },
                    onShare = { /* Dummy action */ },
                    onDelete = {
                        // Dummy action: Deselect all and exit mode
                        selectedItemIds = emptySet()
                        isSelectionMode = false
                    },
                    onCloseSelection = {
                        selectedItemIds = emptySet()
                        isSelectionMode = false
                    }
                )
            } else {
                TopAppBar(onMenuClick = onMenuClick)
            }
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
                    FileTypeGrid(onSearchWithFilter)
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

            items(
                count = DummyRecentFiles.size,
                key = { index -> DummyRecentFiles[index].id }
            ) { index ->
                val file = DummyRecentFiles[index]
                FileListItem(
                    fileItem = file,
                    index = index,
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
                    onClick = { onOpenFile(file.name) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FileTypeGrid(onTypeClick: (String) -> Unit) {
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
                    onClick = { onTypeClick(entry.first) }
                )
            }
        }
    }
}
