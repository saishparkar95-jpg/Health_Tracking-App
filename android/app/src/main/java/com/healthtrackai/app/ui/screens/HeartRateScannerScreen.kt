package com.healthtrackai.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.RoseAccent
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun HeartRateScannerScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isMeasuring by remember { mutableStateOf(false) }
    var measurementProgress by remember { mutableFloatStateOf(0f) }
    var measuredBpm by remember { mutableIntStateOf(72) }
    var isMeasurementComplete by remember { mutableStateOf(false) }
    var showSavedDialog by remember { mutableStateOf(false) }

    // Heart pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isMeasuring) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isMeasuring) 420 else 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    // Measurement 15-second simulation / PPG process
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            measurementProgress = 0f
            isMeasurementComplete = false
            for (i in 1..15) {
                delay(1000)
                measurementProgress = i / 15f
                // Fluctuate measured BPM realistically between 68 and 78
                measuredBpm = (72 + sin(i.toDouble()) * 4).toInt()
            }
            isMeasuring = false
            isMeasurementComplete = true
        }
    }

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
                    Text(
                        text = "Camera Heart Rate Scanner",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Photoplethysmography (PPG) pulse analysis",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isMeasuring) RoseAccent.copy(alpha = 0.15f) else if (isMeasurementComplete) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isMeasuring) {
                        "🔴 Measuring pulse (${(measurementProgress * 100).toInt()}%) - Keep still"
                    } else if (isMeasurementComplete) {
                        "✅ Pulse Captured: $measuredBpm BPM"
                    } else {
                        "👆 Place index finger lightly on rear camera lens"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isMeasuring) RoseAccent else if (isMeasurementComplete) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Big Camera / Pulse Circle
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RoseAccent.copy(alpha = if (isMeasuring) 0.35f else 0.12f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(heartScale)
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(RoseAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heartbeat",
                        tint = RoseAccent,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            // Pulse Waveform / ECG Canvas
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val path = Path()
                    val width = size.width
                    val height = size.height
                    val midY = height / 2

                    path.moveTo(0f, midY)
                    val points = 30
                    for (i in 0..points) {
                        val x = (i / points.toFloat()) * width
                        val wave = if (isMeasuring || isMeasurementComplete) {
                            when (i % 8) {
                                2 -> -height * 0.4f
                                3 -> height * 0.45f
                                4 -> -height * 0.2f
                                else -> sin(i * 0.5f) * (height * 0.1f)
                            }
                        } else {
                            sin(i * 0.4f) * (height * 0.08f)
                        }
                        path.lineTo(x, midY + wave.toFloat())
                    }

                    drawPath(
                        path = path,
                        color = RoseAccent,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            // Live BPM Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$measuredBpm",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 58.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoseAccent
                    )
                )
                Text(
                    text = "BEATS PER MINUTE (BPM)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val zone = when {
                    measuredBpm < 60 -> "🔵 Resting / Athletic Zone"
                    measuredBpm in 60..100 -> "🟢 Normal Resting Rhythm"
                    else -> "🟠 Elevated Cardio Zone"
                }
                Text(
                    text = zone,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isMeasuring && !isMeasurementComplete) {
                    Button(
                        onClick = { isMeasuring = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start 15s Heart Rate Scan", fontWeight = FontWeight.Bold)
                    }
                } else if (isMeasuring) {
                    Button(
                        onClick = { isMeasuring = false },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Cancel Scan", color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Button(
                        onClick = { isMeasuring = true },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retake")
                    }

                    Button(
                        onClick = {
                            healthState.setHeartRate(measuredBpm.toString())
                            showSavedDialog = true
                        },
                        modifier = Modifier.weight(1.2f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Vitals", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSavedDialog) {
        AlertDialog(
            onDismissRequest = {
                showSavedDialog = false
                onNavigateBack()
            },
            title = { Text(text = "Heart Rate Saved! ❤️", fontWeight = FontWeight.Bold) },
            text = {
                Text(text = "Recorded $measuredBpm BPM to your daily wellness vitals.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSavedDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }
}
