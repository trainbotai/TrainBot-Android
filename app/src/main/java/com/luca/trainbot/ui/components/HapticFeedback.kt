package com.luca.trainbot.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/** Small helper to fire a haptic tap from a composable. */
@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    return { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
}

/** Stronger confirmation haptic — used for success events (train, achievement). */
@Composable
fun rememberHapticConfirm(): () -> Unit {
    val view = LocalView.current
    return { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }
}
