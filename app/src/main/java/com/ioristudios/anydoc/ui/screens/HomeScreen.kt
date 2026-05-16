package com.ioristudios.anydoc.ui.screens

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
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ioristudios.anydoc.model.DummyRecentFiles
import com.ioristudios.anydoc.ui.components.BottomNavBar
import com.ioristudios.anydoc.ui.components.FileListItem
import com.ioristudios.anydoc.ui.components.FileTypeCard
import com.ioristudios.anydoc.ui.components.TopAppBar
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onSearchWithFilter: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val spacing = rememberAppSpacing()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(onMenuClick = onMenuClick) },
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
            item { FileTypeGrid(onSearchWithFilter) }
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
        Triple("All Files", "folder", AppColors.BrandStrong),
        Triple("PDF", "sample.pdf", AppColors.Danger),
        Triple("Word", "sample.docx", AppColors.Info),
        Triple("Excel", "sample.xlsx", AppColors.Success),
        Triple("PPT", "sample.pptx", AppColors.Warning),
        Triple("TXT", "sample.txt", AppColors.BorderStrong),
        Triple("Code", "sample.kt", AppColors.Brand)
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
            accentColor = allFiles.third,
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
                    accentColor = entry.third,
                    modifier = Modifier.fillMaxWidth(0.485f),
                    onClick = { onTypeClick(entry.first) }
                )
            }
        }
    }
}
