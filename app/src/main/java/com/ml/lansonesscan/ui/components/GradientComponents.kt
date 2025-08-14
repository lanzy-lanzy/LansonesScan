package com.ml.lansonesscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ml.lansonesscan.ui.theme.*

/**
 * Modern minimal card component with subtle gradient background
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush = cardGradient(),
    shape: Shape = RoundedCornerShape(12.dp),
    border: Brush? = null,
    borderWidth: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .background(gradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = BrandGreen.copy(alpha = 0.1f)),
                onClick = onClick
            )
    } else {
        modifier
            .clip(shape)
            .background(gradient)
    }

    val finalModifier = if (border != null) {
        cardModifier.border(borderWidth, border, shape)
    } else {
        cardModifier
    }

    // Use a simple Box instead of Card to remove default Material shadows
    Box(
        modifier = finalModifier
    ) {
        Column(content = content)
    }
}

/**
 * Enhanced Button component with gradient background
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = buttonGradient(),
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .background(if (enabled) gradient else Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ), shape)
            .clip(shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Enhanced primary action button with gradient
 */
@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false
) {
    GradientButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.height(56.dp),
        gradient = buttonGradient()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isLoading) "Loading..." else text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

/**
 * Enhanced secondary action button with outline gradient
 */
@Composable
fun SecondaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val borderGradient = Brush.linearGradient(
        colors = listOf(BrandGreen, BrandYellow)
    )
    
    GradientButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(48.dp)
            .border(2.dp, borderGradient, RoundedCornerShape(12.dp)),
        gradient = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = BrandGreen
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = BrandGreen
        )
    }
}

/**
 * Enhanced statistics card with gradient background
 */
@Composable
fun StatisticsGradientCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    gradient: Brush = cardGradient()
) {
    GradientCard(
        modifier = modifier,
        gradient = gradient
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BrandGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = BrandGreen
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Minimal action card with subtle gradient background
 */
@Composable
fun ActionGradientCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = secondaryGradient()
) {
    GradientCard(
        modifier = modifier,
        gradient = gradient,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BrandGreen,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
