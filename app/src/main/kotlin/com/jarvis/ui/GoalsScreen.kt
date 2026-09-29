package com.jarvis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.jarvis.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(nav: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.screen_goals)) }) }) { p ->
        Box(Modifier.padding(p)) { Text(stringResource(R.string.goals_empty)) }
    }
}
