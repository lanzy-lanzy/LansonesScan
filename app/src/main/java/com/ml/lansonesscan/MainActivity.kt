package com.ml.lansonesscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ml.lansonesscan.presentation.navigation.LansonesScanNavigation
import com.ml.lansonesscan.ui.theme.LansonesScanTheme

/**
 * Main activity for the Lansones Disease Scanner app
 * Implements Material Design 3 with dynamic colors and proper theme support
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LansonesScanTheme {
                LansonesScanNavigation()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    LansonesScanTheme {
        LansonesScanNavigation()
    }
}