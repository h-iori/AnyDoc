package com.ioristudios.anydoc.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.ioristudios.anydoc.ui.components.DocumentPage
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.ui.theme.neonGlow
import com.ioristudios.anydoc.ui.theme.rememberAppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    fileName: String = "Quarterly_Report.pdf",
    onBack: () -> Unit
) {
    val spacing = rememberAppSpacing()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()
    var pageVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { pageVisible = true }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(fileName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.performHapticFeedback()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { haptics.performHapticFeedback() }) { Icon(Icons.Default.Search, contentDescription = "Search in doc") }
                    IconButton(onClick = { haptics.performHapticFeedback() }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                    IconButton(onClick = { haptics.performHapticFeedback() }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = AppColors.SurfaceBase.copy(alpha = 0.98f),
                    scrolledContainerColor = AppColors.SurfaceElevated.copy(alpha = 0.98f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = AppColors.BrandStrong,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { haptics.performHapticFeedback() },
                containerColor = AppColors.BrandStrong,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.neonGlow(color = AppColors.Brand.copy(alpha = 0.4f), radius = spacing.itemGap)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Annotate")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
        ) {
            items(
                count = 3,
                key = { it }
            ) { index ->
                AnimatedVisibility(
                    visible = pageVisible,
                    enter = fadeIn(
                        tween(
                            durationMillis = AppMotion.Normal + (index * 40),
                            easing = AppMotion.StandardEasing
                        )
                    ) + slideInVertically(
                        initialOffsetY = { it / 6 },
                        animationSpec = tween(
                            durationMillis = AppMotion.Normal + (index * 40),
                            easing = AppMotion.DecelerateEasing
                        )
                    )
                ) {
                    DocumentPage(isLoading = index == 0)
                }
            }
        }
    }
}
