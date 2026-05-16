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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

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

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigate = navigate,
                onSearchWithFilter = { filter -> navigate("search?filter=$filter") },
                onOpenFile = { fileName -> navigate("fileViewer/${Uri.encode(fileName)}") }
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
    }
}
