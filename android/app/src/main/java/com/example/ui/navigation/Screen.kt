package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Platforms : Screen("platforms")
    object BatchImport : Screen("batch_import")
    object LinkQueue : Screen("link_queue/{batchId}") {
        fun createRoute(batchId: String) = "link_queue/$batchId"
    }
    object Capture : Screen("capture/{batchId}/{linkId}?url={url}&platform={platform}") {
        fun createRoute(batchId: String, linkId: String, url: String, platform: String): String {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val encodedPlatform = java.net.URLEncoder.encode(platform, "UTF-8")
            return "capture/$batchId/$linkId?url=$encodedUrl&platform=$encodedPlatform"
        }
    }
    object Export : Screen("export/{batchId}") {
        fun createRoute(batchId: String) = "export/$batchId"
    }
    object ExportHistory : Screen("export_history")
    object Settings : Screen("settings")
    object Diagnostics : Screen("diagnostics")
    object Linking : Screen("linking")
}
