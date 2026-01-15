package util

import androidx.compose.ui.graphics.Color

fun Double?.color(
    upColor: Color = Color(0xFFd81e06),
    downColor: Color = Color(0xFF1aad19),
    flatColor: Color = Color.White,
    epsilon: Double = 1e-6
): Color {
    return when {
        this == null -> flatColor
        this > epsilon -> upColor
        this < -epsilon -> downColor
        else -> flatColor
    }
}