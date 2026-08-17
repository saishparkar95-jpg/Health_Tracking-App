package com.healthtrackai.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.components.SleepLoggerDialog
import com.healthtrackai.app.ui.components.StepLoggerDialog
import com.healthtrackai.app.ui.components.WaterLoggerDialog
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.HealthTrackAITheme
import com.healthtrackai.app.ui.theme.PurpleAccent
import com.healthtrackai.app.ui.theme.RoseAccent

@Composable
fun GoalsScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    modifier: Modifier = Modifier
) {
    var showStepDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    val stepMet = healthState.currentSteps >= healthState.stepGoal
    val waterMet = healthState.currentWaterMl >= healthState.waterGoalMl
    val sleepMet = healthState.sleepHours >= healthState.sleepGoalHours
    val caloriesMet = healthState.caloriesBurned >= 500
    val completedCount = listOf(stepMet, waterMet, sleepMet, caloriesMet).count { it }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        GoalsHeaderSection()

        // Content List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Goals Status Summary Banner
            item {
                GoalsSummaryBanner(
                    completedCount = completedCount,
                    totalGoals = 4
                )
            }

            // 1. Daily Step Goal Card (Interactive)
            item {
                GoalProgressCard(
                    title = "Daily Step Goal",
                    subtitle = "Cardio & Movement Target",
                    currentValue = "%,d".format(healthState.currentSteps),
                    goalValue = "%,d steps".format(healthState.stepGoal),
                    currentNumeric = healthState.currentSteps.toFloat(),
                    goalNumeric = healthState.stepGoal.toFloat(),
                    icon = Icons.Filled.DirectionsWalk,
                    accentColor = EmeraldPrimary,
                    streakDays = 5,
                    remainingText = if (stepMet) "🎉 Step Goal Reached!" else "${"%,d".format((healthState.stepGoal - healthState.currentSteps).coerceAtLeast(0))} steps left",
                    onClick = { showStepDialog = true }
                )
            }

            // 2. Water Hydration Goal Card (Interactive)
            item {
                GoalProgressCard(
                    title = "Water Hydration Goal",
                    subtitle = "Daily Hydration Balance",
                    currentValue = "${healthState.currentWaterMl}",
                    goalValue = "${healthState.waterGoalMl} ml",
                    currentNumeric = healthState.currentWaterMl.toFloat(),
                    goalNumeric = healthState.waterGoalMl.toFloat(),
                    icon = Icons.Filled.WaterDrop,
                    accentColor = CyanAccent,
                    streakDays = 7,
                    remainingText = if (waterMet) "🎉 Hydration Target Met!" else "${(healthState.waterGoalMl - healthState.currentWaterMl).coerceAtLeast(0)} ml remaining",
                    onClick = { showWaterDialog = true }
                )
            }

            // 3. Sleep Recovery Goal Card (Interactive)
            item {
                GoalProgressCard(
                    title = "Sleep Recovery Goal",
                    subtitle = "Rest & Circadian Rhythm",
                    currentValue = healthState.sleepDurationFormatted,
                    goalValue = "${healthState.sleepGoalHours.toInt()}h 00m",
                    currentNumeric = healthState.sleepHours,
                    goalNumeric = healthState.sleepGoalHours,
                    icon = Icons.Filled.Bedtime,
                    accentColor = PurpleAccent,
                    streakDays = 4,
                    remainingText = if (sleepMet) "🎉 Rest Target Accomplished!" else "%.1fh remaining to goal".format((healthState.sleepGoalHours - healthState.sleepHours).coerceAtLeast(0f)),
                    onClick = { showSleepDialog = true }
                )
            }

            // 4. Calorie Burn Goal Card
            item {
                GoalProgressCard(
                    title = "Active Energy Burn",
                    subtitle = "Daily Exertion Metric",
                    currentValue = "${healthState.caloriesBurned}",
                    goalValue = "500 kcal",
                    currentNumeric = healthState.caloriesBurned.toFloat(),
                    goalNumeric = 500f,
                    icon = Icons.Filled.LocalFireDepartment,
                    accentColor = RoseAccent,
                    streakDays = 6,
                    remainingText = if (caloriesMet) "🎉 Active Burn Met!" else "${(500 - healthState.caloriesBurned).coerceAtLeast(0)} kcal left",
                    onClick = { showStepDialog = true }
                )
            }

            // 5. Milestone Badges Section
            item {
                MilestoneBadgesSection()
            }
        }
    }

    if (showStepDialog) {
        StepLoggerDialog(
            currentSteps = healthState.currentSteps,
            stepGoal = healthState.stepGoal,
            onAddSteps = { amount -> healthState.addSteps(amount) },
            onDismiss = { showStepDialog = false }
        )
    }

    if (showWaterDialog) {
        WaterLoggerDialog(
            currentMl = healthState.currentWaterMl,
            goalMl = healthState.waterGoalMl,
            onAddWater = { amount -> healthState.addWater(amount) },
            onDismiss = { showWaterDialog = false }
        )
    }

    if (showSleepDialog) {
        SleepLoggerDialog(
            currentHours = healthState.sleepHours,
            goalHours = healthState.sleepGoalHours,
            onSaveSleep = { hrs -> healthState.setSleep(hrs) },
            onDismiss = { showSleepDialog = false }
        )
    }
}

@Composable
private fun GoalsHeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Health Goals",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Track and conquer your personalized daily wellness milestones",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalsSummaryBanner(
    completedCount: Int,
    totalGoals: Int
) {
    val progress = (completedCount.toFloat() / totalGoals.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        EmeraldPrimary.copy(alpha = 0.2f),
                        CyanAccent.copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        EmeraldPrimary.copy(alpha = 0.6f),
                        CyanAccent.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = AmberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Daily Goals Progress",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$completedCount of $totalGoals goals completed for today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(54.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    strokeWidth = 5.dp
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(54.dp),
                    color = EmeraldPrimary,
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun GoalProgressCard(
    title: String,
    subtitle: String,
    currentValue: String,
    goalValue: String,
    currentNumeric: Float,
    goalNumeric: Float,
    icon: ImageVector,
    accentColor: Color,
    streakDays: Int,
    remainingText: String,
    onClick: () -> Unit = {}
) {
    val progress = if (goalNumeric > 0f) (currentNumeric / goalNumeric).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900),
        label = "GoalProgressAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column {
            // Header Row: Icon + Title + Streak Badge
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
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = "Tap",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AmberAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔥 ${streakDays}d",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AmberAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Values Row: Current vs Goal + Percentage Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = currentValue,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ $goalValue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Remaining Text
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MilestoneBadgesSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Active Streaks & Smartwatch Badges",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BadgeCard(
                emoji = "💧",
                title = "Hydration Pro",
                desc = "7-Day Streak",
                modifier = Modifier.weight(1f)
            )

            BadgeCard(
                emoji = "🚶",
                title = "10k Step Star",
                desc = "5-Day Hit",
                modifier = Modifier.weight(1f)
            )

            BadgeCard(
                emoji = "⌚",
                title = "Rings Master",
                desc = "All 3 Rings Closed",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BadgeCard(
    emoji: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GoalsScreenPreview() {
    HealthTrackAITheme {
        Surface {
            GoalsScreen()
        }
    }
}
