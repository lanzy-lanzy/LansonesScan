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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.ml.lansonesscan.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

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
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300)) }
        ) {
            composable(
                Screen.Dashboard.route,
                enterTransition = { slideInVertically(animationSpec = tween(400)) { it / 2 } + fadeIn(animationSpec = tween(400)) },
                exitTransition = { slideOutVertically(animationSpec = tween(400)) { -it / 2 } + fadeOut(animationSpec = tween(400)) }
            ) {
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
            composable(
                Screen.Analysis.route,
                enterTransition = { slideInHorizontally(animationSpec = tween(400)) { it } + fadeIn(animationSpec = tween(400)) },
                exitTransition = { slideOutHorizontally(animationSpec = tween(400)) { -it } + fadeOut(animationSpec = tween(400)) }
            ) { backStackEntry ->
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
            composable(
                Screen.History.route,
                enterTransition = { slideInHorizontally(animationSpec = tween(400)) { it } + fadeIn(animationSpec = tween(400)) },
                exitTransition = { slideOutHorizontally(animationSpec = tween(400)) { -it } + fadeOut(animationSpec = tween(400)) }
            ) {
                val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToScanDetail = { scanId ->
                        navController.navigate(Screen.ScanDetail.createRoute(scanId))
                    }
                )
            }
            composable(
                Screen.Settings.route,
                enterTransition = { slideInHorizontally(animationSpec = tween(400)) { it } + fadeIn(animationSpec = tween(400)) },
                exitTransition = { slideOutHorizontally(animationSpec = tween(400)) { -it } + fadeOut(animationSpec = tween(400)) }
            ) {
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
                ),
                enterTransition = { slideInVertically(animationSpec = tween(500)) { it } + fadeIn(animationSpec = tween(500)) },
                exitTransition = { slideOutVertically(animationSpec = tween(500)) { it } + fadeOut(animationSpec = tween(500)) },
                popEnterTransition = { slideInVertically(animationSpec = tween(500)) { -it } + fadeIn(animationSpec = tween(500)) },
                popExitTransition = { slideOutVertically(animationSpec = tween(500)) { it } + fadeOut(animationSpec = tween(500)) }
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
 * Enhanced bottom navigation bar with animations and gradient background
 */
@Composable
private fun BottomNavigationBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier.background(navigationGradient()),
        containerColor = Color.Transparent
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == item.screen.route
            } == true

            // Animation states
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "icon_scale"
            )

            val iconColor by animateColorAsState(
                targetValue = if (isSelected) BrandYellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
                label = "icon_color"
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.scale(scale)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (isSelected) BrandGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                selected = isSelected,
                onClick = {
                    Log.d("Navigation", "Clicked on ${item.label}, current: ${currentDestination?.route}")
                    if (currentDestination?.route != item.screen.route) {
                        navController.navigate(item.screen.route)
                        Log.d("Navigation", "Navigated to ${item.screen.route}")
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Transparent,
                    unselectedIconColor = Color.Transparent,
                    selectedTextColor = Color.Transparent,
                    unselectedTextColor = Color.Transparent,
                    indicatorColor = BrandGreen.copy(alpha = 0.2f)
                )
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