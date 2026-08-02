package com.instasave.app.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val hairline: Dp = 0.5.dp,
    val minTouchTarget: Dp = 48.dp
)

val LocalSpacing = androidx.compose.runtime.staticCompositionLocalOf { Spacing() }
