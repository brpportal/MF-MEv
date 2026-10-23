package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.repository.SyncRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MFMPanelTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule periodic background metadata sync via WorkManager
        try {
            SyncRepository(this).scheduleBackgroundSync()
        } catch (e: Exception) {
            // WorkManager fallback if initialization is delayed
        }

        setContent {
            MFMPanelTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavigation()
                }
            }
        }
    }
}
