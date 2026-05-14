package com.anydoc.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class AnyDocHaptics internal constructor(
    private val trigger: (HapticFeedbackType) -> Unit
) {
    fun confirm() = trigger(HapticFeedbackType.LongPress)
    fun navigate() = trigger(HapticFeedbackType.TextHandleMove)
    fun reject() = trigger(HapticFeedbackType.LongPress)
}

@Composable
fun rememberAnyDocHaptics(): AnyDocHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { AnyDocHaptics(haptic::performHapticFeedback) }
}

@Composable
fun rememberHapticFeedback(
    type: HapticFeedbackType = HapticFeedbackType.LongPress
): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic, type) { { haptic.performHapticFeedback(type) } }
}
