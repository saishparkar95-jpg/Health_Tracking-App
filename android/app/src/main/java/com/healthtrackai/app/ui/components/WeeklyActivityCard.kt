package com.healthtrackai.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.ui.theme.EmeraldLight
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.HealthTrackAITheme

data class WeekDayStep(
    val day: String,
    val steps: Int,
    val isToday: Boolean = false
)

@Composable
fun WeeklyActivityCard(
    weeklySteps: List<WeekDayStep> = listOf(
        WeekDayStep("Mon", 0),
        WeekDayStep("Tue", 0),
        WeekDayStep("Wed", 0),
        WeekDayStep("Thu", 0),
        WeekDayStep("Fri", 0),
        WeekDayStep("Sat", 0),
        WeekDayStep("Sun", 0, isToday = true)
    ),
    stepGoal: Int = 10000,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📊 Weekly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Daily step activity overview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Goal: %,d".format(stepGoal),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7-day Bar Chart Placeholder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklySteps.forEach { item ->
                    val ratio = if (item.steps > 0) (item.steps.toFloat() / 12000f).coerceIn(0.12f, 1f) else 0.12f
                    val animatedRatio by animateFloatAsState(
                        targetValue = ratio,
                        animationSpec = tween(durationMillis = 800),
                        label = "BarRatio"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Value label
                        Text(
                            text = if (item.steps >= 1000) "${(item.steps / 1000f).toInt()}k" else "${item.steps}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (item.isToday) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Pillar Bar
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(animatedRatio)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    brush = if (item.steps >= stepGoal) {
                                        Brush.verticalGradient(listOf(EmeraldLight, EmeraldPrimary))
                                    } else if (item.isToday) {
                                        Brush.verticalGradient(listOf(EmeraldPrimary.copy(alpha = 0.8f), EmeraldPrimary.copy(alpha = 0.4f)))
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                            )
                                        )
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day label
                        Text(
                            text = item.day,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isToday) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklyActivityCardPreview() {
    HealthTrackAITheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            WeeklyActivityCard()
        }
    }
}
