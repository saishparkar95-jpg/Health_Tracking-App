package com.healthtrackai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.data.models.MoodType
import com.healthtrackai.app.ui.components.HeartRateLoggerDialog
import com.healthtrackai.app.ui.components.SleepLoggerDialog
import com.healthtrackai.app.ui.components.StepLoggerDialog
import com.healthtrackai.app.ui.components.WaterLoggerDialog
import com.healthtrackai.app.ui.components.WeightLoggerDialog
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.PurpleAccent
import com.healthtrackai.app.ui.theme.RoseAccent

import com.healthtrackai.app.ui.components.ReportExportDialog
import com.healthtrackai.app.ui.components.VoiceLoggerDialog
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic

@Composable
fun TrackScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    onNavigateToFoodScanner: () -> Unit = {},
    onNavigateToWalkSession: () -> Unit = {},
    onNavigateToHeartRateScanner: () -> Unit = {},
    onNavigateToExercise: () -> Unit = {},
    onNavigateToMood: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showStepDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Quick Track Hub",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Fast one-tap logging & advanced health diagnostic tools",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Water Quick Add Bar
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CyanAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Hydration Intake", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "${healthState.currentWaterMl} / ${healthState.waterGoalMl} ml logged", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                }
                            }

                            Text(
                                text = "💧 +",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showWaterDialog = true }
                                    .padding(4.dp),
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickAddPill("+250 ml", CyanAccent, Modifier.weight(1f)) { healthState.addWater(250) }
                            QuickAddPill("+500 ml", CyanAccent, Modifier.weight(1f)) { healthState.addWater(500) }
                            QuickAddPill("+750 ml", CyanAccent, Modifier.weight(1f)) { healthState.addWater(750) }
                        }
                    }
                }
            }

            // 2. Steps Quick Add Bar
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Daily Walking Steps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "${healthState.currentSteps} / ${healthState.stepGoal} steps", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                }
                            }

                            Text(
                                text = "🚶 +",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showStepDialog = true }
                                    .padding(4.dp),
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickAddPill("+500", EmeraldPrimary, Modifier.weight(1f)) { healthState.addSteps(500) }
                            QuickAddPill("+1,000", EmeraldPrimary, Modifier.weight(1f)) { healthState.addSteps(1000) }
                            QuickAddPill("+2,500", EmeraldPrimary, Modifier.weight(1f)) { healthState.addSteps(2500) }
                        }
                    }
                }
            }

            // 3. Grid of Fast Tracking Action Cards
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Voice AI Logger
                    TrackActionCard(
                        title = "Voice AI Logger",
                        subtitle = "Hands-free Speech Log",
                        icon = Icons.Default.Mic,
                        color = CyanAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { showVoiceDialog = true }
                    )

                    // PPG Heart Rate Scanner
                    TrackActionCard(
                        title = "Pulse Scanner",
                        subtitle = "Camera PPG Sensor",
                        icon = Icons.Default.Favorite,
                        color = RoseAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHeartRateScanner
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Meal Photo Scanner
                    TrackActionCard(
                        title = "Scan Meal (Food AI)",
                        subtitle = "Photo Macro Estimate",
                        icon = Icons.Default.CameraAlt,
                        color = EmeraldPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFoodScanner
                    )

                    // Live GPS Walk
                    TrackActionCard(
                        title = "Live GPS Walk",
                        subtitle = "Outdoor Route Map",
                        icon = Icons.Default.DirectionsWalk,
                        color = EmeraldPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToWalkSession
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Exercise Tracker
                    TrackActionCard(
                        title = "Log Exercise",
                        subtitle = "7 Categories & Timer",
                        icon = Icons.Default.DirectionsRun,
                        color = RoseAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToExercise
                    )

                    // Sleep Rest
                    TrackActionCard(
                        title = "Log Sleep",
                        subtitle = "Bedtime & Quality",
                        icon = Icons.Default.Bedtime,
                        color = PurpleAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { showSleepDialog = true }
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Mood Check-in
                    TrackActionCard(
                        title = "Mood Check-in",
                        subtitle = "Log How You Feel",
                        icon = Icons.Default.Psychology,
                        color = AmberAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMood
                    )

                    // 30-Day Medical Report
                    TrackActionCard(
                        title = "Health Report",
                        subtitle = "Export Doctor Summary",
                        icon = Icons.Default.Description,
                        color = CyanAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { showReportDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showVoiceDialog) {
        VoiceLoggerDialog(
            healthState = healthState,
            onDismiss = { showVoiceDialog = false }
        )
    }

    if (showReportDialog) {
        ReportExportDialog(
            healthState = healthState,
            onDismiss = { showReportDialog = false }
        )
    }

    // Modal Interactive Dialogs
    if (showStepDialog) {
        StepLoggerDialog(
            currentSteps = healthState.currentSteps,
            stepGoal = healthState.stepGoal,
            onDismiss = { showStepDialog = false },
            onAddSteps = { amount -> healthState.addSteps(amount) }
        )
    }

    if (showWaterDialog) {
        WaterLoggerDialog(
            currentMl = healthState.currentWaterMl,
            goalMl = healthState.waterGoalMl,
            onDismiss = { showWaterDialog = false },
            onAddWater = { amount -> healthState.addWater(amount) }
        )
    }

    if (showSleepDialog) {
        SleepLoggerDialog(
            currentHours = healthState.sleepHours,
            goalHours = healthState.sleepGoalHours,
            onDismiss = { showSleepDialog = false },
            onSaveSleep = { hours -> healthState.setSleep(hours) }
        )
    }

    if (showWeightDialog) {
        WeightLoggerDialog(
            currentWeightKg = healthState.currentWeightKg,
            onDismiss = { showWeightDialog = false },
            onSaveWeight = { kg -> healthState.setWeight(kg) }
        )
    }
}

@Composable
private fun QuickAddPill(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            )
        }
    }
}

@Composable
private fun TrackActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}
