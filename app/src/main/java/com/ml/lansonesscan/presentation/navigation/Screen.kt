package com.ml.lansonesscan.presentation.navigation

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Analysis : Screen("analysis")
    object History : Screen("history")
    object Settings : Screen("settings")
    object ScanDetail : Screen("scan_detail/{scanId}") {
        fun createRoute(scanId: String) = "scan_detail/$scanId"
    }
}