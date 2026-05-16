package com.ioristudios.anydoc.ui.utils

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberAppHaptics(): AppHaptics {
    val view = LocalView.current
    return remember(view) { AppHaptics(view) }
}

class AppHaptics(private val view: View) {
    fun performHapticFeedback() {
        // As requested, haptic feedback is constant throughout the app.
        // CONTEXT_CLICK provides a premium, subtle, and consistent tactile response.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
