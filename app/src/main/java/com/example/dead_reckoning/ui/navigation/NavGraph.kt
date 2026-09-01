package com.example.dead_reckoning.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dead_reckoning.data.repository.TelemetryRepository
import com.example.dead_reckoning.ui.diagnostics.DiagnosticsScreen
import com.example.dead_reckoning.ui.diagnostics.DiagnosticsViewModel
import com.example.dead_reckoning.ui.experiments.ExperimentsScreen
import com.example.dead_reckoning.ui.experiments.ExperimentsViewModel
import com.example.dead_reckoning.ui.home.HomeScreen
import com.example.dead_reckoning.ui.home.HomeViewModel
import com.example.dead_reckoning.ui.map.MapScreen
import com.example.dead_reckoning.ui.map.MapViewModel
import com.example.dead_reckoning.ui.settings.SettingsScreen
import com.example.dead_reckoning.ui.settings.SettingsViewModel

enum class NavTarget(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    MAP("Map", Icons.Default.Map),
    DIAGNOSTICS("Diagnostics", Icons.Default.Analytics),
    EXPERIMENTS("Experiments", Icons.Default.Science),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainAppNavigation(
    homeViewModel: HomeViewModel,
    mapViewModel: MapViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    experimentsViewModel: ExperimentsViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentTarget by remember { mutableStateOf(NavTarget.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavTarget.entries.forEach { target ->
                    NavigationBarItem(
                        selected = currentTarget == target,
                        onClick = { currentTarget = target },
                        icon = { Icon(imageVector = target.icon, contentDescription = target.title) },
                        label = { Text(target.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentTarget) {
            NavTarget.HOME -> HomeScreen(viewModel = homeViewModel, modifier = modifier)
            NavTarget.MAP -> MapScreen(viewModel = mapViewModel, modifier = modifier)
            NavTarget.DIAGNOSTICS -> DiagnosticsScreen(viewModel = diagnosticsViewModel, modifier = modifier)
            NavTarget.EXPERIMENTS -> ExperimentsScreen(viewModel = experimentsViewModel, modifier = modifier)
            NavTarget.SETTINGS -> SettingsScreen(viewModel = settingsViewModel, modifier = modifier)
        }
    }
}
