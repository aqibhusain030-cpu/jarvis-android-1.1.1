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
    var whisperReady by remember { mutableStateOf(false) }
    var whisperChecked by remember { mutableStateOf(false) }
    var whisperDownloading by remember { mutableStateOf(false) }
    var whisperProgress by remember { mutableFloatStateOf(0f) }
    var whisperError by remember { mutableStateOf("") }
    val killed by killSwitch.isKilled.collectAsState()

    LaunchedEffect(modelManager) {
        val result = withContext(Dispatchers.IO) {
            val stt = modelManager.entry("whisper-small")?.let(modelManager::isPresent) == true
            val all = requiredModels.isNotEmpty() && requiredModels.all(modelManager::isPresent)
            stt to all
        }
        whisperReady = result.first
        modelsReady = result.second
        whisperChecked = true
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

    fun downloadSpeechModel() {
        if (whisperDownloading || whisperReady) return
        whisperDownloading = true
        whisperProgress = 0f
        whisperError = ""
        scope.launch {
            val file = modelManager.download("whisper-small") { current, total ->
                if (total > 0L) {\n                    val ratio = (current.toFloat() / total).coerceIn(0f, 1f)\n                    scope.launch { whisperProgress = ratio }\n                }
            }
            whisperReady = withContext(Dispatchers.IO) {
                val entry = modelManager.entry("whisper-small")
                entry != null && file != null && modelManager.isPresent(entry)
            }
            whisperDownloading = false
            if (whisperReady) {
                whisperProgress = 1f
                status = "Whisper speech model verified. Standalone transcription is ready."
            } else {
                whisperError = "Whisper download or SHA-256 verification failed. Retry on a stable connection."
            }
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

            val result = withContext(Dispatchers.IO) {
                val stt = modelManager.entry("whisper-small")?.let(modelManager::isPresent) == true
                val all = requiredModels.isNotEmpty() && requiredModels.all(modelManager::isPresent)
                stt to all
            }
            whisperReady = result.first
            modelsReady = result.second
            modelsChecked = true
            whisperChecked = true
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
                    !modelsChecked -> "Checking installed AI models…"
                    modelsReady -> "On-device AI models verified"
                    else -> "Full assistant models not installed"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (modelsReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (!modelsReady) {
                Text(
                    "The full assistant needs Qwen 3B and Whisper Small (about 2.5 GB total).",
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
                    Text(if (downloading) "Downloading assistant models…" else "Download full assistant models")
                }
            } else {
                Button(onClick = { startTalkNow() }, enabled = !killed && !downloading) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Talk now")
                }
                Text(
                    "Tap once, speak, and JARVIS will transcribe locally, reason on-device, and reply aloud.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = if (whisperReady) "Whisper multilingual speech model verified" else "Whisper speech model not installed",
                style = MaterialTheme.typography.bodyMedium,
                color = if (whisperReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (!whisperReady) {
                if (whisperDownloading) {
                    LinearProgressIndicator(progress = { whisperProgress }, modifier = Modifier.fillMaxWidth())
                    Text("${(whisperProgress * 100).toInt()}% of speech model downloaded", style = MaterialTheme.typography.labelSmall)
                }
                if (whisperError.isNotBlank()) {
                    Text(whisperError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = { downloadSpeechModel() },
                    enabled = whisperChecked && !whisperDownloading && !killed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (whisperDownloading) "Downloading Whisper…" else "Download speech model only (~488 MB)")
                }
            }
            OutlinedButton(
                onClick = { nav.navigate("transcribe") },
                enabled = whisperReady && !killed && !whisperDownloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Standalone speech recognition")
            }
            Text(
                "Automatic Hindi, English and Arabic transcription. This test uses the phone microphone and Whisper locally; it does not call Android SpeechRecognizer, keyboard dictation, cloud services, the LLM or TTS.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Always-on wake-word listening is separate and requires a correctly licensed, verified wake-word model.",
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
