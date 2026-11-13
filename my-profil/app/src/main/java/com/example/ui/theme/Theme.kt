package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = DeepTeal,
    onPrimary = OnDeepTeal,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = TerracottaCoral,
    onSecondary = OnTerracottaCoral,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = OnCoralContainer,
    tertiary = WarmAmber,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = AlabasterCanvas,
    onBackground = SlateInk,
    surface = CreamSurface,
    onSurface = SlateInk,
    surfaceVariant = WarmSandVariant,
    onSurfaceVariant = MutedStone,
    outline = WarmOutline,
    outlineVariant = WarmOutline.copy(alpha = 0.6f),
    error = ErrorLight,
)

private val DarkColors = darkColorScheme(
    primary = MintTeal,
    onPrimary = OnMintTeal,
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = OnDarkTealContainer,
    secondary = ApricotCoral,
    onSecondary = OnApricotCoral,
    secondaryContainer = DarkCoralContainer,
    onSecondaryContainer = OnDarkCoralContainer,
    tertiary = WarmAmber,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = ObsidianCanvas,
    onBackground = CrispIvory,
    surface = CharcoalSurface,
    onSurface = CrispIvory,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = SilverStone,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.7f),
    error = ErrorDark,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MyProfilTheme(
    darkThemeMode: String = "system",
    largeText: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (darkThemeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val baseScheme = if (isDark) DarkColors else LightColors
    val colorScheme = if (highContrast) {
        if (isDark) {
            baseScheme.copy(
                background = Color(0xFF000000),
                surface = Color(0xFF0E1214),
                onSurface = Color(0xFFFFFFFF),
                onBackground = Color(0xFFFFFFFF),
                outline = Color(0xFF9EB0BA),
            )
        } else {
            baseScheme.copy(
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF000000),
                onBackground = Color(0xFF000000),
                onSurfaceVariant = Color(0xFF22282C),
                outline = Color(0xFF454E54),
            )
        }
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(largeText = largeText),
        shapes = AppShapes,
        content = content,
    )
}
