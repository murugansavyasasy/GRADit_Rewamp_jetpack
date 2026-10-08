package com.vsca.vsnapvoicecollege.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single source of truth for text / label sizes across the app.
 *
 * Use these tokens instead of hardcoding `*.sp` in screens and components so the
 * type scale can be tuned (or scaled for accessibility) in one place.
 */
object AppTextSize {
    val H1 = 30.sp          // largest screen title (e.g. "Welcome back")
    val H2 = 28.sp          // standard screen title
    val H3 = 26.sp          // onboarding title
    val H4 = 22.sp          // bottom-sheet title
    val H5 = 20.sp          // section title / OTP digit
    val H6 = 18.sp          // small title

    val BodyLarge = 16.sp   // buttons, list-row titles
    val Body = 15.sp        // subtitles / descriptions
    val Label = 14.sp       // field labels, links, body text
    val Caption = 13.sp     // captions, chips, helper text
    val CaptionSmall = 12.sp // finest print (e.g. "Powered by")
}

/**
 * Single source of truth for shared component dimensions (button sizing, etc.)
 * so every button has the same height and corner radius across the app.
 */
object AppDimens {
    val ButtonHeight = 56.dp
    val ButtonCornerRadius = 16.dp
}
