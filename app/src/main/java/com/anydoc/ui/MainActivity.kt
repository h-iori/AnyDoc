package com.anydoc.ui

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.graphics.toArgb
import com.anydoc.navigation.AppNavHost
import com.anydoc.ui.theme.AnyDocTheme
import com.anydoc.ui.theme.SurfaceDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(SurfaceDark.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(SurfaceDark.toArgb())
        )
        super.onCreate(savedInstanceState)

        setContent {
            AnyDocTheme {
                AppNavHost()
            }
        }
    }
}
