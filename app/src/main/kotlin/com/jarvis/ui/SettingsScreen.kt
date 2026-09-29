package com.jarvis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.jarvis.R
import com.jarvis.security.KillSwitch
import com.jarvis.security.SecurityUnlockManager
import com.jarvis.service.JarvisForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, killSwitch: KillSwitch, unlock: SecurityUnlockManager) {
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    var unlocked by remember { mutableStateOf(false) }
    val killed by killSwitch.isKilled.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.screen_settings)) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Text("←") } })
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_language))
            Text(stringResource(R.string.settings_voice))
            Text(stringResource(R.string.settings_safety))
            if (killed) {
                Button(onClick = {
                    activity?.let {
                        unlock.authenticate(it) { ok ->
                            if (ok) {
                                killSwitch.resume()
                                unlocked = false
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.action_resume))
                }
            } else {
                Button(onClick = { killSwitch.kill(); JarvisForegroundService.stop(ctx) }) {
                    Text(stringResource(R.string.action_emergency_stop))
                }
            }
            Button(onClick = {
                activity?.let { unlock.authenticate(it) { ok -> unlocked = ok } }
            }) { Text(stringResource(R.string.action_unlock_high_risk)) }
            Text(if (unlocked) stringResource(R.string.high_risk_unlocked) else stringResource(R.string.high_risk_locked))
            Button(onClick = {
                if (!killed) JarvisForegroundService.start(ctx)
            }, enabled = !killed) { Text(stringResource(R.string.action_start)) }
        }
    }
}
