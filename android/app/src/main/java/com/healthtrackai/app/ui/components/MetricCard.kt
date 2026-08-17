package com.healthtrackai.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.HealthTrackAITheme
import com.healthtrackai.app.ui.theme.PurpleAccent
import com.healthtrackai.app.ui.theme.RoseAccent

@Composable
fun MetricGridSection(
    waterCurrentMl: Int = 1250,
    waterGoalMl: Int = 2500,
    sleepDurationFormatted: String = "6h 30m",
    sleepGoalFormatted: String = "8h",
    sleepProgress: Float = 0.81f,
    heartRateBpm: String = "74",
    heartRateContext: String = "Resting Pulse",
    weightKg: String = "70.5",
    bmiText: String = "BMI 22.2",
    onWaterClick: () -> Unit = {},
    onQuickAddWater: (Int) -> Unit = {},
    onSleepClick: () -> Unit = {},
    onHeartRateClick: () -> Unit = {},
    onWeightClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Row 1: Water & Sleep
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Water Card (with quick +250ml tap)
            val waterProgress = if (waterGoalMl > 0) (waterCurrentMl.toFloat() / waterGoalMl.toFloat()).coerceIn(0f, 1f) else 0f
            ProgressMetricCard(
                title = "Water",
                emoji = "💧",
                icon = Icons.Filled.WaterDrop,
                mainValue = "$waterCurrentMl ml",
                targetValue = "/ $waterGoalMl ml",
                progress = waterProgress,
                accentColor = CyanAccent,
                onClick = onWaterClick,
                quickActionLabel = "+250 ml",
                onQuickAction = { onQuickAddWater(250) },
                modifier = Modifier.weight(1f)
            )

            // Sleep Card
            ProgressMetricCard(
                title = "Sleep",
                emoji = "😴",
                icon = Icons.Filled.Nightlight,
                mainValue = sleepDurationFormatted,
                targetValue = "/ $sleepGoalFormatted",
                progress = sleepProgress,
                accentColor = PurpleAccent,
                onClick = onSleepClick,
                quickActionLabel = "Log Rest",
                onQuickAction = onSleepClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Heart Rate & Weight
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Heart Rate Card
            SimpleMetricCard(
                title = "Heart Rate",
                emoji = "❤️",
                icon = Icons.Filled.Favorite,
                mainValue = heartRateBpm,
                unit = "BPM",
                subText = heartRateContext,
                accentColor = RoseAccent,
                onClick = onHeartRateClick,
                badgeText = "Measure",
                modifier = Modifier.weight(1f)
            )

            // Weight Card
            SimpleMetricCard(
                title = "Weight",
                emoji = "⚖️",
                icon = Icons.Filled.Scale,
                mainValue = weightKg,
                unit = "kg",
                subText = bmiText,
                accentColor = AmberAccent,
                onClick = onWeightClick,
                badgeText = "Update",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ProgressMetricCard(
    title: String,
    emoji: String,
    icon: ImageVector,
    mainValue: String,
    targetValue: String,
    progress: Float,
    accentColor: Color,
    onClick: () -> Unit = {},
    quickActionLabel: String? = null,
    onQuickAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "MetricProgress"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            // Header Row: Icon bubble + Percentage Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$emoji $title",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = mainValue,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = targetValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )

            if (quickActionLabel != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .clickable(onClick = onQuickAction)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = quickActionLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleMetricCard(
    title: String,
    emoji: String,
    icon: ImageVector,
    mainValue: String,
    unit: String,
    subText: String,
    accentColor: Color,
    onClick: () -> Unit = {},
    badgeText: String = "Tap",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            // Header Row: Icon bubble + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TouchApp, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$emoji $title",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = mainValue,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MetricGridSectionPreview() {
    HealthTrackAITheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MetricGridSection(
                waterCurrentMl = 1250,
                waterGoalMl = 2500,
                sleepDurationFormatted = "7h 15m",
                sleepGoalFormatted = "8h",
                sleepProgress = 0.9f,
                heartRateBpm = "74",
                heartRateContext = "Resting Pulse",
                weightKg = "70.5",
                bmiText = "BMI 22.2"
            )
        }
    }
}
