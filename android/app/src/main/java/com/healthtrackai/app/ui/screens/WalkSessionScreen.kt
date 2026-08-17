package com.healthtrackai.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.location.GpsRouteTracker
import com.healthtrackai.app.data.location.RoutePoint
import com.healthtrackai.app.data.models.ExerciseCategory
import com.healthtrackai.app.data.models.ExerciseSession
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.data.sensors.StepSensitivity
import com.healthtrackai.app.data.sensors.StepSensorTracker
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.RoseAccent
import kotlinx.coroutines.delay

@Composable
fun WalkSessionScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    stepTracker: StepSensorTracker? = null,
    onRequestPermissions: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gpsTracker = remember { GpsRouteTracker(context) }

    var isActive by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var sessionSteps by remember { mutableIntStateOf(0) }
    var currentCadence by remember { mutableIntStateOf(0) }
    var currentSpeedKmh by remember { mutableFloatStateOf(0f) }
    var isSimulating by remember { mutableStateOf(false) }
    var selectedViewTab by remember { mutableIntStateOf(0) } // 0: Steps, 1: Route Map
    var showCompletedDialog by remember { mutableStateOf(false) }
    var showSensitivityDialog by remember { mutableStateOf(false) }

    val routePoints = remember { mutableStateListOf<RoutePoint>() }

    // Wire live sensor & GPS callback
    DisposableEffect(stepTracker, isActive) {
        if (stepTracker != null && isActive) {
            stepTracker.startListening()
            stepTracker.onLiveStepCallback = { increment, cadence ->
                sessionSteps += increment
                currentCadence = cadence
            }
        }
        if (isActive) {
            gpsTracker.startTracking()
            gpsTracker.onLocationUpdated = { point, _, speed ->
                routePoints.add(point)
                currentSpeedKmh = speed
            }
        }
        onDispose {
            if (stepTracker != null) {
                stepTracker.onLiveStepCallback = null
            }
            gpsTracker.stopTracking()
        }
    }

    // Timer loop
    LaunchedEffect(isActive) {
        while (isActive) {
            delay(1000)
            elapsedSeconds++
            if (isSimulating) {
                val simIncrement = if (elapsedSeconds % 2 == 0) 2 else 1
                sessionSteps += simIncrement
                currentCadence = 108
                healthState.addSteps(simIncrement)
                val simPoint = gpsTracker.generateSimulatedWaypoint(elapsedSeconds / 180f)
                routePoints.add(simPoint)
                currentSpeedKmh = simPoint.speedKmh
            }
        }
    }

    val distanceKm = (sessionSteps * 0.00075f).coerceAtLeast(gpsTracker.totalDistanceMeters / 1000f)
    val calories = (sessionSteps * 0.042f).toInt()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActive) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Live Outdoor Walk & GPS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Sensor pedometer & live route mapping",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                IconButton(onClick = { showSensitivityDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Sensor Sensitivity",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // View Tabs (Pedometer vs GPS Route Map)
        TabRow(
            selectedTabIndex = selectedViewTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = EmeraldPrimary,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Tab(
                selected = selectedViewTab == 0,
                onClick = { selectedViewTab = 0 },
                text = { Text("👟 Step Counter", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedViewTab == 1,
                onClick = { selectedViewTab = 1 },
                text = { Text("🗺️ Live Route Map", fontWeight = FontWeight.Bold) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isActive) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = if (isActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) {
                            "🟢 Live Tracking • ${String.format("%.1f", currentSpeedKmh)} km/h • $currentCadence SPM"
                        } else if (elapsedSeconds > 0) {
                            "⏸️ Walk Paused"
                        } else {
                            "📱 Ready to Walk • GPS & Motion Sensors Connected"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Tab Content: Pedometer or Map
            if (selectedViewTab == 0) {
                // Pedometer Card
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (isActive) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .scale(pulseScale)
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (isActive) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = if (isActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "$sessionSteps",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 52.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "SESSION STEPS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val mins = elapsedSeconds / 60
                            val secs = elapsedSeconds % 60
                            WalkStatColumn("Time", String.format("%02d:%02d", mins, secs))
                            WalkStatColumn("Distance", String.format("%.2f km", distanceKm))
                            WalkStatColumn("Calories", "$calories kcal")
                            WalkStatColumn("Cadence", "$currentCadence spm")
                        }
                    }
                }
            } else {
                // Live Route Map Canvas
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🗺️ GPS Route Path",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${routePoints.size} waypoints",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Vector Path Drawing Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            if (routePoints.size >= 2) {
                                val minLat = routePoints.minOf { it.latitude }
                                val maxLat = routePoints.maxOf { it.latitude }
                                val minLng = routePoints.minOf { it.longitude }
                                val maxLng = routePoints.maxOf { it.longitude }

                                val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
                                val lngSpan = (maxLng - minLng).coerceAtLeast(0.0001)

                                val paddingPx = 30f
                                val drawWidth = size.width - (paddingPx * 2)
                                val drawHeight = size.height - (paddingPx * 2)

                                val path = Path()
                                routePoints.forEachIndexed { index, pt ->
                                    val x = paddingPx + ((pt.longitude - minLng) / lngSpan).toFloat() * drawWidth
                                    val y = paddingPx + (1f - ((pt.latitude - minLat) / latSpan).toFloat()) * drawHeight

                                    if (index == 0) {
                                        path.moveTo(x, y)
                                        drawCircle(color = EmeraldPrimary, radius = 8f, center = Offset(x, y))
                                    } else {
                                        path.lineTo(x, y)
                                    }

                                    if (index == routePoints.size - 1) {
                                        drawCircle(color = CyanAccent, radius = 10f, center = Offset(x, y))
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = EmeraldPrimary,
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            } else {
                                drawCircle(color = EmeraldPrimary, radius = 8f, center = Offset(size.width / 2, size.height / 2))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Speed: ${String.format("%.1f", currentSpeedKmh)} km/h", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Elevation: +${gpsTracker.elevationGainMeters.toInt()}m", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            }

            // Quick Simulation & Permissions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        isSimulating = !isSimulating
                        if (!isActive && isSimulating) isActive = true
                    }
                ) {
                    Text(
                        text = if (isSimulating) "⏹️ Stop Simulation" else "👟 Test Walk Simulation",
                        color = if (isSimulating) AmberAccent else CyanAccent,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                TextButton(onClick = onRequestPermissions) {
                    Text(
                        text = "🔒 Sensor Permissions",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Bottom Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isActive && elapsedSeconds == 0) {
                    Button(
                        onClick = {
                            isActive = true
                            stepTracker?.startListening()
                            gpsTracker.startTracking()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Outdoor Walk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Button(
                        onClick = { isActive = !isActive },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isActive) "Pause" else "Resume")
                    }

                    Button(
                        onClick = {
                            isActive = false
                            isSimulating = false
                            if (sessionSteps > 0) {
                                healthState.addExerciseSession(
                                    ExerciseSession(
                                        id = System.currentTimeMillis().toString(),
                                        category = ExerciseCategory.WALKING,
                                        durationMinutes = (elapsedSeconds / 60).coerceAtLeast(1),
                                        distanceKm = distanceKm,
                                        steps = sessionSteps,
                                        caloriesBurned = calories,
                                        timestamp = "Just now",
                                        dateLabel = "Today"
                                    )
                                )
                            }
                            showCompletedDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Finish & Save")
                    }
                }
            }
        }
    }

    // Sensitivity Dialog
    if (showSensitivityDialog) {
        AlertDialog(
            onDismissRequest = { showSensitivityDialog = false },
            title = { Text(text = "Sensor Sensitivity & Placement", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Adjust motion sensitivity depending on how you carry your phone:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    StepSensitivity.entries.forEach { sensitivity ->
                        val isSelected = stepTracker?.sensitivity == sensitivity
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    stepTracker?.sensitivity = sensitivity
                                    showSensitivityDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelected) "●" else "○",
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = sensitivity.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSensitivityDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Completed Walk Dialog
    if (showCompletedDialog) {
        AlertDialog(
            onDismissRequest = {
                showCompletedDialog = false
                onNavigateBack()
            },
            title = { Text(text = "Walk Session Saved! 🎉", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "You walked $sessionSteps steps (${String.format("%.2f km", distanceKm)}), ${routePoints.size} GPS waypoints, and burned $calories kcal."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletedDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("View Summary")
                }
            }
        )
    }
}

@Composable
private fun WalkStatColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}
