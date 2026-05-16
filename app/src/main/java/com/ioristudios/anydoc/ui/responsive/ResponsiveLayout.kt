package com.ioristudios.anydoc.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class AppWindowClass {
    Compact,
    Medium,
    Expanded
}

@Composable
fun rememberAppWindowClass(): AppWindowClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> AppWindowClass.Expanded
        widthDp >= 600 -> AppWindowClass.Medium
        else -> AppWindowClass.Compact
    }
}
