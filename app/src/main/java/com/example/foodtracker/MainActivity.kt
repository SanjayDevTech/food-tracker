package com.example.foodtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.ui.AppContainer
import com.example.foodtracker.ui.LocalAppContainer
import com.example.foodtracker.ui.navigation.AppNav
import com.example.foodtracker.ui.theme.FoodTrackerTheme
import com.example.foodtracker.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the system can hand off from the
        // splash theme to the post-splash theme cleanly.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = AppContainer.from(application as FoodTrackerApp)
        setContent {
            val themeMode by container.profileStore.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.Amoled)
            FoodTrackerTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    AppNav()
                }
            }
        }
    }
}
