package com.hunter.system.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

fun carryoverGradient(daysCarriedOver: Int): Brush {
    if (daysCarriedOver <= 0) return Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    val intensity = (daysCarriedOver.coerceIn(1, 7) / 7f)
    val warmColor = lerp(Color(0xFFFF9800), Color(0xFFFF1744), intensity)
    return Brush.horizontalGradient(
        listOf(warmColor.copy(alpha = 0.08f + intensity * 0.35f), Color.Transparent)
    )
}

fun carryoverColor(daysCarriedOver: Int): Color {
    if (daysCarriedOver <= 0) return Color.Transparent
    val intensity = (daysCarriedOver.coerceIn(1, 7) / 7f)
    return lerp(Color(0xFFFF9800), Color(0xFFFF1744), intensity)
}
