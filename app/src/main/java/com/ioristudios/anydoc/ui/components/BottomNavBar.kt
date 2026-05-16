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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import com.ioristudios.anydoc.ui.theme.AppMotion

private val NavSurface = Color(0xFF12121A)
private val NavActive = Color(0xFFD455FF)
private val NavInactive = Color(0xFF4A4A5E)
private val NavLabelActive = Color(0xFFEEEEFF)
private val NavIndicator = Color(0xFFBF00FF)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier,
        containerColor = NavSurface.copy(alpha = 0.96f),
        tonalElevation = 0.dp
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
        targetValue = if (isSelected) NavActive else NavInactive,
        animationSpec = tween(AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "navColor"
    )
    val labelColor = animateColorAsState(
        targetValue = if (isSelected) NavLabelActive else NavInactive,
        animationSpec = tween(AppMotion.Normal, easing = AppMotion.StandardEasing),
        label = "navLabelColor"
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
        label = { Text(text = label, color = labelColor.value) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = NavIndicator.copy(alpha = 0.22f)
        )
    )
}
