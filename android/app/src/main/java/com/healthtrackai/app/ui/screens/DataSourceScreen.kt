package com.healthtrackai.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.healthconnect.MetricSourceInfo
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary

@Composable
fun DataSourceScreen(
    healthState: HealthStateHolder,
    onNavigateBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sources = healthState.connectedDataSources

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
                        text = "Connected Health Sources",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Data origin & pipeline transparency",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Explanation Header Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "HealthTrack AI automatically reads verified metrics from Android Health Connect. Below is the active origin for each metric category on your device.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // List of Data Sources
            if (sources.isNotEmpty()) {
                items(sources) { source ->
                    DataSourceCard(source = source)
                }
            } else {
                // Fallback list of default categories
                item {
                    DefaultDataSourceList(healthState = healthState)
                }
            }

            // Privacy & Permissions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cable,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Health Connect Permissions",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "You can manage, revoke, or adjust granular permissions for each data type in Android Health Connect settings at any time.",
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
private fun DataSourceCard(source: MetricSourceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(text = source.iconEmoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = source.metricName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = source.sourceApp,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (source.isAvailable) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (source.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = source.note,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = CircleShape,
                color = if (source.isAvailable) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(modifier = Modifier.padding(6.dp)) {
                    Icon(
                        imageVector = if (source.isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (source.isAvailable) EmeraldPrimary else Color(0xFFFFA726),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultDataSourceList(healthState: HealthStateHolder) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DataSourceCard(
            MetricSourceInfo(
                metricName = "Steps & Distance",
                isAvailable = healthState.currentSteps > 0,
                sourceApp = "Phone / Health Connect",
                lastSyncTime = "Synced today",
                iconEmoji = "🚶",
                note = "Hardware pedometer & synced companion devices."
            )
        )
        DataSourceCard(
            MetricSourceInfo(
                metricName = "Sleep Duration",
                isAvailable = healthState.sleepHours > 0f,
                sourceApp = if (healthState.sleepHours > 0f) "Connected health source" else "No connected sleep tracker",
                lastSyncTime = "Synced today",
                iconEmoji = "😴",
                note = "Automatically captured sleep intervals."
            )
        )
        DataSourceCard(
            MetricSourceInfo(
                metricName = "Heart Rate",
                isAvailable = healthState.latestHeartRateSummary != null,
                sourceApp = if (healthState.latestHeartRateSummary != null) "Wearable / Pulse Sensor" else "No wearable connected",
                lastSyncTime = "Synced today",
                iconEmoji = "❤️",
                note = "Continuous pulse & resting BPM."
            )
        )
        DataSourceCard(
            MetricSourceInfo(
                metricName = "Hydration",
                isAvailable = healthState.isHydrationSourceConnected,
                sourceApp = if (healthState.isHydrationSourceConnected) "Connected Health Source" else "⚠ No connected source",
                lastSyncTime = "Unavailable",
                iconEmoji = "💧",
                note = "Water intake apps or BLE smart bottles."
            )
        )
    }
}
