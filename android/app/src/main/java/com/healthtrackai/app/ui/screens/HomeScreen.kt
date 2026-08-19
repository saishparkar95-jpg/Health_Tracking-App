package com.healthtrackai.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.healthconnect.HealthConnectPermissionState
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.data.services.HealthScoreBreakdown
import com.healthtrackai.app.data.services.HealthScoreEngine
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.PurpleAccent
import com.healthtrackai.app.ui.theme.RoseAccent
import java.time.LocalTime

@Composable
fun HomeScreen(
    healthState: HealthStateHolder,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSleep: () -> Unit = {},
    onNavigateToHeart: () -> Unit = {},
    onNavigateToHydration: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onRefreshHealthConnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "GOOD MORNING 👋"
        in 12..16 -> "GOOD AFTERNOON ☀️"
        in 17..21 -> "GOOD EVENING 🌆"
        else -> "HELLO THERE 🌙"
    }

    val healthScore = healthState.currentHealthScoreResult ?: HealthScoreEngine.calculateScore(
        healthState,
        healthState.todayHealthRecord,
        healthState.historical7Days
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header / Greeting Top Section
        HomeHeaderBar(
            greeting = greeting,
            userName = healthState.user.name,
            lastSyncTime = healthState.lastHealthConnectSyncTime,
            isSyncing = healthState.isSyncingHealthConnect,
            onSettingsClick = onNavigateToSettings,
            onRefreshClick = onRefreshHealthConnect
        )

        // Main Scrollable Dashboard Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Permission Notice Banner (if Health Connect permissions not granted)
            if (healthState.healthConnectPermissionState != HealthConnectPermissionState.ALL_GRANTED) {
                item {
                    PermissionNoticeBanner(
                        onGrantClick = onNavigateToPermissions
                    )
                }
            }

            // 1. Health Score Hero Card
            item {
                HealthScoreHeroBanner(
                    score = healthScore,
                    onClick = onNavigateToInsights
                )
            }

            // 2. Main 2x2 Metric Grid (Steps, Distance, Calories, Active Minutes)
            item {
                DashboardMetricsGrid(
                    healthState = healthState,
                    onNavigateToActivity = onNavigateToActivity,
                    onNavigateToHeart = onNavigateToHeart,
                    onNavigateToSleep = onNavigateToSleep,
                    onNavigateToHydration = onNavigateToHydration
                )
            }

            // 3. Workout & Activity Summary Card
            item {
                WorkoutSummaryCard(
                    healthState = healthState,
                    onViewActivityClick = onNavigateToActivity
                )
            }

            // 4. AI Daily Summary Snippet Card
            item {
                AiDailySummarySnippetCard(
                    healthState = healthState,
                    healthScore = healthScore,
                    onViewInsightsClick = onNavigateToInsights
                )
            }
        }
    }
}

@Composable
private fun HomeHeaderBar(
    greeting: String,
    userName: String,
    lastSyncTime: String,
    isSyncing: Boolean,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                ),
                color = EmeraldPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSyncing) CyanAccent else EmeraldPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSyncing) "Syncing Health Connect..." else "Health Connect: $lastSyncTime",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = EmeraldPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionNoticeBanner(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGrantClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Cable,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Connect Health Data",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Grant Health Connect permissions to automatically sync your steps, sleep, & heart rate.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldPrimary
            ) {
                Text(
                    text = "Grant",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthScoreHeroBanner(
    score: HealthScoreBreakdown,
    onClick: () -> Unit
) {
    val tierColor = when (score.ratingTier) {
        "Excellent" -> EmeraldPrimary
        "Good" -> CyanAccent
        else -> Color(0xFFFFA726)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.18f),
                            CyanAccent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YOUR HEALTH SCORE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${score.overallScore}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 44.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " / 100",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // Rating badge
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = tierColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = score.ratingTier,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tierColor
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val deltaText = if (score.deltaFromYesterday >= 0) "+${score.deltaFromYesterday} pts vs yday" else "${score.deltaFromYesterday} pts vs yday"
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (score.deltaFromYesterday >= 0) EmeraldPrimary else Color(0xFFFF7043)
                        )
                    }
                }

                // Explanation
                Text(
                    text = score.whyChangedExplanation,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardMetricsGrid(
    healthState: HealthStateHolder,
    onNavigateToActivity: () -> Unit,
    onNavigateToHeart: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToHydration: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Steps Card
            MetricSquareCard(
                title = "Steps",
                value = String.format("%,d", healthState.currentSteps),
                subtext = "Goal: ${String.format("%,d", healthState.stepGoal)}",
                icon = Icons.Default.DirectionsWalk,
                color = EmeraldPrimary,
                progress = healthState.stepProgress,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToActivity
            )

            // Sleep Card
            MetricSquareCard(
                title = "Sleep",
                value = healthState.sleepDurationFormatted,
                subtext = "Restorative Rest",
                icon = Icons.Default.Bedtime,
                color = PurpleAccent,
                progress = healthState.sleepProgress,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSleep
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Distance Card
            MetricSquareCard(
                title = "Distance",
                value = String.format("%.1f km", healthState.distanceKm),
                subtext = "Active Movement",
                icon = Icons.Default.DirectionsRun,
                color = CyanAccent,
                progress = (healthState.distanceKm / 8.0f).coerceIn(0f, 1f),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToActivity
            )

            // Calories Card
            MetricSquareCard(
                title = "Calories",
                value = "${healthState.caloriesBurned} kcal",
                subtext = "Active Burn",
                icon = Icons.Default.LocalFireDepartment,
                color = RoseAccent,
                progress = (healthState.caloriesBurned / 600f).coerceIn(0f, 1f),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToActivity
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Heart Rate Card
            val hrVal = if (healthState.heartRateBpm.isNotBlank() && healthState.heartRateBpm != "--") "${healthState.heartRateBpm} BPM" else "Not available"
            MetricSquareCard(
                title = "Heart Rate",
                value = hrVal,
                subtext = "Resting Pulse",
                icon = Icons.Default.Favorite,
                color = RoseAccent,
                progress = null,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToHeart
            )

            // Activity / Active Minutes Card
            MetricSquareCard(
                title = "Activity",
                value = "${healthState.activeMinutes} min",
                subtext = "Goal: ${healthState.activeMinutesGoal}m",
                icon = Icons.Default.BarChart,
                color = CyanAccent,
                progress = (healthState.activeMinutes.toFloat() / healthState.activeMinutesGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToActivity
            )
        }

        // Hydration Card (Full Width)
        val waterVal = if (healthState.isHydrationSourceConnected && healthState.currentWaterMl > 0)
            String.format("%.1f L", healthState.currentWaterMl / 1000f)
        else
            "Not available"

        MetricHorizontalCard(
            title = "Hydration",
            value = waterVal,
            subtext = if (healthState.isHydrationSourceConnected) "Target: 2.5 L" else "No connected health source",
            icon = Icons.Default.WaterDrop,
            color = CyanAccent,
            onClick = onNavigateToHydration
        )
    }
}

@Composable
private fun MetricSquareCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    progress: Float?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(138.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun MetricHorizontalCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutSummaryCard(
    healthState: HealthStateHolder,
    onViewActivityClick: () -> Unit
) {
    val workouts = healthState.healthConnectWorkouts

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewActivityClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Today's Workouts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (workouts.isNotEmpty()) "${workouts.size} Sessions" else "No sessions yet",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            if (workouts.isNotEmpty()) {
                workouts.take(2).forEach { workout ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = workout.iconEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = workout.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${workout.durationMinutes} min • ${workout.sourceName}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Tracked runs, walks, or gym sessions synced via Health Connect will appear here automatically.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiDailySummarySnippetCard(
    healthState: HealthStateHolder,
    healthScore: HealthScoreBreakdown,
    onViewInsightsClick: () -> Unit
) {
    val dailySummary = healthState.todayAiDailySummary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewInsightsClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            PurpleAccent.copy(alpha = 0.12f),
                            CyanAccent.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI DAILY INSIGHT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = PurpleAccent
                            )
                        )
                    }

                    Text(
                        text = "View Insights →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )
                    )
                }

                Text(
                    text = dailySummary?.aiInsight ?: "Your activity level is good today and your sleep duration is close to your recent average.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
