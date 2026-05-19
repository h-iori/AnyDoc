package com.ioristudios.anydoc.ui.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ioristudios.anydoc.ui.components.AppSidebar
import com.ioristudios.anydoc.ui.screens.AboutScreen
import com.ioristudios.anydoc.ui.screens.FileBrowserScreen
import com.ioristudios.anydoc.ui.screens.FileViewerScreen
import com.ioristudios.anydoc.ui.screens.HomeScreen
import com.ioristudios.anydoc.ui.screens.PermissionScreen
import com.ioristudios.anydoc.ui.screens.SearchScreen
import com.ioristudios.anydoc.ui.theme.AppMotion
import com.ioristudios.anydoc.util.PermissionManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

// ── Transition helpers ──
private const val NAV_DURATION = 200

private fun navEnter(): EnterTransition =
    fadeIn(tween(NAV_DURATION)) +
        slideInHorizontally(
            initialOffsetX = { it / 5 },
            animationSpec = tween(NAV_DURATION)
        )

private fun navExit(): ExitTransition =
    fadeOut(tween(NAV_DURATION)) +
        slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = tween(NAV_DURATION)
        )

private fun navPopEnter(): EnterTransition =
    fadeIn(tween(NAV_DURATION)) +
        slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = tween(NAV_DURATION)
        )

private fun navPopExit(): ExitTransition =
    fadeOut(tween(NAV_DURATION)) +
        slideOutHorizontally(
            targetOffsetX = { it / 5 },
            animationSpec = tween(NAV_DURATION)
        )

@Composable
fun AppNavigation(initialFilePath: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var isSidebarVisible by remember { mutableStateOf(false) }

    val navigate = remember(navController) {
        { route: String ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val hasPermission = PermissionManager.hasStoragePermission(context)
    val startDest = if (hasPermission) "home" else "permission"

    LaunchedEffect(hasPermission, initialFilePath) {
        if (hasPermission && !initialFilePath.isNullOrBlank()) {
            navController.navigate("fileViewer?path=${Uri.encode(initialFilePath)}&isExternal=true")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050510),
                        Color(0xFF0A0A0F),
                        Color(0xFF0D0D1A)
                    )
                )
            )
    ) {
        NavHost(
            navController = navController,
            startDestination = startDest,
            enterTransition = { navEnter() },
            exitTransition = { navExit() },
            popEnterTransition = { navPopEnter() },
            popExitTransition = { navPopExit() }
        ) {
            composable("permission") {
                PermissionScreen(onPermissionGranted = {
                    navController.navigate("home") {
                        popUpTo("permission") { inclusive = true }
                    }
                })
            }
            composable("home") {
                HomeScreen(
                    onNavigate = navigate,
                    onSearchWithFilter = { filter -> navigate("search?filter=$filter") },
                    onOpenFile = { filePath -> navigate("fileViewer?path=${Uri.encode(filePath)}") },
                    onMenuClick = { isSidebarVisible = true }
                )
            }
            composable(
                route = "search?filter={filter}",
                arguments = listOf(navArgument("filter") { 
                    type = NavType.StringType
                    defaultValue = "All"
                    nullable = true
                })
            ) { backStackEntry ->
                val filter = backStackEntry.arguments?.getString("filter") ?: "All"
                SearchScreen(
                    initialFilter = filter,
                    onNavigate = navigate,
                    onOpenFile = { filePath -> navigate("fileViewer?path=${Uri.encode(filePath)}") }
                )
            }
            composable("files") {
                FileBrowserScreen(
                    onNavigate = navigate,
                    onOpenFile = { filePath -> navigate("fileViewer?path=${Uri.encode(filePath)}") }
                )
            }
            composable(
                route = "fileViewer?path={path}&isExternal={isExternal}",
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("isExternal") { 
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val filePath = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
                val isExternal = backStackEntry.arguments?.getBoolean("isExternal") ?: false
                FileViewerScreen(
                    filePath = filePath,
                    isExternal = isExternal,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }

        // Custom Sidebar Overlay
        AppSidebar(
            isVisible = isSidebarVisible,
            onDismiss = { isSidebarVisible = false },
            onAboutClick = {
                isSidebarVisible = false
                navigate("about")
            }
        )

    }
}
