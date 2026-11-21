package com.duchamp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.duchamp.data.repo.ProfileRepository
import com.duchamp.design.DuchampColor
import com.duchamp.design.DuchampTheme
import com.duchamp.feature.gamemode.GameModeScreen
import com.duchamp.feature.health.HealthScreen
import com.duchamp.feature.monitor.MonitorScreen
import com.duchamp.feature.network.NetworkScreen
import com.duchamp.feature.onboarding.OnboardingScreen
import com.duchamp.feature.sessions.SessionsScreen
import com.duchamp.feature.system.SystemScreen
import com.duchamp.nav.DuchampDestination
import com.duchamp.nav.destinations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DuchampTheme {
                AppRoot(profileRepository = profileRepository)
            }
        }
    }
}

@Composable
private fun AppRoot(profileRepository: ProfileRepository) {
    var onboarded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        onboarded = profileRepository.profile.first().onboarded
    }

    if (!onboarded) {
        OnboardingScreen(onFinished = { onboarded = true })
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Surface(modifier = Modifier.fillMaxSize(), color = DuchampColor.Background) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destinationIcon(destination), contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = DuchampDestination.GAME_MODE.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(DuchampDestination.GAME_MODE.route) {
                    GameModeScreen(modifier = Modifier.fillMaxSize())
                }
                composable(DuchampDestination.MONITOR.route) {
                    MonitorScreen(modifier = Modifier.fillMaxSize())
                }
                composable(DuchampDestination.SESSIONS.route) {
                    SessionsScreen(modifier = Modifier.fillMaxSize())
                }
                composable(DuchampDestination.HEALTH.route) {
                    HealthScreen(modifier = Modifier.fillMaxSize())
                }
                composable(DuchampDestination.SYSTEM.route) {
                    SystemScreen(modifier = Modifier.fillMaxSize())
                }
                composable(DuchampDestination.NETWORK.route) {
                    NetworkScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun destinationIcon(destination: DuchampDestination): ImageVector = when (destination) {
    DuchampDestination.GAME_MODE -> Icons.Filled.Gamepad
    DuchampDestination.MONITOR -> Icons.Filled.Dashboard
    DuchampDestination.SESSIONS -> Icons.Filled.History
    DuchampDestination.HEALTH -> Icons.Filled.BatteryFull
    DuchampDestination.SYSTEM -> Icons.Filled.Settings
    DuchampDestination.NETWORK -> Icons.Filled.Wifi
}
