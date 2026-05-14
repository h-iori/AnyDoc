package com.anydoc.navigation

import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anydoc.data.FileType
import com.anydoc.ui.components.AppDrawer
import com.anydoc.ui.components.BottomNavBar
import com.anydoc.ui.components.BottomNavDestination
import com.anydoc.ui.screens.AboutScreen
import com.anydoc.ui.screens.FileTypeDetailScreen
import com.anydoc.ui.screens.FileViewerScreen
import com.anydoc.ui.screens.GlobalSearchScreen
import com.anydoc.ui.screens.HomeScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val hideBottomBar = currentRoute.startsWith("file-viewer/")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AppDrawer(
                    onHomeClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(AppDestination.Home.route)
                    },
                    onSearchClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(AppDestination.GlobalSearch.route)
                    },
                    onAboutClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(AppDestination.About.route)
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                if (!hideBottomBar) {
                    BottomNavBar(
                        selectedDestination = if (currentRoute.startsWith(AppDestination.GlobalSearch.route)) {
                            BottomNavDestination.Search
                        } else {
                            BottomNavDestination.Home
                        },
                        onHomeClick = { navController.navigate(AppDestination.Home.route) },
                        onSearchClick = { navController.navigate(AppDestination.GlobalSearch.route) },
                        onOpenFileClick = {
                            Toast.makeText(context, "File picker would open here", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = AppDestination.Home.route,
                    enterTransition = {
                        slideInHorizontally { -it / 4 } + fadeIn()
                    },
                    exitTransition = {
                        slideOutHorizontally { it / 6 } + fadeOut()
                    }
                ) {
                    HomeScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSearchClick = { navController.navigate(AppDestination.GlobalSearch.route) },
                        onTypeClick = { fileType ->
                            navController.navigate(AppDestination.FileTypeDetail.createRoute(fileType.name))
                        },
                        onFileClick = { file ->
                            navController.navigate(AppDestination.FileViewer.createRoute(file.name))
                        }
                    )
                }

                composable(
                    route = AppDestination.FileTypeDetail.route,
                    arguments = listOf(navArgument(AppDestination.FileTypeDetail.ARG_FILE_TYPE) { type = NavType.StringType }),
                    enterTransition = {
                        slideInHorizontally { it / 3 } + fadeIn()
                    },
                    exitTransition = {
                        slideOutHorizontally { -it / 4 } + fadeOut()
                    },
                    popEnterTransition = {
                        slideInHorizontally { -it / 4 } + fadeIn()
                    },
                    popExitTransition = {
                        slideOutHorizontally { it / 3 } + fadeOut()
                    }
                ) { backStack ->
                    val arg = backStack.arguments?.getString(AppDestination.FileTypeDetail.ARG_FILE_TYPE).orEmpty()
                    val type = FileType.entries.find { it.name == arg } ?: FileType.ALL
                    FileTypeDetailScreen(
                        fileType = type,
                        onBackClick = { navController.popBackStack() },
                        onFileClick = { file -> navController.navigate(AppDestination.FileViewer.createRoute(file.name)) }
                    )
                }

                composable(
                    route = AppDestination.GlobalSearch.route,
                    enterTransition = { slideInVertically { it / 3 } + fadeIn() },
                    exitTransition = { slideOutVertically { -it / 4 } + fadeOut() }
                ) {
                    GlobalSearchScreen(
                        onFileClick = { file -> navController.navigate(AppDestination.FileViewer.createRoute(file.name)) }
                    )
                }

                composable(
                    route = AppDestination.FileViewer.route,
                    arguments = listOf(navArgument(AppDestination.FileViewer.ARG_FILE_NAME) { type = NavType.StringType }),
                    enterTransition = { scaleIn(initialScale = 0.92f) + fadeIn() },
                    exitTransition = { scaleOut(targetScale = 1.06f) + fadeOut() },
                    popEnterTransition = { scaleIn(initialScale = 1.06f) + fadeIn() },
                    popExitTransition = { scaleOut(targetScale = 0.92f) + fadeOut() }
                ) { backStack ->
                    val fileName = backStack.arguments?.getString(AppDestination.FileViewer.ARG_FILE_NAME).orEmpty()
                    FileViewerScreen(fileName = fileName)
                }

                composable(route = AppDestination.About.route) {
                    AboutScreen(onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}
