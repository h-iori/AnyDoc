package com.ioristudios.anydoc.ui.screens

import kotlin.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ioristudios.anydoc.model.DummySearchFiles
import com.ioristudios.anydoc.ui.components.BottomNavBar
import com.ioristudios.anydoc.ui.components.FileListItem
import com.ioristudios.anydoc.ui.components.FilterChip
import com.ioristudios.anydoc.ui.components.SearchBar
import com.ioristudios.anydoc.ui.components.SelectionTopAppBar
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    initialFilter: String = "All",
    onNavigate: (String) -> Unit,
    onOpenFile: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    val filters = listOf("All", "PDF", "Word", "Excel", "PPT", "TXT", "Code")
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
                    totalCount = DummySearchFiles.size,
                    onSelectAllChange = { selectAll ->
                        selectedItemIds = if (selectAll) {
                            DummySearchFiles.map { it.id }.toSet()
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
                        text = "Matches Found (${DummySearchFiles.size})",
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
                count = DummySearchFiles.size,
                key = { index -> DummySearchFiles[index].id }
            ) { index ->
                val file = DummySearchFiles[index]
                Column(modifier = Modifier.padding(horizontal = spacing.screenPadding)) {
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
}
