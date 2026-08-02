package com.instasave.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.instasave.app.presentation.downloads.DownloadsScreen
import com.instasave.app.presentation.home.HomeScreen
import com.instasave.app.presentation.login.LoginCookieScreen
import com.instasave.app.presentation.preview.PreviewScreen
import com.instasave.app.presentation.settings.SettingsScreen
import com.instasave.app.presentation.theme.PitchBlack

@Composable
fun InstaSaveNavHost(
    navController: NavHostController = rememberNavController(),
    initialUrl: String? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = PitchBlack,
        bottomBar = {
            if (currentRoute != Screen.Preview.route) {
                BottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    initialSharedUrl = initialUrl,
                    onNavigateToDownloads = {
                        navController.navigate(Screen.Downloads.route)
                    },
                    onNavigateToPreview = { shortcode ->
                        navController.navigate(Screen.Preview.createRoute(shortcode))
                    }
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onNavigateToPreview = { shortcode ->
                        navController.navigate(Screen.Preview.createRoute(shortcode))
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginCookieScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.Preview.route) { backStackEntry ->
                val shortcode = backStackEntry.arguments?.getString("shortcode") ?: ""
                PreviewScreen(
                    shortcode = shortcode,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
