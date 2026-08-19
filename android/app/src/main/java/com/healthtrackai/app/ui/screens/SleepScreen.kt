package com.healthtrackai.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.healthconnect.SleepSessionData
import com.healthtrackai.app.data.healthconnect.SleepStageType
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.PurpleAccent

@Composable
fun SleepScreen(
    healthState: HealthStateHolder,
    onNavigateBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sleepSession = healthState.latestSleepSession
    val sleepHours = healthState.sleepHours
    val sleepGoal = healthState.sleepGoalHours
    val past7Days = healthState.historical7Days

    val avgSleep = if (past7Days.isNotEmpty()) {
        val list = past7Days.mapNotNull { it.sleepMinutes?.let { m -> m.toFloat() / 60f } }
        if (list.isNotEmpty()) list.average().toFloat() else sleepHours
    } else sleepHours

    val bedtime = sleepSession?.bedtimeFormatted ?: healthState.sleepBedtime
    val wakeTime = sleepSession?.wakeTimeFormatted ?: healthState.sleepWakeTime

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Sleep & Recovery",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Synced via Health Connect",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = PurpleAccent
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
            // 1. Sleep Main Duration Hero Card
            item {
                SleepDurationHeroCard(
                    sleepHours = sleepHours,
                    sleepGoal = sleepGoal,
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    sourceName = sleepSession?.sourceName ?: "Health Connect"
                )
            }

            // 2. Bedtime & Wake Time Details Card
            item {
                SleepScheduleCard(
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    sleepHours = sleepHours
                )
            }

            // 3. Sleep Stages Breakdown Card (if recorded)
            item {
                SleepStagesSection(session = sleepSession)
            }

            // 4. Weekly Sleep Duration Bar Chart
            item {
                WeeklySleepChartCard(
                    past7Days = past7Days,
                    avgSleep = avgSleep,
                    sleepGoal = sleepGoal
                )
            }

            // 5. Sleep Consistency & Wellness Insight
            item {
                SleepConsistencyCard(
                    avgSleep = avgSleep,
                    sleepHours = sleepHours
                )
            }
        }
    }
}

@Composable
private fun SleepDurationHeroCard(
    sleepHours: Float,
    sleepGoal: Float,
    bedtime: String,
    wakeTime: String,
    sourceName: String
) {
    val progress = (sleepHours / sleepGoal.coerceAtLeast(1f)).coerceIn(0f, 1.2f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "SleepProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PurpleAccent.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PurpleAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Last Night's Sleep",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = sourceName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        val hours = sleepHours.toInt()
                        val minutes = ((sleepHours - hours) * 60).toInt()
                        val durationText = if (sleepHours > 0f) "${hours}h ${minutes}m" else "Not recorded"

                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Target: ${sleepGoal.toInt()} hours (${(animatedProgress * 100).toInt()}% achieved)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sleep Quality Badge
                    val qualityTier = when {
                        sleepHours >= 7.5f -> "Optimal Rest"
                        sleepHours >= 6.5f -> "Good Sleep"
                        sleepHours > 0f -> "Low Sleep"
                        else -> "Unavailable"
                    }
                    val qualityColor = when {
                        sleepHours >= 7.5f -> CyanAccent
                        sleepHours >= 6.5f -> PurpleAccent
                        else -> Color(0xFFFFA726)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = qualityColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = qualityTier,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = qualityColor
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceAtMost(1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PurpleAccent, CyanAccent)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepScheduleCard(bedtime: String, wakeTime: String, sleepHours: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bedtime
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BEDTIME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (sleepHours > 0f) bedtime else "Not available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            // Wake Time
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WAKE TIME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (sleepHours > 0f) wakeTime else "Not available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun SleepStagesSection(session: SleepSessionData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                    text = "Sleep Stages",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (session?.hasStages == true) "Health Connect Stages" else "Stages Unavailable",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session != null && session.hasStages) {
                val total = session.totalMinutes.coerceAtLeast(1)

                // Visual Stage Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (session.deepSleepMinutes > 0) {
                        Box(
                            modifier = Modifier
                                .weight((session.deepSleepMinutes.toFloat() / total).coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .background(Color(SleepStageType.DEEP.colorHex))
                        )
                    }
                    if (session.remSleepMinutes > 0) {
                        Box(
                            modifier = Modifier
                                .weight((session.remSleepMinutes.toFloat() / total).coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .background(Color(SleepStageType.REM.colorHex))
                        )
                    }
                    if (session.lightSleepMinutes > 0) {
                        Box(
                            modifier = Modifier
                                .weight((session.lightSleepMinutes.toFloat() / total).coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .background(Color(SleepStageType.LIGHT.colorHex))
                        )
                    }
                    if (session.awakeMinutes > 0) {
                        Box(
                            modifier = Modifier
                                .weight((session.awakeMinutes.toFloat() / total).coerceAtLeast(0.05f))
                                .fillMaxHeight()
                                .background(Color(SleepStageType.AWAKE.colorHex))
                        )
                    }
                }

                // Stage details pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StageInfoPill("Deep", "${session.deepSleepMinutes}m", Color(SleepStageType.DEEP.colorHex))
                    StageInfoPill("REM", "${session.remSleepMinutes}m", Color(SleepStageType.REM.colorHex))
                    StageInfoPill("Light", "${session.lightSleepMinutes}m", Color(SleepStageType.LIGHT.colorHex))
                    StageInfoPill("Awake", "${session.awakeMinutes}m", Color(SleepStageType.AWAKE.colorHex))
                }
            } else {
                // Informative message when sleep stages are not provided by hardware
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sleep-stage breakdown (Deep, Light, REM) requires a compatible wearable synced with Health Connect. Total duration is tracked automatically.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageInfoPill(label: String, duration: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = duration,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WeeklySleepChartCard(
    past7Days: List<com.healthtrackai.app.data.healthconnect.DailyHealthRecord>,
    avgSleep: Float,
    sleepGoal: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                    text = "Weekly Sleep Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Avg: ${String.format("%.1f", avgSleep)} hrs/night",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleAccent
                    )
                )
            }

            // 7-day Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val days = if (past7Days.isNotEmpty()) past7Days else emptyList()
                val maxBarHours = 10f

                if (days.isNotEmpty()) {
                    days.forEach { record ->
                        val hours = record.sleepMinutes?.let { it.toFloat() / 60f } ?: 0f
                        val heightFraction = (hours / maxBarHours).coerceIn(0.08f, 1f)
                        val isToday = record.isToday

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (hours > 0) String.format("%.1f", hours) else "-",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isToday) Brush.verticalGradient(listOf(CyanAccent, PurpleAccent))
                                        else Brush.verticalGradient(
                                            listOf(
                                                PurpleAccent.copy(alpha = 0.7f),
                                                PurpleAccent.copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = record.dayOfWeek,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) PurpleAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Historical sleep records will appear here as Health Connect syncs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepConsistencyCard(avgSleep: Float, sleepHours: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = PurpleAccent.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Sleep Consistency Insight",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val insight = when {
                    avgSleep >= 7.5f -> "Your sleep schedule has high restorative quality and steady consistency."
                    avgSleep >= 6.5f -> "Good sleep rhythm. Keeping bedtime within a 30-minute window will boost REM recovery."
                    avgSleep > 0f -> "Your sleep duration has been below optimal. Try a relaxing wind-down routine 1 hour before bed."
                    else -> "Sync Health Connect with a sleep tracker to view personalized recovery analytics."
                }
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
