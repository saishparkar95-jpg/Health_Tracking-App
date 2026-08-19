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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.healthconnect.ExerciseRecordItem
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.RoseAccent

@Composable
fun ActivityScreen(
    healthState: HealthStateHolder,
    onNavigateBack: (() -> Unit)? = null,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf("Weekly") } // "Weekly" or "Monthly"
    val past7Days = healthState.historical7Days
    val workouts = healthState.healthConnectWorkouts

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column {
                    Text(
                        text = "Activity & Movement",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-Time Health Connect Sync",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = EmeraldPrimary
                    )
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Primary Steps Hero Ring Card
            item {
                ActivityHeroRingCard(
                    steps = healthState.currentSteps,
                    goal = healthState.stepGoal,
                    progress = healthState.stepProgress
                )
            }

            // 2. Metrics Breakdown Row (Distance, Calories, Active Time)
            item {
                ActivityMetricsRow(
                    distanceKm = healthState.distanceKm,
                    calories = healthState.caloriesBurned,
                    activeMinutes = healthState.activeMinutes
                )
            }

            // 3. Timeframe Toggle & Chart (Weekly / Monthly)
            item {
                ActivityChartSection(
                    past7Days = past7Days,
                    stepGoal = healthState.stepGoal,
                    selectedTimeframe = selectedTimeframe,
                    onTimeframeChange = { selectedTimeframe = it }
                )
            }

            // 4. Exercise Sessions from Health Connect
            item {
                ExerciseSessionsSection(workouts = workouts)
            }

            // 5. Activity Trends Insight Card
            item {
                ActivityTrendInsightCard(
                    steps = healthState.currentSteps,
                    past7Days = past7Days
                )
            }
        }
    }
}

@Composable
private fun ActivityHeroRingCard(
    steps: Int,
    goal: Int,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "HeroStepProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TODAY'S MOVEMENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%,d", steps),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target: ${String.format("%,d", goal)} steps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Circular Progress Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(90.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 9.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress.coerceAtMost(1f) },
                        modifier = Modifier.size(90.dp),
                        color = EmeraldPrimary,
                        strokeWidth = 9.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityMetricsRow(
    distanceKm: Float,
    calories: Int,
    activeMinutes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActivityMiniStatCard(
            title = "Distance",
            value = String.format("%.2f km", distanceKm),
            icon = Icons.Default.Straighten,
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
        ActivityMiniStatCard(
            title = "Calories",
            value = "$calories kcal",
            icon = Icons.Default.LocalFireDepartment,
            color = RoseAccent,
            modifier = Modifier.weight(1f)
        )
        ActivityMiniStatCard(
            title = "Active Time",
            value = "$activeMinutes min",
            icon = Icons.Default.Timer,
            color = EmeraldPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActivityMiniStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActivityChartSection(
    past7Days: List<com.healthtrackai.app.data.healthconnect.DailyHealthRecord>,
    stepGoal: Int,
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTimeframe == "Weekly") "Weekly Step History" else "Monthly Activity Overview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Toggle Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("Weekly", "Monthly").forEach { timeframe ->
                        val isSelected = selectedTimeframe == timeframe
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.clickable { onTimeframeChange(timeframe) }
                        ) {
                            Text(
                                text = timeframe,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (past7Days.isNotEmpty()) {
                    past7Days.forEach { record ->
                        val steps = record.steps?.toInt() ?: 0
                        val heightFraction = (steps.toFloat() / (stepGoal * 1.2f).coerceAtLeast(1000f)).coerceIn(0.08f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (steps > 0) "${steps / 1000}k" else "0",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (record.isToday) Brush.verticalGradient(listOf(CyanAccent, EmeraldPrimary))
                                        else Brush.verticalGradient(
                                            listOf(
                                                EmeraldPrimary.copy(alpha = 0.7f),
                                                EmeraldPrimary.copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = record.dayOfWeek,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (record.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (record.isToday) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSessionsSection(workouts: List<ExerciseRecordItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tracked Workout Sessions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Health Connect",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldPrimary,
                        fontSize = 11.sp
                    )
                )
            }

            if (workouts.isNotEmpty()) {
                workouts.forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = session.iconEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${session.durationMinutes} minutes • ${session.sourceName}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (session.caloriesBurnedKcal != null) {
                            Text(
                                text = "${session.caloriesBurnedKcal.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RoseAccent
                                )
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No exercise sessions recorded today. Workouts recorded by compatible fitness apps will automatically synchronize.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActivityTrendInsightCard(
    steps: Int,
    past7Days: List<com.healthtrackai.app.data.healthconnect.DailyHealthRecord>
) {
    val avgSteps = if (past7Days.isNotEmpty()) {
        past7Days.mapNotNull { it.steps?.toInt() }.average().toInt()
    } else 7000

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = EmeraldPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Activity Trend Analysis",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val trendText = if (steps >= avgSteps) {
                    "Your activity today is higher than your weekly average of ${String.format("%,d", avgSteps)} steps."
                } else {
                    "Your step count is currently below your weekly average (${String.format("%,d", avgSteps)} steps). A 15m walk will boost your score!"
                }
                Text(
                    text = trendText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
