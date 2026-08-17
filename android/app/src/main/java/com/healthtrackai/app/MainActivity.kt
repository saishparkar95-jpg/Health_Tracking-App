package com.healthtrackai.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.data.sensors.StepSensorTracker
import com.healthtrackai.app.ui.components.BottomNavigationBar
import com.healthtrackai.app.ui.navigation.AppNavigation
import com.healthtrackai.app.ui.navigation.Screen
import com.healthtrackai.app.ui.theme.HealthTrackAITheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Sensor permissions callback
        val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        } else {
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) { }

        setContent {
            val context = LocalContext.current
            val healthState = remember { HealthStateHolder(context) }
            val stepTracker = remember { StepSensorTracker(context, healthState) }
            val reminderManager = remember { com.healthtrackai.app.data.notifications.SmartHealthReminderManager(context, healthState) }

            DisposableEffect(Unit) {
                try {
                    stepTracker.startListening()
                } catch (e: Throwable) { }
                try {
                    reminderManager.syncLiveStepNotification()
                } catch (e: Throwable) { }
                onDispose {
                    try {
                        stepTracker.stopListening()
                    } catch (e: Throwable) { }
                }
            }

            HealthTrackAITheme(themeMode = healthState.themeMode) {
                MainAppScreen(
                    healthState = healthState,
                    stepTracker = stepTracker,
                    onRequestPermissions = { requestSensorPermissions() }
                )
            }
        }
    }

    private fun requestSensorPermissions() {
        try {
            val permissionsToRequest = mutableListOf<String>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BODY_SENSORS)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }

            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } catch (e: Throwable) {
            // Prevent lifecycle / permission launch crash
        }
    }
}

@Composable
fun MainAppScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    stepTracker: StepSensorTracker? = null,
    onRequestPermissions: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show BottomNavigationBar on main dashboard tabs (Home, Activity, Goals, Profile)
    val shouldShowBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            paddingValues = innerPadding,
            healthState = healthState,
            stepTracker = stepTracker,
            onRequestPermissions = onRequestPermissions
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppScreenPreview() {
    HealthTrackAITheme {
        MainAppScreen()
    }
}
