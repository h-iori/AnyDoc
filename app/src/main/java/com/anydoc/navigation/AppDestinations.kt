package com.anydoc.navigation

sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object GlobalSearch : AppDestination("global-search")
    data object About : AppDestination("about")
    data object FileTypeDetail : AppDestination("file-type/{fileType}") {
        const val ARG_FILE_TYPE = "fileType"

        fun createRoute(fileType: String): String = "file-type/$fileType"
    }

    data object FileViewer : AppDestination("file-viewer/{fileName}") {
        const val ARG_FILE_NAME = "fileName"

        fun createRoute(fileName: String): String = "file-viewer/$fileName"
    }
}

object AppDestinations {
    val Home = AppDestination.Home
    val GlobalSearch = AppDestination.GlobalSearch
    val About = AppDestination.About
    val FileTypeDetail = AppDestination.FileTypeDetail
    val FileViewer = AppDestination.FileViewer
}
