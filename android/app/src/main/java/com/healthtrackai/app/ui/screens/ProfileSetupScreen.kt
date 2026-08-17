package com.healthtrackai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthtrackai.app.data.models.ExerciseCategory
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    healthState: HealthStateHolder,
    onCompleteSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(healthState.user.name.ifBlank { "Alex Rivera" }) }
    var ageText by remember { mutableStateOf(healthState.user.age.toString()) }
    var gender by remember { mutableStateOf(healthState.user.gender) }
    var heightText by remember { mutableStateOf(healthState.user.heightCm.toString()) }
    var weightText by remember { mutableStateOf(healthState.user.weightKg.toString()) }
    var stepGoalText by remember { mutableStateOf(healthState.stepGoal.toString()) }
    var waterGoalText by remember { mutableStateOf(healthState.waterGoalMl.toString()) }
    var sleepGoalText by remember { mutableStateOf(healthState.sleepGoalHours.toString()) }
    var activityLevel by remember { mutableStateOf("Moderate") }
    var preferredWorkout by remember { mutableStateOf(healthState.user.preferredWorkout) }

    var expandedGender by remember { mutableStateOf(false) }
    var expandedActivity by remember { mutableStateOf(false) }
    var expandedWorkout by remember { mutableStateOf(false) }

    val genderOptions = listOf("Female", "Male", "Non-Binary", "Prefer not to say")
    val activityLevels = listOf("Sedentary (mostly sitting)", "Light (light walking)", "Moderate (active daily)", "Very Active (frequent workouts)")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Personalize Your Plan",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Set up your wellness profile & daily targets",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            TextButton(onClick = {
                healthState.isProfileSetupCompleted = true
                onCompleteSetup()
            }) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Fields
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Gender Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedGender,
                        onExpandedChange = { expandedGender = !expandedGender },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedGender,
                            onDismissRequest = { expandedGender = false }
                        ) {
                            genderOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        gender = opt
                                        expandedGender = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
            }

            // Daily Target Goals
            item {
                Text(
                    text = "Daily Wellness Targets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = stepGoalText,
                    onValueChange = { stepGoalText = it },
                    label = { Text("Daily Step Target") },
                    trailingIcon = { Text("steps ", style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = waterGoalText,
                        onValueChange = { waterGoalText = it },
                        label = { Text("Water Target") },
                        trailingIcon = { Text("ml ", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sleepGoalText,
                        onValueChange = { sleepGoalText = it },
                        label = { Text("Sleep Target") },
                        trailingIcon = { Text("hrs ", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
            }

            // Preferred Workout Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = expandedWorkout,
                    onExpandedChange = { expandedWorkout = !expandedWorkout },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = preferredWorkout.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Preferred Workout Routine") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWorkout) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedWorkout,
                        onDismissRequest = { expandedWorkout = false }
                    ) {
                        ExerciseCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.iconEmoji} ${cat.displayName}") },
                                onClick = {
                                    preferredWorkout = cat
                                    expandedWorkout = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                val age = ageText.toIntOrNull() ?: 24
                val height = heightText.toIntOrNull() ?: 178
                val weight = weightText.toFloatOrNull() ?: 70.5f
                val stepGoal = stepGoalText.toIntOrNull() ?: 10000
                val waterGoal = waterGoalText.toIntOrNull() ?: 2500
                val sleepGoal = sleepGoalText.toFloatOrNull() ?: 8.0f

                healthState.updateProfile(
                    name = name.ifBlank { "Alex Rivera" },
                    age = age,
                    gender = gender,
                    heightCm = height,
                    weightKg = weight,
                    activityLevel = activityLevel,
                    stepGoal = stepGoal,
                    waterGoalMl = waterGoal,
                    sleepGoalHours = sleepGoal,
                    preferredWorkout = preferredWorkout
                )
                healthState.isProfileSetupCompleted = true
                onCompleteSetup()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text(text = "Save & Continue &rarr;", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
