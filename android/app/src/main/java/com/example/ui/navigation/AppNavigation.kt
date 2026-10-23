package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.BatchImportScreen
import com.example.ui.screens.CaptureScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.ExportHistoryScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.LinkQueueScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PlatformsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BatchViewModel
import com.example.ui.viewmodel.CaptureViewModel
import com.example.ui.viewmodel.ExportViewModel
import com.example.ui.viewmodel.PlatformViewModel
import com.example.ui.viewmodel.LinkingViewModel
import com.example.data.repository.RegistrationStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.screens.WebsiteLinkingScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel(),
    batchViewModel: BatchViewModel = viewModel(),
    captureViewModel: CaptureViewModel = viewModel(),
    exportViewModel: ExportViewModel = viewModel(),
    platformViewModel: PlatformViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val linkingViewModel: LinkingViewModel = viewModel()
            val status by linkingViewModel.registrationStatus.collectAsState()
            
            when (status) {
                RegistrationStatus.ACTIVATED -> {
                    DashboardScreen(
                        authViewModel = authViewModel,
                        batchViewModel = batchViewModel,
                        onNavigateToBatchImport = { navController.navigate(Screen.BatchImport.route) },
                        onNavigateToLinkQueue = { batchId ->
                            navController.navigate(Screen.LinkQueue.createRoute(batchId))
                        },
                        onNavigateToPlatforms = { navController.navigate(Screen.Platforms.route) },
                        onNavigateToExport = { batchId ->
                            navController.navigate(Screen.Export.createRoute(batchId))
                        },
                        onNavigateToExportHistory = { navController.navigate(Screen.ExportHistory.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
                    )
                }
                else -> {
                    WebsiteLinkingScreen(
                        status = status,
                        linkingViewModel = linkingViewModel,
                        authViewModel = authViewModel,
                        onPairingSubmit = { code -> linkingViewModel.pairApp(code) }
                    )
                }
            }
        }

        composable(Screen.Linking.route) {
            val linkingViewModel: LinkingViewModel = viewModel()
            val status by linkingViewModel.registrationStatus.collectAsState()
            WebsiteLinkingScreen(
                status = status,
                linkingViewModel = linkingViewModel,
                authViewModel = authViewModel,
                onPairingSubmit = { code -> linkingViewModel.pairApp(code) }
            )
        }

        composable(Screen.BatchImport.route) {
            BatchImportScreen(
                authViewModel = authViewModel,
                batchViewModel = batchViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToQueue = { batchId ->
                    navController.navigate(Screen.LinkQueue.createRoute(batchId)) {
                        popUpTo(Screen.BatchImport.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.LinkQueue.route,
            arguments = listOf(navArgument("batchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
            LinkQueueScreen(
                batchId = batchId,
                batchViewModel = batchViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = { bId, linkId, url, platform ->
                    navController.navigate(Screen.Capture.createRoute(bId, linkId, url, platform))
                },
                onNavigateToExport = { bId ->
                    navController.navigate(Screen.Export.createRoute(bId))
                }
            )
        }

        composable(
            route = Screen.Capture.route,
            arguments = listOf(
                navArgument("batchId") { type = NavType.StringType },
                navArgument("linkId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType; defaultValue = "https://google.com" },
                navArgument("platform") { type = NavType.StringType; defaultValue = "Other" }
            )
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
            val linkId = backStackEntry.arguments?.getString("linkId") ?: ""
            val rawUrl = backStackEntry.arguments?.getString("url") ?: "https://google.com"
            val rawPlatform = backStackEntry.arguments?.getString("platform") ?: "Other"

            val url = java.net.URLDecoder.decode(rawUrl, "UTF-8")
            val platform = java.net.URLDecoder.decode(rawPlatform, "UTF-8")

            CaptureScreen(
                batchId = batchId,
                linkId = linkId,
                initialUrl = url,
                platform = platform,
                authViewModel = authViewModel,
                captureViewModel = captureViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Export.route,
            arguments = listOf(navArgument("batchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
            ExportScreen(
                batchId = batchId,
                authViewModel = authViewModel,
                exportViewModel = exportViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExportHistory.route) {
            ExportHistoryScreen(
                batchViewModel = batchViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Platforms.route) {
            PlatformsScreen(
                platformViewModel = platformViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenPlatformLogin = { loginUrl, platformName ->
                    navController.navigate(Screen.Capture.createRoute("PLATFORM-LOGIN", "AUTH", loginUrl, platformName))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                batchViewModel = batchViewModel,
                platformViewModel = platformViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlatforms = { navController.navigate(Screen.Platforms.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onLogoutComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(
                authViewModel = authViewModel,
                batchViewModel = batchViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
