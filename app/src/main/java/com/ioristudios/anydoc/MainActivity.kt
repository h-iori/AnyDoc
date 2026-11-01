package com.ioristudios.anydoc

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.ioristudios.anydoc.ui.navigation.AppNavigation
import com.ioristudios.anydoc.ui.theme.AnyDocTheme

class MainActivity : ComponentActivity() {

    private val incomingPathState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        incomingPathState.value = extractIncomingUri(intent)
        setContent {
            AnyDocTheme {
                AppNavigation(initialFilePath = incomingPathState.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val path = extractIncomingUri(intent)
        if (!path.isNullOrBlank()) {
            incomingPathState.value = path
        }
    }

    private fun extractIncomingUri(intent: Intent?): String? {
        if (intent == null) return null
        val uri: Uri? = intent.data
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
        return uri?.toString()
    }
}
