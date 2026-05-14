package com.anydoc.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anydoc.ui.theme.AnyDocTheme
import com.anydoc.ui.theme.rememberAnyDocHaptics

@Stable
enum class BottomNavDestination {
    Home,
    Search
}

@Composable
fun BottomNavBar(
    selectedDestination: BottomNavDestination,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAnyDocHaptics()
    val items = listOf(
        BottomNavItem(BottomNavDestination.Home, "Home", Icons.Outlined.Home),
        BottomNavItem(BottomNavDestination.Search, "Search", Icons.Outlined.Search)
    )

    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .height(76.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = item.destination == selectedDestination
            val iconScale = animateFloatAsState(
                targetValue = if (selected) 1.12f else 1f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
                label = "bottom-nav-icon-scale"
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    haptics.navigate()
                    if (item.destination == BottomNavDestination.Home) onHomeClick() else onSearchClick()
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.scale(iconScale.value)
                    )
                },
                label = {
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 3 }),
                        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 3 })
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                alwaysShowLabel = false,
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                    unselectedTextColor = MaterialTheme.colorScheme.outline,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        val openScale = animateFloatAsState(
            targetValue = 1f,
            label = "bottom-nav-open-scale"
        )

        NavigationBarItem(
            selected = false,
            onClick = {
                haptics.confirm()
                onOpenFileClick()
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Open File",
                    modifier = Modifier.scale(openScale.value)
                )
            },
            label = {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Text(
                        text = "Open File",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            alwaysShowLabel = true,
            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.secondary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

private data class BottomNavItem(
    val destination: BottomNavDestination,
    val label: String,
    val icon: ImageVector
)

@Preview(showBackground = true)
@Composable
private fun BottomNavBarPreview() {
    AnyDocTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                BottomNavBar(
                    selectedDestination = BottomNavDestination.Home,
                    onHomeClick = {},
                    onSearchClick = {},
                    onOpenFileClick = {}
                )
            }
        }
    }
}
