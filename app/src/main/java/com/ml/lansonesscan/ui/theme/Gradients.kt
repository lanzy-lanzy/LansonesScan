package com.ml.lansonesscan.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient utilities for the Lansones Scan app
 * Provides consistent gradient backgrounds using the black, green, yellow theme
 */

/**
 * Primary gradient for main UI elements
 */
@Composable
fun primaryGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.linearGradient(
            colors = listOf(
                BrandBlack,
                DarkGreen.copy(alpha = 0.8f),
                BrandGreen.copy(alpha = 0.6f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                LightGreen.copy(alpha = 0.3f),
                BrandGreen.copy(alpha = 0.1f)
            )
        )
    }
}

/**
 * Secondary gradient for accent elements
 */
@Composable
fun secondaryGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.linearGradient(
            colors = listOf(
                BrandYellow.copy(alpha = 0.2f),
                BrandGreen.copy(alpha = 0.4f),
                BrandBlack.copy(alpha = 0.8f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                LightYellow.copy(alpha = 0.3f),
                LightGreen.copy(alpha = 0.2f),
                Color.White
            )
        )
    }
}

/**
 * Subtle card gradient for modern minimal design
 */
@Composable
fun cardGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.linearGradient(
            colors = listOf(
                DarkSurface.copy(alpha = 0.8f),
                DarkSurface.copy(alpha = 0.6f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                Color.White.copy(alpha = 0.7f),
                BrandGreen.copy(alpha = 0.02f)
            )
        )
    }
}

/**
 * Button gradient for primary actions
 */
@Composable
fun buttonGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.horizontalGradient(
            colors = listOf(
                BrandGreen,
                DarkGreen,
                BrandYellow.copy(alpha = 0.8f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                BrandGreen,
                LightGreenVariant,
                BrandYellow.copy(alpha = 0.7f)
            )
        )
    }
}

/**
 * Navigation bar gradient
 */
@Composable
fun navigationGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                DarkSurface.copy(alpha = 0.95f),
                BrandBlack.copy(alpha = 0.98f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                LightSurfaceVariant.copy(alpha = 0.8f)
            )
        )
    }
}

/**
 * Header/Toolbar gradient
 */
@Composable
fun headerGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                BrandGreen.copy(alpha = 0.3f),
                DarkSurface,
                BrandBlack
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                BrandGreen.copy(alpha = 0.1f),
                Color.White,
                LightSurfaceVariant
            )
        )
    }
}

/**
 * Success gradient for positive states
 */
@Composable
fun successGradient(): Brush {
    return Brush.horizontalGradient(
        colors = listOf(
            HealthyGreen,
            BrandGreen,
            LightGreen
        )
    )
}

/**
 * Warning gradient for caution states
 */
@Composable
fun warningGradient(): Brush {
    return Brush.horizontalGradient(
        colors = listOf(
            CautionAmber,
            BrandYellow,
            YellowVariant
        )
    )
}

/**
 * Error gradient for error states
 */
@Composable
fun errorGradient(): Brush {
    return Brush.horizontalGradient(
        colors = listOf(
            DiseaseRed,
            Color(0xFFE57373),
            Color(0xFFFFCDD2)
        )
    )
}

/**
 * Modifier extension for applying primary gradient background
 */
@Composable
fun Modifier.primaryGradientBackground(): Modifier {
    return this.background(primaryGradient())
}

/**
 * Modifier extension for applying card gradient background
 */
@Composable
fun Modifier.cardGradientBackground(): Modifier {
    return this.background(cardGradient())
}

/**
 * Modifier extension for applying button gradient background
 */
@Composable
fun Modifier.buttonGradientBackground(): Modifier {
    return this.background(buttonGradient())
}
