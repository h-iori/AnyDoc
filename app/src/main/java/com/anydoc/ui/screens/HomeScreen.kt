package com.anydoc.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anydoc.data.FileItem
import com.anydoc.data.FileType
import com.anydoc.data.FileTypeChip
import com.anydoc.data.MockData
import com.anydoc.ui.components.FileListItem
import com.anydoc.ui.theme.AnyDocTheme
import com.anydoc.ui.theme.CoreWhite
import com.anydoc.ui.theme.CoreWhiteDim
import com.anydoc.ui.theme.SurfaceDark
import com.anydoc.ui.theme.screenHorizontalPadding
import com.anydoc.ui.theme.screenVerticalPadding
import com.anydoc.ui.theme.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    typeChips: List<FileTypeChip> = MockData.typeChips,
    recentFiles: List<FileItem> = MockData.recentFiles,
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onTypeClick: (FileType) -> Unit = {},
    onFileClick: (FileItem) -> Unit = {}
) {
    val onTapHaptic = rememberHapticFeedback()
    val listState = rememberLazyListState()
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10 }
    }
    val appBarAlpha by animateFloatAsState(targetValue = if (collapsed) 0.92f else 0.65f, label = "appBarAlpha")
    val appBarElevation by animateDpAsState(targetValue = if (collapsed) 10.dp else 0.dp, label = "appBarElevation")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "AnyDoc",
                    style = MaterialTheme.typography.headlineSmall,
                    color = CoreWhite,
                    modifier = Modifier.alpha(appBarAlpha)
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    onTapHaptic()
                    onMenuClick()
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Open drawer",
                        tint = CoreWhiteDim
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    onTapHaptic()
                    onSearchClick()
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = CoreWhiteDim
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = appBarAlpha),
                titleContentColor = CoreWhite,
                navigationIconContentColor = CoreWhiteDim,
                actionIconContentColor = CoreWhiteDim
            ),
            modifier = Modifier
                .statusBarsPadding()
                .alpha(1f)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(appBarElevation),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {}

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = screenHorizontalPadding, vertical = screenVerticalPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Browse by type",
                        style = MaterialTheme.typography.titleLarge,
                        color = CoreWhite
                    )
                    val rows = typeChips.chunked(2)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { chip ->
                                TypeCard(
                                    chip = chip,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onTapHaptic()
                                        onTypeClick(chip.fileType)
                                    }
                                )
                            }
                            if (rowItems.size == 1) {
                                Row(modifier = Modifier.weight(1f)) {}
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleLarge,
                    color = CoreWhite,
                    modifier = Modifier.padding(horizontal = screenHorizontalPadding)
                )
            }

            items(recentFiles) { file ->
                FileListItem(
                    file = file,
                    modifier = Modifier.padding(horizontal = screenHorizontalPadding),
                    onClick = onFileClick
                )
            }
        }
    }
}

@Composable
private fun TypeCard(
    chip: FileTypeChip,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .widthIn(min = 0.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = chip.label,
                style = MaterialTheme.typography.titleMedium,
                color = CoreWhiteDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${chip.count} files",
                style = MaterialTheme.typography.bodyMedium,
                color = chip.accent
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun HomeScreenPreview() {
    AnyDocTheme {
        HomeScreen()
    }
}
