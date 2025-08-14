package com.ml.lansonesscan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    secondary = BrandYellow,
    tertiary = DarkGreenVariant,
    background = AlmostBlack,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = BrandBlack,
    onSecondary = BrandBlack,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    primaryContainer = DarkGreen,
    secondaryContainer = YellowVariant,
    onPrimaryContainer = BrandBlack,
    onSecondaryContainer = BrandBlack,
    error = DiseaseRed,
    onError = Color.White,
    outline = BrandGreen.copy(alpha = 0.5f),
    outlineVariant = BrandYellow.copy(alpha = 0.3f)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    secondary = BrandYellow,
    tertiary = LightGreenVariant,
    background = Color.White,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = BrandBlack,
    onTertiary = Color.White,
    onBackground = BrandBlack,
    onSurface = BrandBlack,
    onSurfaceVariant = BrandBlack.copy(alpha = 0.7f),
    primaryContainer = LightGreen,
    secondaryContainer = LightYellow,
    onPrimaryContainer = BrandBlack,
    onSecondaryContainer = BrandBlack,
    error = DiseaseRed,
    onError = Color.White,
    outline = BrandGreen.copy(alpha = 0.7f),
    outlineVariant = BrandYellow.copy(alpha = 0.5f)
)

@Composable
fun LansonesScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}