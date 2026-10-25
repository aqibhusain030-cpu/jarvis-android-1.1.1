package com.jarvis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.jarvis.R
import com.jarvis.brain.RemoteInferenceCredentialStore
import com.jarvis.config.ConfigRepository
import com.jarvis.security.KillSwitch
import com.jarvis.security.SecurityUnlockManager
import com.jarvis.service.JarvisForegroundService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    nav: NavController,
    killSwitch: KillSwitch,
    unlock: SecurityUnlockManager,
    config: ConfigRepository,
    remoteCredentials: RemoteInferenceCredentialStore
) {
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    val scope = rememberCoroutineScope()
    var unlocked by remember { mutableStateOf(false) }
    val killed by killSwitch.isKilled.collectAsState()
    val current by config.current.collectAsState()

    var remoteEnabled by remember(current.remoteInferenceEnabled) {
        mutableStateOf(current.remoteInferenceEnabled)
    }
    var remoteUrl by remember(current.remoteInferenceBaseUrl) {
        mutableStateOf(current.remoteInferenceBaseUrl)
    }
    var remoteModel by remember(current.remoteInferenceModel) {
        mutableStateOf(current.remoteInferenceModel)
    }
    var remoteToken by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.screen_settings)) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) { Text("←") }
            }
        )
    }) { p ->
        Column(
            Modifier.padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings_language))
            Text(stringResource(R.string.settings_voice))
            Text(stringResource(R.string.settings_safety))

            HorizontalDivider()
            Text("2T / Remote inference", style = MaterialTheme.typography.titleMedium)
            Text(
                "Disabled by default. When enabled, JARVIS may use the configured HTTPS inference server; Android tools remain authoritative."
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable remote inference")
                Switch(
                    checked = remoteEnabled,
                    onCheckedChange = { remoteEnabled = it }
                )
            }

            OutlinedTextField(
                value = remoteUrl,
                onValueChange = { remoteUrl = it.take(2048) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Inference server URL") },
                placeholder = { Text("https://server.example") },
                singleLine = true
            )

            OutlinedTextField(
                value = remoteModel,
                onValueChange = { remoteModel = it.take(256) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model ID") },
                singleLine = true
            )

            OutlinedTextField(
                value = remoteToken,
                onValueChange = { remoteToken = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bearer token (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        runCatching {
                            config.setRemoteInference(
                                enabled = remoteEnabled,
                                baseUrl = remoteUrl,
                                model = remoteModel,
                                timeoutMs = current.remoteInferenceTimeoutMs
                            )
                            if (remoteToken.isNotBlank()) {
                                remoteCredentials.setToken(remoteToken)
                                remoteToken = ""
                            }
                            status = "Remote inference settings saved."
                        }.onFailure {
                            status = it.message ?: "Could not save remote inference settings."
                        }
                    }
                }) {
                    Text("Save")
                }

                OutlinedButton(onClick = {
                    remoteCredentials.clear()
                    remoteToken = ""
                    status = "Remote inference token cleared."
                }) {
                    Text("Clear token")
                }
            }

            if (status.isNotBlank()) Text(status)

            HorizontalDivider()

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
                Button(onClick = {
                    killSwitch.kill()
                    JarvisForegroundService.stop(ctx)
                }) {
                    Text(stringResource(R.string.action_emergency_stop))
                }
            }

            Button(onClick = {
                activity?.let { unlock.authenticate(it) { ok -> unlocked = ok } }
            }) {
                Text(stringResource(R.string.action_unlock_high_risk))
            }

            Text(
                if (unlocked) stringResource(R.string.high_risk_unlocked)
                else stringResource(R.string.high_risk_locked)
            )

            Button(
                onClick = {
                    if (!killed) JarvisForegroundService.start(ctx)
                },
                enabled = !killed
            ) {
                Text(stringResource(R.string.action_start))
            }
        }
    }
}
