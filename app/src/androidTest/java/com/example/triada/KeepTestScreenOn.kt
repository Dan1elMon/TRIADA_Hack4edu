package com.example.triada

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/** Long credential setup must not let the test window suspend; restore the previous flag. */
@Composable
internal fun KeepTestScreenOn() {
    val view=LocalView.current
    DisposableEffect(view) {
        val previous=view.keepScreenOn
        view.keepScreenOn=true
        onDispose{view.keepScreenOn=previous}
    }
}
