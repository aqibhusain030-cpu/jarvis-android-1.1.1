package com.jarvis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.security.KillSwitch
import com.jarvis.security.SecurityUnlockManager
import com.jarvis.models.ModelManager
import com.jarvis.ui.*
import com.jarvis.ui.theme.JarvisTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var killSwitch: KillSwitch
    @Inject lateinit var securityUnlock: SecurityUnlockManager
    @Inject lateinit var modelManager: ModelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisTheme {
                JarvisNav(killSwitch, securityUnlock, modelManager)
            }
        }
    }
}

@Composable
private fun JarvisNav(killSwitch: KillSwitch, securityUnlock: SecurityUnlockManager, modelManager: ModelManager) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "onboarding") {
        composable("onboarding") { OnboardingScreen(onDone = { nav.navigate("home") }) }
        composable("home") { HomeScreen(nav, killSwitch, modelManager) }
        composable("settings") { SettingsScreen(nav, killSwitch, securityUnlock) }
        composable("goals") { GoalsScreen(nav) }
        composable("memory") { MemoryScreen(nav) }
        composable("logs") { LogsScreen(nav) }
    }
}
