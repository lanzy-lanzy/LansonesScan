package com.ml.lansonesscan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Brand Colors - Black, Green, Yellow Theme
val BrandBlack = Color(0xFF000000)
val BrandGreen = Color(0xFF4CAF50)
val BrandYellow = Color(0xFFFFEB3B)

// Enhanced Dark Theme Colors
val DarkGreen = Color(0xFF66BB6A)
val DarkGreenVariant = Color(0xFF388E3C)
val VibrantYellow = Color(0xFFFFEB3B)
val YellowVariant = Color(0xFFFFC107)
val AlmostBlack = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)

// Enhanced Light Theme Colors
val LightGreen = Color(0xFF81C784)
val LightGreenVariant = Color(0xFF4CAF50)
val LightYellow = Color(0xFFFFF176)
val LightYellowVariant = Color(0xFFFFEB3B)
val LightSurface = Color(0xFFFFFBFE)
val LightSurfaceVariant = Color(0xFFF5F5F5)

// Gradient Definitions
val GreenToYellowGradient = Brush.horizontalGradient(
    colors = listOf(BrandGreen, BrandYellow)
)

val BlackToGreenGradient = Brush.verticalGradient(
    colors = listOf(BrandBlack, DarkGreen)
)

val YellowToGreenGradient = Brush.horizontalGradient(
    colors = listOf(BrandYellow, BrandGreen)
)

val DarkGradient = Brush.verticalGradient(
    colors = listOf(AlmostBlack, DarkSurface)
)

val LightGradient = Brush.verticalGradient(
    colors = listOf(Color.White, LightSurfaceVariant)
)

// Card and Component Gradients
val PrimaryCardGradient = Brush.linearGradient(
    colors = listOf(BrandGreen.copy(alpha = 0.1f), BrandYellow.copy(alpha = 0.1f))
)

val SecondaryCardGradient = Brush.linearGradient(
    colors = listOf(BrandBlack.copy(alpha = 0.05f), BrandGreen.copy(alpha = 0.05f))
)

// Legacy colors (keeping for compatibility)
val Green80 = Color(0xFFA8D5A8)
val GreenGrey80 = Color(0xFFB8C8B8)
val Amber80 = Color(0xFFFFD54F)
val Green40 = Color(0xFF4CAF50)
val GreenGrey40 = Color(0xFF689F38)
val Amber40 = Color(0xFFFF8F00)

// Additional colors for disease detection
val HealthyGreen = Color(0xFF4CAF50)
val CautionAmber = Color(0xFFFF9800)
val DiseaseRed = Color(0xFFF44336)
val SurfaceVariant = Color(0xFFF5F5F5)
