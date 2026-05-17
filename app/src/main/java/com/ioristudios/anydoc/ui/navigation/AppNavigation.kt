package com.ioristudios.anydoc.ui.navigation

import android.content.Intent
import android.net.Uri
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
import com.ioristudios.anydoc.ui.screens.FileViewerScreen
import com.ioristudios.anydoc.ui.screens.HomeScreen
import com.ioristudios.anydoc.ui.screens.PermissionScreen
import com.ioristudios.anydoc.ui.screens.SearchScreen
import com.ioristudios.anydoc.util.PermissionManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var isSidebarVisible by remember { mutableStateOf(false) }

    val navigate = androidx.compose.runtime.remember(navController) {
        { route: String ->
            if (route == "files") {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                context.startActivity(Intent.createChooser(intent, "Open File"))
            } else {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val startDest = if (PermissionManager.hasStoragePermission(context)) "home" else "permission"

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
        NavHost(navController = navController, startDestination = startDest) {
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
                    onOpenFile = { fileName -> navigate("fileViewer/${Uri.encode(fileName)}") },
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
                    onOpenFile = { fileName -> navigate("fileViewer/${Uri.encode(fileName)}") }
                )
            }
            composable(
                route = "fileViewer/{fileName}",
                arguments = listOf(navArgument("fileName") { type = NavType.StringType })
            ) { backStackEntry ->
                val fileName = Uri.decode(backStackEntry.arguments?.getString("fileName") ?: "Document")
                FileViewerScreen(
                    fileName = fileName,
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



