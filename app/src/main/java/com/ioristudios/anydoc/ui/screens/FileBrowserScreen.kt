package com.ioristudios.anydoc.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioristudios.anydoc.model.BrowseItem
import com.ioristudios.anydoc.ui.components.BottomNavBar
import com.ioristudios.anydoc.ui.components.FileTypeIconRegistry
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.rememberAppSizes
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing
import com.ioristudios.anydoc.viewmodel.FileBrowserState
import com.ioristudios.anydoc.viewmodel.FileBrowserViewModel
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay

@Composable
fun FileBrowserScreen(
    onNavigate: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    viewModel: FileBrowserViewModel = viewModel()
) {
    val spacing = rememberAppSpacing()
    val uiState by viewModel.uiState.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val breadcrumbs = viewModel.getBreadcrumbs()
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    // Handle system back button
    BackHandler(enabled = !viewModel.isAtRoot()) {
        viewModel.navigateUp()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FileBrowserTopBar(
                breadcrumbs = breadcrumbs,
                isAtRoot = viewModel.isAtRoot(),
                onNavigateUp = { viewModel.navigateUp() },
                onBreadcrumbClick = { path -> viewModel.navigateInto(path) }
            )
        },
        bottomBar = { BottomNavBar("files", onNavigate) }
    ) { paddingValues ->
        when (uiState) {
            is FileBrowserState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Brand)
                }
            }

            is FileBrowserState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (uiState as FileBrowserState.Error).message,
                        color = AppColors.Danger,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            is FileBrowserState.Success -> {
                val items = (uiState as FileBrowserState.Success).items
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .alpha(0.4f),
                                tint = AppColors.BorderSubtle
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = "No supported files in this folder",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(
                            start = spacing.screenPadding,
                            end = spacing.screenPadding,
                            top = spacing.itemGap,
                            bottom = spacing.sectionGap
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> item.path }
                        ) { index, item ->
                            BrowseListItem(
                                item = item,
                                index = index,
                                onClick = {
                                    haptics.performHapticFeedback()
                                    if (item.isDirectory) {
                                        viewModel.navigateInto(item.path)
                                    } else {
                                        onOpenFile(item.name)
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

@Composable
private fun FileBrowserTopBar(
    breadcrumbs: List<Pair<String, String>>,
    isAtRoot: Boolean,
    onNavigateUp: () -> Unit,
    onBreadcrumbClick: (String) -> Unit
) {
    val spacing = rememberAppSpacing()
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBase.copy(alpha = 0.96f))
            .statusBarsPadding()
            .padding(horizontal = spacing.screenPadding, vertical = spacing.itemGap)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isAtRoot) {
                IconButton(onClick = {
                    haptics.performHapticFeedback()
                    onNavigateUp()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go up",
                        tint = AppColors.BrandStrong
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = AppColors.BrandStrong,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "File Manager",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        // Breadcrumb row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = if (isAtRoot) 12.dp else 0.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            breadcrumbs.forEachIndexed { index, (label, path) ->
                val isLast = index == breadcrumbs.lastIndex
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLast) AppColors.BrandStrong else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (!isLast) Modifier.clickable {
                                haptics.performHapticFeedback()
                                onBreadcrumbClick(path)
                            } else Modifier
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                if (!isLast) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseListItem(
    item: BrowseItem,
    index: Int,
    onClick: () -> Unit
) {
    val spacing = rememberAppSpacing()
    val sizes = rememberAppSizes()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (index < 15) {
            delay((index * 15L).coerceAtMost(150L))
        }
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(AppMotion.Normal),
        label = "alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(AppMotion.Normal),
        label = "translationY"
    )

    val visual = if (item.isDirectory) {
        FileTypeIconRegistry.resolveFileVisual("folder")
    } else {
        FileTypeIconRegistry.resolveFileVisual(item.name)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = sizes.fileRowMinHeight)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
            }
            .background(AppColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, visual.borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = spacing.cardPadding, vertical = spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = visual.iconRes),
            contentDescription = visual.label,
            modifier = Modifier.size(sizes.fileIconContainer),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.size(spacing.itemGap))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = visual.accentColor.copy(alpha = 0.98f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.size} • ${item.lastModified}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (item.isDirectory) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open folder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
