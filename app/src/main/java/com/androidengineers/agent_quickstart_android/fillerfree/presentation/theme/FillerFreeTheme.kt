package com.androidengineers.agent_quickstart_android.fillerfree.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design language: this app's entire personality is "cut the fluff."
 * The UI should feel like a stopwatch / scoreboard, not a friendly chat
 * assistant — high contrast, monospaced numerals for counters, no soft
 * bubble decoration. Silence (no interruption) reads as calm dark space;
 * an interruption is a hard, brief flash of amber, not a cute animation.
 */
object FillerFreeColors {
    val background = Color(0xFF0E0F11)      // near-black, not pure black
    val surface = Color(0xFF17181B)
    val surfaceRaised = Color(0xFF1F2124)
    val hairline = Color(0xFF2B2D31)

    val textPrimary = Color(0xFFF2F1EE)      // warm off-white, not pure white
    val textSecondary = Color(0xFF9A9CA3)
    val textMuted = Color(0xFF6B6D73)

    val signalAmber = Color(0xFFE0A63A)      // interruption flash — sharp, not alarming red
    val signalGreen = Color(0xFF5FBF8A)       // clean-run positive state
    val signalRed = Color(0xFFD9564A)         // used sparingly: repetition warning only
}

object FillerFreeType {
    // Utility/counter face: tabular, mechanical feel for live-updating numbers.
    val counterNumber = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = 0.sp,
    )

    val counterLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
    )

    val screenTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.3).sp,
    )

    val body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    val interruptionLine = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.2).sp,
    )
}
