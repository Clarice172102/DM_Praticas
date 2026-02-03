package com.example.WeatherApp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.WeatherApp.model.MainViewModel
import com.example.WeatherApp.ui.CityDialog
import com.example.WeatherApp.ui.nav.BottomNavBar
import com.example.WeatherApp.ui.nav.BottomNavItem
import com.example.WeatherApp.ui.nav.MainNavHost
import com.example.WeatherApp.ui.theme.WeatherAppTheme
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.WeatherApp.api.WeatherService
import com.example.WeatherApp.db.fb.FBDatabase
import com.example.WeatherApp.model.MainViewModelFactory
import com.example.WeatherApp.monitor.ForecastMonitor
import com.example.WeatherApp.ui.nav.Route
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val fbDB = remember { FBDatabase() }
            val weatherService = remember { WeatherService(this) }
            val forecastMonitor = remember { ForecastMonitor (this) }


            val viewModel : MainViewModel = viewModel(
                factory = MainViewModelFactory(fbDB, weatherService, forecastMonitor)
            )

            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { intent ->
                    viewModel.city = intent.getStringExtra("city")
                    viewModel.page = Route.Home
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }


            val launcher = rememberLauncherForActivityResult(contract =
                ActivityResultContracts.RequestPermission(), onResult = {} )
            var showDialog by remember { mutableStateOf(false) }
            val navController = rememberNavController()
            val currentRoute = navController.currentBackStackEntryAsState()
            val showButton = currentRoute.value?.destination?.hasRoute(Route.List::class) == true

            WeatherAppTheme {
                if (showDialog) CityDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { city ->
                        if (city.isNotBlank()) viewModel.addCity(city)
                        showDialog = false
                    })
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                val name = viewModel.user?.name?:"[carregando...]"
                                Text("Bem-vindo/a! $name")
                            },

                            actions = {

                                IconButton( onClick = { Firebase.auth.signOut()
                                    } ) {
                                    Icon(
                                        imageVector =
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Localized description"
                                    )
                                }
                            }
                        )
                    },

                    bottomBar = {
                        val items = listOf(
                            BottomNavItem.HomeButton,
                            BottomNavItem.ListButton,
                            BottomNavItem.MapButton,

                            )

                        BottomNavBar(viewModel, items)

                    },

                    floatingActionButton = {

                        if (showButton) {
                            FloatingActionButton(onClick = { showDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        launcher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        MainNavHost(
                            navController,
                            viewModel
                        )
                    }
                    LaunchedEffect(viewModel.page) {
                        navController.navigate(viewModel.page) {
                            // Volta pilha de navegação até HomePage (startDest).
                            navController.graph.startDestinationRoute?.let {
                                popUpTo(it) {
                                    saveState = true
                                }
                                restoreState = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}

