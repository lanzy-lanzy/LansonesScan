package com.ml.lansonesscan.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.ml.lansonesscan.presentation.ViewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ml.lansonesscan.presentation.scandetail.ScanDetailViewModel
import com.ml.lansonesscan.presentation.analysis.AnalysisScreen
import com.ml.lansonesscan.presentation.analysis.AnalysisViewModel
import com.ml.lansonesscan.presentation.dashboard.DashboardScreen
import com.ml.lansonesscan.presentation.dashboard.DashboardViewModel
import com.ml.lansonesscan.presentation.history.HistoryScreen
import com.ml.lansonesscan.presentation.history.HistoryViewModel
import com.ml.lansonesscan.presentation.scandetail.ScanDetailScreen
import com.ml.lansonesscan.presentation.settings.SettingsScreen
import com.ml.lansonesscan.presentation.settings.SettingsViewModel
import androidx.compose.material3.CircularProgressIndicator

/**
 * Data class representing a bottom navigation item
 */
data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

/**
 * List of bottom navigation items
 */
val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Dashboard,
        icon = Icons.Default.Home,
        label = "Dashboard"
    ),
    BottomNavItem(
        screen = Screen.Analysis,
        icon = Icons.Default.Search,
        label = "Analysis"
    ),
    BottomNavItem(
        screen = Screen.History,
        icon = Icons.AutoMirrored.Filled.List,
        label = "History"
    ),
    BottomNavItem(
        screen = Screen.Settings,
        icon = Icons.Default.Settings,
        label = "Settings"
    )
)

/**
 * Main navigation composable with bottom navigation bar
 */
@Composable
fun LansonesScanNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val viewModelFactory = ViewModelFactory(context)
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAnalysis = {
                        navController.navigate(Screen.Analysis.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    },
                    onNavigateToScanDetail = { scanId ->
                        navController.navigate(Screen.ScanDetail.createRoute(scanId))
                    }
                )
            }
            composable(Screen.Analysis.route) { backStackEntry ->
                val viewModel: AnalysisViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = viewModelFactory
                )
                AnalysisScreen(
                    viewModel = viewModel,
                    onNavigateToScanDetail = { scanId ->
                        navController.navigate(Screen.ScanDetail.createRoute(scanId))
                    },
                    onNavigateToDashboard = {
                        Log.d("Navigation", "AnalysisScreen requested navigation to Dashboard")
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.History.route) {
                val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToScanDetail = { scanId ->
                        navController.navigate(Screen.ScanDetail.createRoute(scanId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
                SettingsScreen(
                    viewModel = viewModel
                )
            }
            composable(
                route = Screen.ScanDetail.route,
                arguments = listOf(
                    navArgument("scanId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val viewModel: ScanDetailViewModel = viewModel(factory = viewModelFactory)
                val scanId = backStackEntry.arguments?.getString("scanId") ?: ""

                LaunchedEffect(scanId) {
                    viewModel.loadScanDetails(scanId)
                }

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                if (uiState.isDeleted) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.error!!)
                    }
                } else if (uiState.scanResult != null) {
                    ScanDetailScreen(
                        scanResult = uiState.scanResult!!,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onShareResult = {
                            // TODO: Implement share functionality
                        },
                        onDeleteScan = {
                            viewModel.deleteScan(scanId)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Bottom navigation bar composable
 */
@Composable
private fun BottomNavigationBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { 
                it.route == item.screen.route 
            } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                },
                selected = isSelected,
                onClick = {
                    Log.d("Navigation", "Clicked on ${item.label}, current: ${currentDestination?.route}")
                    if (currentDestination?.route != item.screen.route) {
                        // Ultra-simple navigation - just navigate directly
                        navController.navigate(item.screen.route)
                        Log.d("Navigation", "Navigated to ${item.screen.route}")
                    }
                }
            )
        }
    }
}

/**
 * Placeholder screen composable for testing navigation
 */
@Composable
private fun PlaceholderScreen(screenName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$screenName Screen",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
    }
}