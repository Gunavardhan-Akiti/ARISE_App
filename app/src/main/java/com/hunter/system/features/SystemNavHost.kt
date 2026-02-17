package com.hunter.system.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Navigation routes for the Solo Leveling SYSTEM app.
 * Supports predictive back gesture (Android 16 requirement).
 */
object SystemRoutes {
    const val ALARM = "alarm"
    const val QUEST = "quest"
    const val SETTINGS = "settings"
    const val STATS = "stats"
}

@Composable
fun SystemNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = SystemRoutes.ALARM
    ) {
        composable(SystemRoutes.ALARM) {
            // AlarmScreen will be implemented in Phase 1
            PlaceholderScreen(title = "ALARM SYSTEM", subtitle = "Wake-up scheduling — Phase 1")
        }
        composable(SystemRoutes.QUEST) {
            // QuestScreen will be implemented in Phase 1
            PlaceholderScreen(title = "DAILY QUEST", subtitle = "Step tracking quest — Phase 1")
        }
        composable(SystemRoutes.SETTINGS) {
            // SettingsScreen will be implemented in Phase 1
            PlaceholderScreen(title = "SYSTEM SETTINGS", subtitle = "Configuration — Phase 1")
        }
        composable(SystemRoutes.STATS) {
            // StatsScreen will be implemented in Phase 3
            PlaceholderScreen(title = "HUNTER STATS", subtitle = "Quest history & ranks — Phase 3")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}
