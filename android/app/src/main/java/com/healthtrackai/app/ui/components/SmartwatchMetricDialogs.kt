package com.healthtrackai.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldLight
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.PurpleAccent
import com.healthtrackai.app.ui.theme.RoseAccent
import kotlinx.coroutines.delay

/**
 * Smartwatch Concentric Activity Rings (Move, Exercise, Stand)
 */
@Composable
fun SmartwatchActivityRings(
    moveProgress: Float,     // Calories / Move (Rose)
    exerciseProgress: Float, // Steps / Exercise (Emerald)
    standProgress: Float,    // Active / Stand (Cyan)
    modifier: Modifier = Modifier,
    strokeWidth: Float = 22f
) {
    val animatedMove by animateFloatAsState(targetValue = moveProgress.coerceIn(0f, 1f), tween(900), label = "move")
    val animatedExercise by animateFloatAsState(targetValue = exerciseProgress.coerceIn(0f, 1f), tween(900), label = "exercise")
    val animatedStand by animateFloatAsState(targetValue = standProgress.coerceIn(0f, 1f), tween(900), label = "stand")

    Canvas(modifier = modifier) {
        val size = this.size.minDimension
        val center = androidx.compose.ui.geometry.Offset(size / 2, size / 2)

        // Outer Ring (Move / Rose)
        val r1 = (size / 2) - strokeWidth
        drawCircle(
            color = RoseAccent.copy(alpha = 0.15f),
            radius = r1,
            center = center,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = RoseAccent,
            startAngle = -90f,
            sweepAngle = animatedMove * 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - r1, center.y - r1),
            size = androidx.compose.ui.geometry.Size(r1 * 2, r1 * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        // Middle Ring (Exercise / Emerald)
        val r2 = r1 - strokeWidth - 10f
        drawCircle(
            color = EmeraldPrimary.copy(alpha = 0.15f),
            radius = r2,
            center = center,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = EmeraldPrimary,
            startAngle = -90f,
            sweepAngle = animatedExercise * 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - r2, center.y - r2),
            size = androidx.compose.ui.geometry.Size(r2 * 2, r2 * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        // Inner Ring (Stand / Cyan)
        val r3 = r2 - strokeWidth - 10f
        drawCircle(
            color = CyanAccent.copy(alpha = 0.15f),
            radius = r3,
            center = center,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = CyanAccent,
            startAngle = -90f,
            sweepAngle = animatedStand * 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - r3, center.y - r3),
            size = androidx.compose.ui.geometry.Size(r3 * 2, r3 * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}

// ---------------------------------------------------------------------------------
// 1. STEPS LOGGER DIALOG
// ---------------------------------------------------------------------------------
@Composable
fun StepLoggerDialog(
    currentSteps: Int,
    stepGoal: Int,
    onAddSteps: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var customSteps by remember { mutableFloatStateOf(currentSteps.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DirectionsWalk, contentDescription = null, tint = EmeraldPrimary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Log Daily Steps",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Steps Counter Display
                Text(
                    text = "%,d".format(customSteps.toInt()),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
                Text(
                    text = "Goal: %,d steps".format(stepGoal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Step Add Buttons (Smartwatch-style pills)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickPillButton(text = "+500", color = EmeraldPrimary, modifier = Modifier.weight(1f)) {
                        customSteps += 500f
                    }
                    QuickPillButton(text = "+1,000", color = EmeraldPrimary, modifier = Modifier.weight(1f)) {
                        customSteps += 1000f
                    }
                    QuickPillButton(text = "+2,500", color = EmeraldPrimary, modifier = Modifier.weight(1f)) {
                        customSteps += 2500f
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slider
                Slider(
                    value = customSteps,
                    onValueChange = { customSteps = it },
                    valueRange = 0f..20000f,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val diff = customSteps.toInt() - currentSteps
                        onAddSteps(diff)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Steps", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 2. WATER LOGGER DIALOG
// ---------------------------------------------------------------------------------
@Composable
fun WaterLoggerDialog(
    currentMl: Int,
    goalMl: Int,
    onAddWater: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = CyanAccent)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Log Hydration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "$currentMl ml",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = CyanAccent
                )
                Text(
                    text = "Goal: $goalMl ml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Smartwatch Quick Drink Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WaterCupOption(emoji = "🥛", label = "+250 ml", desc = "Small Glass", color = CyanAccent, modifier = Modifier.weight(1f)) {
                        onAddWater(250)
                    }
                    WaterCupOption(emoji = "🧴", label = "+500 ml", desc = "Bottle", color = CyanAccent, modifier = Modifier.weight(1f)) {
                        onAddWater(500)
                    }
                    WaterCupOption(emoji = "🫙", label = "+750 ml", desc = "Flask", color = CyanAccent, modifier = Modifier.weight(1f)) {
                        onAddWater(750)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onAddWater(-currentMl) // Reset
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 3. SLEEP LOGGER DIALOG
// ---------------------------------------------------------------------------------
@Composable
fun SleepLoggerDialog(
    currentHours: Float,
    goalHours: Float,
    onSaveSleep: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var hours by remember { mutableFloatStateOf(currentHours) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PurpleAccent.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurpleAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Bedtime, contentDescription = null, tint = PurpleAccent)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Log Sleep Duration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val hrsInt = hours.toInt()
                val minsInt = ((hours - hrsInt) * 60).toInt()
                Text(
                    text = "${hrsInt}h ${minsInt}m",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = PurpleAccent
                )
                Text(
                    text = "Goal: ${goalHours.toInt()}h 0m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stepper Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { hours = (hours - 0.5f).coerceAtLeast(0f) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "-30m", tint = PurpleAccent)
                    }

                    Text(
                        text = "Adjust ±30m",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { hours = (hours + 0.5f).coerceAtMost(16f) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "+30m", tint = PurpleAccent)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Slider(
                    value = hours,
                    onValueChange = { hours = it },
                    valueRange = 0f..14f,
                    steps = 27,
                    colors = SliderDefaults.colors(
                        thumbColor = PurpleAccent,
                        activeTrackColor = PurpleAccent
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onSaveSleep(hours)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("Save Sleep", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 4. HEART RATE LOGGER DIALOG (PULSE SENSOR SIMULATOR)
// ---------------------------------------------------------------------------------
@Composable
fun HeartRateLoggerDialog(
    currentBpm: String,
    onSaveHeartRate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isSimulating by remember { mutableStateOf(false) }
    var displayedBpm by remember { mutableStateOf(if (currentBpm == "--") "72" else currentBpm) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSimulating) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartPulse"
    )

    LaunchedEffect(isSimulating) {
        if (isSimulating) {
            repeat(6) {
                delay(500)
                displayedBpm = (70..88).random().toString()
            }
            isSimulating = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RoseAccent.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = RoseAccent)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Heart Rate Monitor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pulsing Heart Sensor Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(RoseAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Pulse",
                        tint = RoseAccent,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayedBpm,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoseAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = if (isSimulating) "⚡ Measuring real-time pulse..." else "Resting Heart Rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSimulating) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { isSimulating = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = RoseAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Pulse Reading", color = RoseAccent, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onSaveHeartRate(displayedBpm)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("Save Reading", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 5. WEIGHT LOGGER DIALOG
// ---------------------------------------------------------------------------------
@Composable
fun WeightLoggerDialog(
    currentWeightKg: Float,
    onSaveWeight: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableFloatStateOf(currentWeightKg) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = AmberAccent)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Log Body Weight",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.1f".format(weight),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = AmberAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steppers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { weight = (weight - 0.2f).coerceAtLeast(30f) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "-0.2kg", tint = AmberAccent)
                    }

                    Text(
                        text = "Adjust ±0.2 kg",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { weight = (weight + 0.2f).coerceAtMost(200f) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "+0.2kg", tint = AmberAccent)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onSaveWeight(weight)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("Save Weight", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 6. SIGN-IN / PROFILE AUTH MODAL
// ---------------------------------------------------------------------------------
@Composable
fun SignInDialog(
    currentName: String,
    currentEmail: String,
    onSignIn: (name: String, email: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👤 User Sign In",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sign in to sync your health data across your smartwatch and phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        cursorColor = EmeraldPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        cursorColor = EmeraldPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSignIn(name, email)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Sign In & Continue", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onSignIn("Guest Athlete", "guest@healthtrack.ai")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue as Guest", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Helper Sub-Components
// ---------------------------------------------------------------------------------
@Composable
private fun QuickPillButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun WaterCupOption(
    emoji: String,
    label: String,
    desc: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color)
            Text(text = desc, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
