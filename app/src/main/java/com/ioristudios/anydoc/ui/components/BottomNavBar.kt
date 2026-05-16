package com.ioristudios.anydoc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.AppMotion

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = AppColors.SurfaceBase.copy(alpha = 0.98f),
        tonalElevation = 4.dp
    ) {
        NavItem("Home", Icons.Default.Home, currentRoute == "home") { onNavigate("home") }
        NavItem("Search", Icons.Default.Search, currentRoute == "search") { onNavigate("search") }
        NavItem("Files", Icons.Default.FolderOpen, currentRoute == "files") { onNavigate("files") }
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = animateColorAsState(
        targetValue = if (isSelected) AppColors.BrandStrong else AppColors.BorderStrong,
        animationSpec = tween(AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "navColor"
    )

    val haptics = com.ioristudios.anydoc.ui.utils.rememberAppHaptics()

    NavigationBarItem(
        selected = isSelected,
        onClick = {
            haptics.performHapticFeedback()
            onClick()
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = selectedColor.value
            )
        },
        label = { Text(text = label, color = selectedColor.value) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = AppColors.Brand.copy(alpha = 0.16f)
        )
    )
}
