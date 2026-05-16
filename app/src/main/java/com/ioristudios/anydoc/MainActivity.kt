package com.ioristudios.anydoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ioristudios.anydoc.ui.navigation.AppNavigation
import com.ioristudios.anydoc.ui.theme.AnyDocTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnyDocTheme {
                AppNavigation()
            }
        }
    }
}
