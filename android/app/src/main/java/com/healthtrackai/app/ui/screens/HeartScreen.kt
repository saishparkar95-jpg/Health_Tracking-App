package com.healthtrackai.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.RoseAccent

@Composable
fun HeartScreen(
    healthState: HealthStateHolder,
    onNavigateBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hrSummary = healthState.latestHeartRateSummary
    val latestBpm = hrSummary?.latestBpm ?: healthState.heartRateBpm.toLongOrNull()
    val minBpm = hrSummary?.minBpm
    val maxBpm = hrSummary?.maxBpm
    val restingBpm = hrSummary?.restingBpm
    val past7Days = healthState.historical7Days

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
                        text = "Heart Rate & Pulse",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-Time Health Connect Stream",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = RoseAccent
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
            // 1. Current Heart Rate Hero Card
            item {
                CurrentHeartRateHeroCard(
                    latestBpm = latestBpm,
                    sourceName = hrSummary?.sourceName ?: "Health Connect"
                )
            }

            // 2. Daily Pulse Range (Min, Resting, Max)
            item {
                HeartMetricsRangeCard(
                    minBpm = minBpm,
                    restingBpm = restingBpm,
                    maxBpm = maxBpm
                )
            }

            // 3. Weekly Heart Rate Trend Chart
            item {
                WeeklyHeartTrendCard(past7Days = past7Days)
            }

            // 4. Workout Heart Rate & Cardiovascular Zones
            item {
                CardiovascularZonesCard(latestBpm = latestBpm)
            }

            // 5. Medical Disclaimer Card
            item {
                HeartDisclaimerCard()
            }
        }
    }
}

@Composable
private fun CurrentHeartRateHeroCard(latestBpm: Long?, sourceName: String) {
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
                            RoseAccent.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RoseAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = RoseAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Latest Heart Rate",
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
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (latestBpm != null) "$latestBpm" else "Not available",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (latestBpm != null) 42.sp else 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (latestBpm != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BPM",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = RoseAccent
                                    ),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                        Text(
                            text = if (latestBpm != null) "Normal resting range: 60 - 100 BPM" else "Connect a wearable sensor to view live pulse",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (latestBpm != null) {
                        val status = when {
                            latestBpm in 50..85 -> "Resting Flow"
                            latestBpm in 86..120 -> "Elevated / Active"
                            else -> "High Zone"
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = RoseAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RoseAccent
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartMetricsRangeCard(minBpm: Long?, restingBpm: Long?, maxBpm: Long?) {
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
            // Min BPM
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DAILY MIN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (minBpm != null) "$minBpm" else "—",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                )
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            // Resting BPM
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "RESTING HR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (restingBpm != null) "$restingBpm" else "—",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = RoseAccent
                    )
                )
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            // Max BPM
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DAILY MAX",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (maxBpm != null) "$maxBpm" else "—",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF7043)
                    )
                )
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklyHeartTrendCard(past7Days: List<com.healthtrackai.app.data.healthconnect.DailyHealthRecord>) {
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
                    text = "7-Day Heart Rate Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Health Connect Daily",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = RoseAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            if (past7Days.isNotEmpty() && past7Days.any { it.heartRateSummary?.latestBpm != null }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    past7Days.forEach { record ->
                        val bpm = record.heartRateSummary?.latestBpm ?: 0L
                        val heightFraction = (bpm.toFloat() / 150f).coerceIn(0.1f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (bpm > 0) "$bpm" else "—",
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
                                        if (record.isToday) Brush.verticalGradient(listOf(RoseAccent, CyanAccent))
                                        else Brush.verticalGradient(
                                            listOf(
                                                RoseAccent.copy(alpha = 0.7f),
                                                RoseAccent.copy(alpha = 0.3f)
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
                                    color = if (record.isToday) RoseAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            } else {
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
                            tint = RoseAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Historical heart rate records will populate automatically when paired with a supported smartwatch or sensor.",
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
private fun CardiovascularZonesCard(latestBpm: Long?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cardiovascular Zones Guide",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            ZoneRow("Zone 1: Rest & Recovery", "50 - 65% max HR (Resting / Slow Walk)", CyanAccent)
            ZoneRow("Zone 2: Fat Burn / Aerobic", "65 - 75% max HR (Brisk Walk / Easy Jog)", Color(0xFF66BB6A))
            ZoneRow("Zone 3: Cardio Endurance", "75 - 85% max HR (Running / Cycling)", Color(0xFFFFA726))
            ZoneRow("Zone 4: Peak Performance", "85 - 95% max HR (HIIT / Sprints)", RoseAccent)
        }
    }
}

@Composable
private fun ZoneRow(title: String, subtitle: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeartDisclaimerCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "HealthTrack AI does not provide medical diagnosis. Heart rate readings are obtained via Android Health Connect for fitness and wellness tracking.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
