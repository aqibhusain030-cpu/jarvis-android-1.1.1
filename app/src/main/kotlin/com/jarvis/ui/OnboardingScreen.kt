package com.jarvis.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jarvis.R

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val permsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { step = 3 }

    Scaffold { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                0 -> {
                    Text(stringResource(R.string.onboard_welcome),
                        style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { step = 1 }) { Text(stringResource(R.string.onboard_start)) }
                }
                1 -> {
                    Text(stringResource(R.string.onboard_language),
                        style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.onboard_language_body))
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { step = 2 }) { Text("हिन्दी") }
                        OutlinedButton(onClick = { step = 2 }) { Text("English") }
                    }
                }
                2 -> {
                    Text(stringResource(R.string.onboard_permissions),
                        style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.onboard_permissions_body))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        val toAsk = PERMISSIONS.filter {
                            Build.VERSION.SDK_INT >= 33 || it != Manifest.permission.POST_NOTIFICATIONS
                        }.toTypedArray()
                        permsLauncher.launch(toAsk)
                    }) { Text(stringResource(R.string.onboard_grant)) }
                }
                3 -> {
                    Text(stringResource(R.string.onboard_voice),
                        style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.onboard_voice_body))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { step = 4 }) { Text(stringResource(R.string.onboard_done)) }
                }
                else -> {
                    Text(stringResource(R.string.onboard_ready),
                        style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onDone) { Text(stringResource(R.string.onboard_begin)) }
                }
            }
        }
    }
}

private val PERMISSIONS = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
}
