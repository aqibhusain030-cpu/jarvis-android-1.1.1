package com.jarvis

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.brain.BrainBridge
import com.jarvis.brain.RemoteInferenceCredentialStore
import com.jarvis.config.ConfigRepository
import com.jarvis.memory.MemoryStore
import com.jarvis.models.ModelManager
import com.jarvis.security.KillSwitch
import com.jarvis.security.SecurityUnlockManager
import com.jarvis.service.JarvisForegroundService
import com.jarvis.ui.*
import com.jarvis.ui.theme.JarvisTheme
import com.jarvis.voice.TextToSpeechBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var killSwitch: KillSwitch
    @Inject lateinit var securityUnlock: SecurityUnlockManager
    @Inject lateinit var config: ConfigRepository
    @Inject lateinit var remoteCredentials: RemoteInferenceCredentialStore
    @Inject lateinit var brain: BrainBridge
    @Inject lateinit var models: ModelManager
    @Inject lateinit var memory: MemoryStore
    @Inject lateinit var tts: TextToSpeechBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisTheme {
                JarvisNav(
                    killSwitch, securityUnlock, config, remoteCredentials,
                    brain, models, memory, tts
                )
            }
        }
    }
}

@Composable
private fun JarvisNav(
    killSwitch: KillSwitch,
    securityUnlock: SecurityUnlockManager,
    config: ConfigRepository,
    remoteCredentials: RemoteInferenceCredentialStore,
    brain: BrainBridge,
    models: ModelManager,
    memory: MemoryStore,
    tts: TextToSpeechBridge
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("jarvis_setup", Context.MODE_PRIVATE) }
    val startDestination = remember {
        if (preferences.getBoolean("onboarding_complete", false)) "home" else "onboarding"
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(onDone = {
                preferences.edit().putBoolean("onboarding_complete", true).apply()
                nav.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable("home") { HomeScreen(nav, killSwitch, brain, models, memory, tts) }
        composable("settings") { SettingsScreen(nav, killSwitch, securityUnlock, config, remoteCredentials) }
        composable("goals") { GoalsScreen(nav) }
        composable("memory") { MemoryScreen(nav) }
        composable("logs") { LogsScreen(nav) }
    }
}
