package com.hunter.system.features

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hunter.system.core.theme.SoloLevelingSystemTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the Solo Leveling SYSTEM app.
 *
 * Edge-to-edge is enforced on Android 16.
 * Hosts the Compose navigation graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SoloLevelingSystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Navigation host will be set up in Phase 1 implementation
                    SystemNavHost()
                }
            }
        }
    }
}
