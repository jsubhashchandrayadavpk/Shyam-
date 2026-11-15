package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ShyamLightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = Color.White,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = OnSaffronContainer,
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = KumkumMaroon,
    onTertiary = Color.White,
    tertiaryContainer = KumkumContainer,
    onTertiaryContainer = KumkumMaroon,
    background = CreamBackground,
    onBackground = InkPrimary,
    surface = CreamSurface,
    onSurface = InkPrimary,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = OutlineWarm
)

private val ShyamDarkColorScheme = darkColorScheme(
    primary = TwilightSaffron,
    onPrimary = Color(0xFF421600),
    primaryContainer = SaffronDeep,
    onPrimaryContainer = Color(0xFFFFE5D1),
    secondary = TwilightGold,
    onSecondary = Color(0xFF3E2900),
    secondaryContainer = Color(0xFF5A3F0A),
    onSecondaryContainer = GoldContainer,
    tertiary = Color(0xFFFFB4A9),
    onTertiary = Color(0xFF5D130D),
    background = TwilightBackground,
    onBackground = TwilightCreamText,
    surface = TwilightSurface,
    onSurface = TwilightCreamText,
    surfaceVariant = TwilightCard,
    onSurfaceVariant = TwilightSubText,
    outline = Color(0xFF6B4B36)
)

val ShyamShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun ShyamTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ShyamDarkColorScheme else ShyamLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShyamTypography,
        shapes = ShyamShapes,
        content = content
    )
}
