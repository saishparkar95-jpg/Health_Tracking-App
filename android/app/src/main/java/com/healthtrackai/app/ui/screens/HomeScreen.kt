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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
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
import com.healthtrackai.app.ui.components.DailyActivityCard
import com.healthtrackai.app.ui.components.DailyChallengeCard
import com.healthtrackai.app.ui.components.HealthScoreHeroCard
import com.healthtrackai.app.ui.components.HeartRateLoggerDialog
import com.healthtrackai.app.ui.components.MetricGridSection
import com.healthtrackai.app.ui.components.SleepLoggerDialog
import com.healthtrackai.app.ui.components.StepLoggerDialog
import com.healthtrackai.app.ui.components.TodaysPlanCard
import com.healthtrackai.app.ui.components.WaterLoggerDialog
import com.healthtrackai.app.ui.components.WeekDayStep
import com.healthtrackai.app.ui.components.WeeklyActivityCard
import com.healthtrackai.app.ui.components.WeightLoggerDialog
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.PurpleAccent
import com.healthtrackai.app.ui.theme.RoseAccent
import com.healthtrackai.app.data.sensors.StepSensorTracker
import androidx.compose.material.icons.filled.Sensors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.healthtrackai.app.ui.components.ReportExportDialog
import com.healthtrackai.app.ui.components.VoiceLoggerDialog
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.CameraAlt

@Composable
fun HomeScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    stepTracker: StepSensorTracker? = null,
    onRequestPermissions: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTrack: (String) -> Unit = {},
    onNavigateToWalkSession: () -> Unit = {},
    onNavigateToHeartRateScanner: () -> Unit = {},
    onNavigateToChallenges: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dialog visibility states
    var showStepDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showHeartDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sticky Header / Greeting Top Section
            HomeHeaderSection(
                userName = healthState.user.name,
                isGuestTrial = healthState.user.isGuestTrial,
                onSettingsClick = onNavigateToSettings
            )

            // Main Scrollable Dashboard Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Quick Metric Tap Bar
                item {
                    QuickMetricSelectorRow(
                        onSelect = { metric ->
                            when (metric) {
                                "steps" -> showStepDialog = true
                                "water" -> showWaterDialog = true
                                "sleep" -> showSleepDialog = true
                                "heart" -> showHeartDialog = true
                                "weight" -> showWeightDialog = true
                                "voice" -> showVoiceDialog = true
                                "report" -> showReportDialog = true
                                "scanner" -> onNavigateToHeartRateScanner()
                            }
                        }
                    )
                }

            // 2. Large Circular Health Score Hero Card (Score 0-100, delta indicator, "Why is my score changing?")
            item {
                HealthScoreHeroCard(healthState = healthState)
            }

            // 3. "Your Plan For Today" Interactive Checklist Card
            item {
                TodaysPlanCard(
                    healthState = healthState,
                    onNavigateToTrack = onNavigateToTrack
                )
            }

            // 3.5 Live Motion Sensor & Pedometer Status Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (healthState.isSensorActive) EmeraldPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onNavigateToWalkSession)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Direct Step Counter",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldPrimary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = if (healthState.sensorCadenceSpm > 0) {
                                        "🟢 Tracking motion • ${healthState.sensorCadenceSpm} SPM"
                                    } else {
                                        "🟢 Hardware sensor active • Ready to walk"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldPrimary
                        ) {
                            Text(
                                text = "Start Walk ▶",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 4. Daily Core Activity Card
            item {
                DailyActivityCard(
                    currentSteps = healthState.currentSteps,
                    stepGoal = healthState.stepGoal,
                    distanceKm = healthState.distanceKm,
                    caloriesBurned = healthState.caloriesBurned,
                    activeMinutes = healthState.activeMinutes,
                    onClick = { showStepDialog = true }
                )
            }

            // 5. Grid of Vitals & Metric Cards (Water, Sleep, Heart, Weight)
            item {
                MetricGridSection(
                    waterCurrentMl = healthState.currentWaterMl,
                    waterGoalMl = healthState.waterGoalMl,
                    sleepDurationFormatted = healthState.sleepDurationFormatted,
                    sleepProgress = healthState.sleepProgress,
                    heartRateBpm = healthState.heartRateBpm,
                    weightKg = healthState.currentWeightKg.toString(),
                    onWaterClick = { showWaterDialog = true },
                    onSleepClick = { showSleepDialog = true },
                    onHeartRateClick = { showHeartDialog = true },
                    onWeightClick = { showWeightDialog = true }
                )
            }

            // 6. Daily Challenge & Streak Card
            item {
                DailyChallengeCard(
                    healthState = healthState,
                    onNavigateToChallenges = onNavigateToChallenges
                )
            }

            // 7. Weekly Activity Trend Visualizer
            item {
                WeeklyActivityCard(
                    weeklySteps = healthState.weeklyLogs.map { log ->
                        WeekDayStep(day = log.day, steps = log.steps, isToday = log.isToday)
                    },
                    stepGoal = healthState.stepGoal
                )
            }
        }
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

    if (showHeartDialog) {
        HeartRateLoggerDialog(
            currentBpm = healthState.heartRateBpm,
            onDismiss = { showHeartDialog = false },
            onSaveHeartRate = { bpm -> healthState.setHeartRate(bpm) }
        )
    }

    if (showWeightDialog) {
        WeightLoggerDialog(
            currentWeightKg = healthState.currentWeightKg,
            onDismiss = { showWeightDialog = false },
            onSaveWeight = { kg -> healthState.setWeight(kg) }
        )
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

        // Floating Voice AI Assistant Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .clickable { showVoiceDialog = true },
            shape = CircleShape,
            color = EmeraldPrimary,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice AI Logger",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun HomeHeaderSection(
    userName: String,
    isGuestTrial: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = remember {
        val formatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        formatter.format(Date())
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Good Morning, ${userName.ifBlank { "Alex" }} 👋",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    if (isGuestTrial) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Trial",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            // Settings Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSettingsClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickMetricSelectorRow(
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        QuickMetricItem("voice", "Voice AI", Icons.Default.Mic, CyanAccent),
        QuickMetricItem("scanner", "Pulse Scan", Icons.Default.Favorite, RoseAccent),
        QuickMetricItem("report", "Report", Icons.Default.Description, EmeraldPrimary),
        QuickMetricItem("steps", "Steps", Icons.Default.DirectionsWalk, EmeraldPrimary),
        QuickMetricItem("water", "Water", Icons.Default.WaterDrop, CyanAccent),
        QuickMetricItem("sleep", "Sleep", Icons.Default.Bedtime, PurpleAccent),
        QuickMetricItem("weight", "Weight", Icons.Default.MonitorWeight, AmberAccent)
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(item.id) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(item.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = item.color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

data class QuickMetricItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)
