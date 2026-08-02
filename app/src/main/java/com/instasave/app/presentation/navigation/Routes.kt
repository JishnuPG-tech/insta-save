package com.instasave.app.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Downloads : Screen("downloads", "Downloads")
    data object Login : Screen("login", "Session Sync")
    data object Settings : Screen("settings", "Settings")
    data object Preview : Screen("preview/{shortcode}", "Preview") {
        fun createRoute(shortcode: String) = "preview/$shortcode"
    }
}
