package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold),
)

fun buildAppTypography(largeText: Boolean = false): Typography {
    val scale = if (largeText) 1.16f else 1.0f
    return Typography(
        displaySmall = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (30f * scale).sp,
            lineHeight = (38f * scale).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22f * scale).sp,
            lineHeight = (30f * scale).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (19f * scale).sp,
            lineHeight = (26f * scale).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16f * scale).sp,
            lineHeight = (22f * scale).sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15.5f * scale).sp,
            lineHeight = (23f * scale).sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp,
        ),
        labelLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp,
        ),
        labelMedium = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * scale).sp,
            lineHeight = (16f * scale).sp,
        ),
        labelSmall = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * scale).sp,
            lineHeight = (15f * scale).sp,
        ),
    )
}
