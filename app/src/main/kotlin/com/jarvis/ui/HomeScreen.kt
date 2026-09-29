package com.jarvis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jarvis.R
import com.jarvis.security.KillSwitch
import com.jarvis.service.JarvisForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController, killSwitch: KillSwitch) {
    val ctx = LocalContext.current
    var status by remember { mutableStateOf("idle") }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
            IconButton(onClick = { nav.navigate("goals") }) { Icon(Icons.Default.CheckCircle, "Goals") }
            IconButton(onClick = { nav.navigate("memory") }) { Icon(Icons.Default.FavoriteBorder, "Memory") }
            IconButton(onClick = { nav.navigate("logs") }) { Icon(Icons.Default.List, "Logs") }
            IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Default.Settings, "Settings") }
        })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Text(
                text = if (killSwitch.isKilled.collectAsState().value) "KILLED" else status,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = !killSwitch.isKilled.collectAsState().value, onClick = {
                    status = if (JarvisForegroundService.start(ctx)) "starting" else "microphone permission required"
                }) { Text(stringResource(R.string.action_start)) }
                OutlinedButton(onClick = { JarvisForegroundService.stop(ctx); status = "stopped" }) {
                    Text(stringResource(R.string.action_stop))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_safety))
            Spacer(Modifier.weight(1f))
        }
    }
}
