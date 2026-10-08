package com.vsca.vsnapvoicecollege.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Sets the status- and navigation-bar icon contrast for the current screen.
 *
 * The system bars are transparent app-wide (see MainActivity), so the screen's
 * own background shows through them; this just keeps the icons legible.
 *
 * @param darkIcons `true` for dark icons on a light background, `false` for light
 *   icons on a dark/brand background (e.g. the splash).
 */
@Composable
fun SystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(darkIcons) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
        onDispose { }
    }
}
