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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.models.ExerciseCategory
import com.healthtrackai.app.data.models.ExerciseSession
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.RoseAccent
import kotlinx.coroutines.delay

@Composable
fun ExerciseScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.WALKING) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var completedSessionSummary by remember { mutableStateOf<ExerciseSession?>(null) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val calories = (elapsedSeconds / 60f * selectedCategory.defaultCalPerMin).toInt()
    val distance = if (selectedCategory == ExerciseCategory.RUNNING) (elapsedSeconds / 60f * 0.16f)
                   else if (selectedCategory == ExerciseCategory.CYCLING) (elapsedSeconds / 60f * 0.35f)
                   else (elapsedSeconds / 60f * 0.08f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Exercise & Workout Tracker", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = "Track workouts and active calories", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Category Selector
            item {
                Text(
                    text = "SELECT WORKOUT CATEGORY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ExerciseCategory.values().size) { idx ->
                        val cat = ExerciseCategory.values()[idx]
                        val isSelected = selectedCategory == cat
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { if (!isTimerRunning) selectedCategory = cat },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cat.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Active Workout Timer Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RoseAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${selectedCategory.iconEmoji} ${selectedCategory.displayName}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RoseAccent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Timer Display
                        val mins = elapsedSeconds / 60
                        val secs = elapsedSeconds % 60
                        val timeStr = String.format("%02d:%02d", mins, secs)

                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "ACTIVE DURATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Live Metrics (Calories & Distance)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$calories kcal", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                Text(text = "Est. Burned", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = String.format("%.2f km", distance), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                Text(text = "Est. Distance", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Workout Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!isTimerRunning && elapsedSeconds == 0) {
                                Button(
                                    onClick = { isTimerRunning = true },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Workout", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            } else {
                                Button(
                                    onClick = { isTimerRunning = !isTimerRunning },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary)
                                ) {
                                    Icon(imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isTimerRunning) "Pause" else "Resume")
                                }

                                Button(
                                    onClick = {
                                        isTimerRunning = false
                                        val session = ExerciseSession(
                                            id = System.currentTimeMillis().toString(),
                                            category = selectedCategory,
                                            durationMinutes = (elapsedSeconds / 60).coerceAtLeast(1),
                                            distanceKm = distance,
                                            caloriesBurned = calories.coerceAtLeast(10),
                                            timestamp = "Just now",
                                            dateLabel = "Today"
                                        )
                                        healthState.addExerciseSession(session)
                                        completedSessionSummary = session
                                        showCompletionDialog = true
                                        elapsedSeconds = 0
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Finish")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Exercise History Log
            item {
                Text(
                    text = "RECENT WORKOUT SESSIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (healthState.exerciseHistory.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No exercise sessions logged yet. Start a workout above!",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                } else {
                    healthState.exerciseHistory.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.category.iconEmoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.category.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "${item.dateLabel} • ${item.timestamp}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "${item.durationMinutes} mins", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "${item.caloriesBurned} kcal", style = MaterialTheme.typography.labelSmall.copy(color = RoseAccent, fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Workout Completion Celebration Modal
    if (showCompletionDialog && completedSessionSummary != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            title = { Text(text = "Workout Completed! 🎉", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Great job finishing your ${completedSessionSummary?.category?.displayName} workout session!")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "⏱️ Duration: ${completedSessionSummary?.durationMinutes} minutes")
                    Text(text = "🔥 Calories: ${completedSessionSummary?.caloriesBurned} kcal")
                    if ((completedSessionSummary?.distanceKm ?: 0f) > 0f) {
                        Text(text = "📍 Distance: ${String.format("%.2f km", completedSessionSummary?.distanceKm)}")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCompletionDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Awesome")
                }
            }
        )
    }
}
