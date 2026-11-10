package com.jarvis.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.jarvis.R
import com.jarvis.models.ModelManager
import com.jarvis.security.KillSwitch
import com.jarvis.service.JarvisForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController, killSwitch: KillSwitch, modelManager: ModelManager) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val requiredModels = remember(modelManager) {
        modelManager.all().filter { it.id == "qwen3b" || it.id == "whisper-small" }
    }
    var status by remember { mutableStateOf("idle") }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadError by remember { mutableStateOf("") }
    var modelsReady by remember { mutableStateOf(false) }
    var modelsChecked by remember { mutableStateOf(false) }
    val killed by killSwitch.isKilled.collectAsState()

    // SHA-256 checks read multi-gigabyte model files; keep them off the main thread.
    LaunchedEffect(modelManager) {
        modelsReady = withContext(Dispatchers.IO) {
            requiredModels.isNotEmpty() && requiredModels.all(modelManager::isPresent)
        }
        modelsChecked = true
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = if (granted) {
            if (JarvisForegroundService.startOneShot(ctx)) {
                "Listening once — speak after the listening notification appears."
            } else {
                "Stop the current JARVIS microphone session before starting another."
            }
        } else {
            "Microphone permission is required for voice input."
        }
    }

    fun startTalkNow() {
        if (!modelsReady) {
            status = "Download the verified Qwen and Whisper models first."
            return
        }
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            status = if (JarvisForegroundService.startOneShot(ctx)) {
                "Listening once — speak after the listening notification appears."
            } else {
                "Stop the current JARVIS microphone session before starting another."
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun downloadOfflineModels() {
        if (downloading) return
        downloading = true
        downloadProgress = 0f
        downloadError = ""
        scope.launch {
            val totalBytes = requiredModels.sumOf { it.sizeBytes }.coerceAtLeast(1L)
            var completedBytes = 0L
            var failedName: String? = null

            for (entry in requiredModels) {
                val alreadyInstalled = withContext(Dispatchers.IO) { modelManager.isPresent(entry) }
                if (alreadyInstalled) {
                    completedBytes += entry.sizeBytes
                    downloadProgress = completedBytes.toFloat() / totalBytes
                    continue
                }
                val alreadyDownloaded = completedBytes
                var lastReportedBytes = 0L
                val file = modelManager.download(entry.id) { current, total ->
                    if (current - lastReportedBytes >= 8L * 1024L * 1024L || current >= total) {
                        lastReportedBytes = current
                        val ratio = ((alreadyDownloaded + current).toFloat() / totalBytes).coerceIn(0f, 1f)
                        scope.launch { downloadProgress = ratio }
                    }
                }
                if (file == null) {
                    failedName = entry.filename
                    break
                }
                completedBytes += entry.sizeBytes
                downloadProgress = (completedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
            }

            modelsReady = withContext(Dispatchers.IO) {
                requiredModels.isNotEmpty() && requiredModels.all(modelManager::isPresent)
            }
            modelsChecked = true
            downloading = false
            if (modelsReady) {
                downloadProgress = 1f
                status = "Verified offline models are ready. Tap Talk now to use JARVIS."
            } else {
                downloadError = "Could not download or verify ${failedName ?: "the required model"}. Check your connection and retry."
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
            IconButton(onClick = { nav.navigate("goals") }) { Icon(Icons.Default.CheckCircle, "Goals") }
            IconButton(onClick = { nav.navigate("memory") }) { Icon(Icons.Default.FavoriteBorder, "Memory") }
            IconButton(onClick = { nav.navigate("logs") }) { Icon(Icons.Default.List, "Logs") }
            IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Default.Settings, "Settings") }
        })
    }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = if (killed) "KILLED" else status,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = when {
                    !modelsChecked -> "Checking installed model files…"
                    modelsReady -> "On-device AI models verified"
                    else -> "On-device models not installed"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (modelsReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (!modelsReady) {
                Text(
                    "Download Qwen 3B and Whisper Small once (about 2.5 GB). The app stores and verifies them itself — no Termux required. Use Wi-Fi and keep sufficient free storage.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (downloading) {
                    LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth())
                    Text("${(downloadProgress * 100).toInt()}% downloaded", style = MaterialTheme.typography.labelSmall)
                }
                if (downloadError.isNotBlank()) {
                    Text(downloadError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { downloadOfflineModels() }, enabled = modelsChecked && !downloading && !killed) {
                    Text(
                        when {
                            !modelsChecked -> "Checking models…"
                            downloading -> "Downloading models…"
                            else -> "Download offline models"
                        }
                    )
                }
            } else {
                Button(onClick = { startTalkNow() }, enabled = !killed && !downloading) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Talk now")
                }
                Text(
                    "Tap once, speak, and JARVIS will transcribe locally, reason on-device, and reply aloud. The microphone session stops after the command.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "Always-on wake-word listening is separate and requires a correctly licensed, verified wake-word model. This build does not silently install non-commercial model weights.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = { nav.navigate("settings") }) { Text("Settings & service controls") }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { JarvisForegroundService.stop(ctx); status = "stopped" }) {
                Text(stringResource(R.string.action_stop))
            }
        }
    }
}
