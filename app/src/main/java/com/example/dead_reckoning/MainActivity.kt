package com.example.dead_reckoning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.dead_reckoning.ui.diagnostics.DiagnosticsViewModel
import com.example.dead_reckoning.ui.experiments.ExperimentsViewModel
import com.example.dead_reckoning.ui.home.HomeViewModel
import com.example.dead_reckoning.ui.map.MapViewModel
import com.example.dead_reckoning.ui.navigation.MainAppNavigation
import com.example.dead_reckoning.ui.settings.SettingsViewModel
import com.example.dead_reckoning.ui.theme.DeadReckoningTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        val app = application as DeadReckoningApp
        HomeViewModel.Factory(app.repository)
    }

    private val mapViewModel: MapViewModel by viewModels {
        val app = application as DeadReckoningApp
        MapViewModel.Factory(app.repository)
    }

    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels {
        val app = application as DeadReckoningApp
        DiagnosticsViewModel.Factory(app.repository)
    }

    private val experimentsViewModel: ExperimentsViewModel by viewModels {
        val app = application as DeadReckoningApp
        ExperimentsViewModel.Factory(app.repository)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as DeadReckoningApp
        SettingsViewModel.Factory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeadReckoningTheme {
                MainAppNavigation(
                    homeViewModel = homeViewModel,
                    mapViewModel = mapViewModel,
                    diagnosticsViewModel = diagnosticsViewModel,
                    experimentsViewModel = experimentsViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
