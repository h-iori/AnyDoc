package com.ioristudios.anydoc.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ioristudios.anydoc.ui.screens.FileViewerScreen
import com.ioristudios.anydoc.ui.screens.HomeScreen
import com.ioristudios.anydoc.ui.screens.SearchScreen

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.ioristudios.anydoc.ui.screens.AboutScreen
import com.ioristudios.anydoc.ui.theme.AppColors
import com.ioristudios.anydoc.ui.theme.LogoTextStyle
import kotlinx.coroutines.launch


import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush



import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ioristudios.anydoc.ui.components.AppSidebar


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
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



