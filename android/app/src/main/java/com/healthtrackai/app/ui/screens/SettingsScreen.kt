package com.healthtrackai.app.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.healthtrackai.app.data.models.AppThemeMode
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.data.models.UnitSystem
import com.healthtrackai.app.ui.theme.AmberAccent
import com.healthtrackai.app.ui.theme.CyanAccent
import com.healthtrackai.app.ui.theme.EmeraldPrimary
import com.healthtrackai.app.ui.theme.HealthTrackAITheme
import com.healthtrackai.app.ui.theme.PurpleAccent
import com.healthtrackai.app.ui.theme.RoseAccent

@Composable
fun SettingsScreen(
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    onNavigateBack: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showGoalSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Settings & Preferences",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Settings Content List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. ACCOUNT & AUTH STATUS CARD
            item {
                AccountStatusCard(
                    healthState = healthState,
                    onAuthAction = onNavigateToAuth
                )
            }

            // 2. THEME & APPEARANCE (Light / Dark Theme Selector)
            item {
                ThemeSelectorSection(
                    currentTheme = healthState.themeMode,
                    onSelectTheme = { healthState.themeMode = it }
                )
            }

            // 3. TARGET GOALS CONFIGURATION
            item {
                GoalTargetSettingsCard(
                    stepGoal = healthState.stepGoal,
                    waterGoalMl = healthState.waterGoalMl,
                    sleepGoalHours = healthState.sleepGoalHours,
                    onConfigureClick = { showGoalSettingsDialog = true }
                )
            }

            // 4. UNITS OF MEASUREMENT
            item {
                UnitSystemCard(
                    unitSystem = healthState.unitSystem,
                    onSelectUnit = { healthState.unitSystem = it }
                )
            }

            // 5. NOTIFICATION PREFERENCES
            item {
                NotificationSettingsCard(
                    hydration = healthState.hydrationAlerts,
                    onHydrationChange = { healthState.hydrationAlerts = it },
                    inactivity = healthState.inactivityAlerts,
                    onInactivityChange = { healthState.inactivityAlerts = it },
                    sleep = healthState.sleepScheduleAlerts,
                    onSleepChange = { healthState.sleepScheduleAlerts = it },
                    daily = healthState.dailySummaryNotification,
                    onDailyChange = { healthState.dailySummaryNotification = it }
                )
            }

            // 6. DATA & PRIVACY ACTIONS
            item {
                DataManagementCard(
                    onResetToday = { healthState.resetDailyData() }
                )
            }

            // 7. APP ABOUT
            item {
                AboutAppCard()
            }
        }
    }

    if (showGoalSettingsDialog) {
        CustomizeGoalsDialog(
            currentSteps = healthState.stepGoal,
            currentWater = healthState.waterGoalMl,
            currentSleep = healthState.sleepGoalHours,
            onSave = { steps, water, sleep ->
                healthState.stepGoal = steps
                healthState.waterGoalMl = water
                healthState.sleepGoalHours = sleep
            },
            onDismiss = { showGoalSettingsDialog = false }
        )
    }
}

@Composable
private fun AccountStatusCard(
    healthState: HealthStateHolder,
    onAuthAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "Account & Authentication",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (healthState.user.isGuestTrial) AmberAccent.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = if (healthState.user.isGuestTrial) AmberAccent else EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (healthState.user.name.isNotBlank()) healthState.user.name else "Guest User",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (healthState.user.isGuestTrial) "⚠️ Guest Trial Mode" else healthState.user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (healthState.user.isGuestTrial) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (healthState.user.isGuestTrial) AmberAccent.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (healthState.user.isGuestTrial) "TRIAL" else "REGISTERED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (healthState.user.isGuestTrial) AmberAccent else EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (healthState.user.isGuestTrial || !healthState.user.isSignedIn) {
                    Button(
                        onClick = onAuthAction,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Register / Sign In", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    OutlinedButton(
                        onClick = onAuthAction,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Switch User", color = MaterialTheme.colorScheme.onBackground)
                    }

                    Button(
                        onClick = {
                            healthState.logout()
                            onAuthAction()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectorSection(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "🎨 Appearance & Theme",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionChip(
                    label = "Dark Mode",
                    icon = Icons.Filled.DarkMode,
                    isSelected = currentTheme == AppThemeMode.DARK,
                    onClick = { onSelectTheme(AppThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )

                ThemeOptionChip(
                    label = "Light Mode",
                    icon = Icons.Filled.LightMode,
                    isSelected = currentTheme == AppThemeMode.LIGHT,
                    onClick = { onSelectTheme(AppThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )

                ThemeOptionChip(
                    label = "System",
                    icon = Icons.Filled.PhoneAndroid,
                    isSelected = currentTheme == AppThemeMode.SYSTEM,
                    onClick = { onSelectTheme(AppThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                1.5.dp,
                if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun GoalTargetSettingsCard(
    stepGoal: Int,
    waterGoalMl: Int,
    sleepGoalHours: Float,
    onConfigureClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Daily Target Goals",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Customize",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary,
                    modifier = Modifier.clickable(onClick = onConfigureClick)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GoalTargetPill(title = "Steps Target", value = "%,d".format(stepGoal), color = EmeraldPrimary)
                GoalTargetPill(title = "Hydration", value = "$waterGoalMl ml", color = CyanAccent)
                GoalTargetPill(title = "Sleep Rest", value = "${sleepGoalHours.toInt()}h 0m", color = PurpleAccent)
            }
        }
    }
}

@Composable
private fun GoalTargetPill(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun UnitSystemCard(
    unitSystem: UnitSystem,
    onSelectUnit: (UnitSystem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "📏 Units of Measurement",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UnitOptionChip(
                    title = "Metric System",
                    desc = "kg, cm, ml, km",
                    isSelected = unitSystem == UnitSystem.METRIC,
                    onClick = { onSelectUnit(UnitSystem.METRIC) },
                    modifier = Modifier.weight(1f)
                )

                UnitOptionChip(
                    title = "Imperial System",
                    desc = "lbs, ft/in, oz, mi",
                    isSelected = unitSystem == UnitSystem.IMPERIAL,
                    onClick = { onSelectUnit(UnitSystem.IMPERIAL) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun UnitOptionChip(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) CyanAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.5.dp, if (isSelected) CyanAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (isSelected) CyanAccent else MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotificationSettingsCard(
    hydration: Boolean,
    onHydrationChange: (Boolean) -> Unit,
    inactivity: Boolean,
    onInactivityChange: (Boolean) -> Unit,
    sleep: Boolean,
    onSleepChange: (Boolean) -> Unit,
    daily: Boolean,
    onDailyChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "🔔 Notifications & Alerts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            NotificationToggleRow(title = "Hydration Drink Reminder", subtitle = "Periodic drink notifications", checked = hydration, onCheckedChange = onHydrationChange)
            NotificationToggleRow(title = "Inactivity Movement Alert", subtitle = "Alert when sedentary > 1 hour", checked = inactivity, onCheckedChange = onInactivityChange)
            NotificationToggleRow(title = "Bedtime Schedule Alert", subtitle = "Remind 30m before target sleep", checked = sleep, onCheckedChange = onSleepChange)
            NotificationToggleRow(title = "Daily Health Summary", subtitle = "Morning briefing & daily score", checked = daily, onCheckedChange = onDailyChange)
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldPrimary,
                checkedTrackColor = EmeraldPrimary.copy(alpha = 0.35f)
            )
        )
    }
}

@Composable
private fun DataManagementCard(
    onResetToday: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "💾 Data & Storage",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onResetToday,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = AmberAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Today's Tracked Metrics to 0", color = AmberAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AboutAppCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "ℹ️ About HealthTrack AI",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Version 1.0.0 • College Project Presentation Edition\nNative Android • Kotlin • Jetpack Compose • Material 3",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomizeGoalsDialog(
    currentSteps: Int,
    currentWater: Int,
    currentSleep: Float,
    onSave: (steps: Int, water: Int, sleep: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var steps by remember { mutableFloatStateOf(currentSteps.toFloat()) }
    var water by remember { mutableFloatStateOf(currentWater.toFloat()) }
    var sleep by remember { mutableFloatStateOf(currentSleep) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Customize Daily Goals", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)

                Spacer(modifier = Modifier.height(16.dp))

                // Steps slider
                Text(text = "Step Goal: %,d steps".format(steps.toInt()), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldPrimary)
                Slider(value = steps, onValueChange = { steps = it }, valueRange = 2000f..20000f, steps = 17, colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary))

                Spacer(modifier = Modifier.height(10.dp))

                // Water slider
                Text(text = "Hydration Goal: %,d ml".format(water.toInt()), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = CyanAccent)
                Slider(value = water, onValueChange = { water = it }, valueRange = 1000f..5000f, steps = 15, colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent))

                Spacer(modifier = Modifier.height(10.dp))

                // Sleep slider
                Text(text = "Sleep Goal: ${sleep.toInt()}h 0m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PurpleAccent)
                Slider(value = sleep, onValueChange = { sleep = it }, valueRange = 4f..12f, steps = 7, colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent))

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSave(steps.toInt(), water.toInt(), sleep)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Goals", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HealthTrackAITheme {
        Surface {
            SettingsScreen()
        }
    }
}
