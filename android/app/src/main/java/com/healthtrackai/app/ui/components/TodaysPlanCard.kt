package com.healthtrackai.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.PurpleAccent
import com.healthtrackai.app.ui.theme.RoseAccent

@Composable
fun TodaysPlanCard(
    healthState: HealthStateHolder,
    onNavigateToTrack: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "YOUR PLAN FOR TODAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "Daily Wellness Goals",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Tap item to log",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Steps Item
            val stepRatio = (healthState.currentSteps.toFloat() / healthState.stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
            PlanItemRow(
                iconEmoji = "🚶",
                title = "Daily Steps",
                currentText = "${healthState.currentSteps} / ${healthState.stepGoal}",
                unit = "steps",
                progress = stepRatio,
                accentColor = EmeraldPrimary,
                isCompleted = healthState.currentSteps >= healthState.stepGoal,
                onClick = { onNavigateToTrack("steps") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Water Item
            val waterRatio = (healthState.currentWaterMl.toFloat() / healthState.waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
            PlanItemRow(
                iconEmoji = "💧",
                title = "Hydration Intake",
                currentText = "${(healthState.currentWaterMl / 1000f).formatOneDec()} / ${(healthState.waterGoalMl / 1000f).formatOneDec()}",
                unit = "Liters",
                progress = waterRatio,
                accentColor = CyanAccent,
                isCompleted = healthState.currentWaterMl >= healthState.waterGoalMl,
                onClick = { onNavigateToTrack("water") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Exercise Item
            val exerciseRatio = (healthState.activeMinutes.toFloat() / healthState.activeMinutesGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
            PlanItemRow(
                iconEmoji = "🏃",
                title = "Exercise & Movement",
                currentText = "${healthState.activeMinutes} / ${healthState.activeMinutesGoal}",
                unit = "minutes",
                progress = exerciseRatio,
                accentColor = RoseAccent,
                isCompleted = healthState.activeMinutes >= healthState.activeMinutesGoal,
                onClick = { onNavigateToTrack("exercise") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Sleep Item
            val sleepRatio = (healthState.sleepHours / healthState.sleepGoalHours.coerceAtLeast(1f)).coerceIn(0f, 1f)
            PlanItemRow(
                iconEmoji = "😴",
                title = "Sleep & Rest",
                currentText = healthState.sleepDurationFormatted,
                unit = "target ${healthState.sleepGoalHours.toInt()}h",
                progress = sleepRatio,
                accentColor = PurpleAccent,
                isCompleted = healthState.sleepHours >= 7.0f,
                onClick = { onNavigateToTrack("sleep") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Mood Item
            val isMoodLogged = healthState.currentMood != null
            PlanItemRow(
                iconEmoji = healthState.currentMood?.emoji ?: "😊",
                title = "Mood Check-in",
                currentText = healthState.currentMood?.label ?: "Not logged yet",
                unit = if (isMoodLogged) "Recorded" else "Tap to log",
                progress = if (isMoodLogged) 1.0f else 0f,
                accentColor = AmberAccent,
                isCompleted = isMoodLogged,
                onClick = { onNavigateToTrack("mood") }
            )
        }
    }
}

@Composable
private fun PlanItemRow(
    iconEmoji: String,
    title: String,
    currentText: String,
    unit: String,
    progress: Float,
    accentColor: Color,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(text = iconEmoji, fontSize = 22.sp)

            Spacer(modifier = Modifier.width(12.dp))

            // Text & Progress
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "$currentText $unit (${(progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Checkmark
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                contentDescription = null,
                tint = if (isCompleted) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun Float.formatOneDec(): String {
    return String.format("%.1f", this)
}
