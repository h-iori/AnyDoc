package com.ioristudios.anydoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ioristudios.anydoc.ui.navigation.AppNavigation
import com.ioristudios.anydoc.ui.theme.AnyDocTheme

import android.content.pm.ActivityInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        val initialFilePath = intent?.data?.toString()
        setContent {
            AnyDocTheme {
                AppNavigation(initialFilePath = initialFilePath)
            }
        }
    }
}
