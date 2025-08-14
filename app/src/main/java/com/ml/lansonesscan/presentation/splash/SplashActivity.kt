package com.ml.lansonesscan.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.ml.lansonesscan.MainActivity
import com.ml.lansonesscan.R
import com.ml.lansonesscan.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    
    private var keepSplashScreen = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        }
        
        super.onCreate(savedInstanceState)
        
        setContent {
            LansonesScanTheme {
                SplashScreen()
            }
        }
        
        // Navigate to main activity after animation
        lifecycleScope.launch {
            delay(3000) // 3 seconds total duration
            keepSplashScreen = false
            navigateToMain()
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        // Add custom transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

@Composable
fun SplashScreen() {
    // Animation states
    var startAnimation by remember { mutableStateOf(false) }
    
    // Logo scale animation with bounce effect
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // Logo alpha animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = LinearOutSlowInEasing
        ),
        label = "logoAlpha"
    )
    
    // Text alpha animation (delayed)
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 500,
            easing = LinearOutSlowInEasing
        ),
        label = "textAlpha"
    )
    
    // Text slide up animation
    val textOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 50f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "textOffset"
    )
    
    // Start animation when composable is first composed
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BrandGreen.copy(alpha = 0.1f),
                        Color.White,
                        BrandYellow.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lansones fruit logo
            Image(
                painter = painterResource(id = R.drawable.ic_lansones_fruit),
                contentDescription = "LansonesScan Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App name
            Text(
                text = "LansonesScan",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha)
                    .offset(y = textOffset.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Smart Lansones Health Analysis",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha)
                    .offset(y = textOffset.dp)
            )
        }
        
        // Version info at bottom
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(textAlpha)
        )
    }
}
