package com.skofqq.boxy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** A foreground colour with its soft background, as used by BFR badges and action pills. */
@Immutable
data class Tint(val fg: Color, val bg: Color)

object Tints {
    private fun pair(light: Color, dark: Color, isDark: Boolean): Tint {
        val fg = if (isDark) dark else light
        return Tint(fg, fg.copy(alpha = if (isDark) 0.18f else 0.13f))
    }

    val green @Composable get() = pair(Color(0xFF1F883D), Color(0xFF3FB950), Boxy.colors.isDark)
    val red @Composable get() = pair(Color(0xFFCF222E), Color(0xFFF85149), Boxy.colors.isDark)
    val amber @Composable get() = pair(Color(0xFFBF8700), Color(0xFFD29922), Boxy.colors.isDark)
    val blue @Composable get() = pair(Color(0xFF0969DA), Color(0xFF58A6FF), Boxy.colors.isDark)
    val teal @Composable get() = pair(Color(0xFF0E9F8E), Color(0xFF2FD1B9), Boxy.colors.isDark)
    val purple @Composable get() = pair(Color(0xFF8250DF), Color(0xFFBC8CFF), Boxy.colors.isDark)
    val orange @Composable get() = pair(Color(0xFFD1602A), Color(0xFFF0883E), Boxy.colors.isDark)
    val gray @Composable get() = pair(Color(0xFF57606A), Color(0xFFB4BCC8), Boxy.colors.isDark)
}
