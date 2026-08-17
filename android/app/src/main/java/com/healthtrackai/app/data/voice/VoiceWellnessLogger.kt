package com.healthtrackai.app.data.voice

import com.healthtrackai.app.data.models.ExerciseCategory
import com.healthtrackai.app.data.models.ExerciseSession
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.data.models.MoodType

sealed class VoiceActionResult(
    val title: String,
    val description: String,
    val iconEmoji: String
) {
    class WaterAdded(val amountMl: Int) : VoiceActionResult("Hydration Logged", "+$amountMl ml water recorded", "💧")
    class StepsAdded(val count: Int) : VoiceActionResult("Steps Added", "+$count walking steps added", "👟")
    class SleepLogged(val hours: Float) : VoiceActionResult("Sleep Recorded", "$hours hours of sleep saved", "🌙")
    class HeartRateLogged(val bpm: String) : VoiceActionResult("Heart Rate Saved", "$bpm BPM pulse recorded", "❤️")
    class WeightLogged(val kg: Float) : VoiceActionResult("Weight Updated", "$kg kg weight logged", "⚖️")
    class MoodLogged(val mood: MoodType) : VoiceActionResult("Mood Check-in", "Mood logged as ${mood.label}", mood.emoji)
    class WalkStarted(val minutes: Int) : VoiceActionResult("Walk Session Logged", "$minutes min walk saved", "🚶")
    class Unrecognized(val originalText: String) : VoiceActionResult("Command Not Recognized", "Try saying: 'Log 500ml water' or 'Walked 2000 steps'", "❓")
}

/**
 * Natural Language Voice Wellness Intent Parser
 * Converts spoken phrases into structured health metric updates.
 */
class VoiceWellnessLogger(
    private val healthState: HealthStateHolder
) {

    fun parseAndExecute(voiceQuery: String): VoiceActionResult {
        val clean = voiceQuery.trim().lowercase()

        // 1. Water Tracking
        if (clean.contains("water") || clean.contains("drink") || clean.contains("drank") || clean.contains("hydrate") || clean.contains("glass")) {
            val amount = when {
                clean.contains("1 glass") || clean.contains("one glass") -> 250
                clean.contains("2 glass") || clean.contains("two glass") -> 500
                clean.contains("3 glass") || clean.contains("three glass") -> 750
                clean.contains("bottle") -> 600
                else -> {
                    val numbers = Regex("\\d+").findAll(clean).map { it.value.toInt() }.toList()
                    if (numbers.isNotEmpty()) {
                        val num = numbers.first()
                        if (num in 10..5000) num else 250
                    } else 250
                }
            }
            healthState.addWater(amount)
            return VoiceActionResult.WaterAdded(amount)
        }

        // 2. Step Tracking
        if (clean.contains("step") || clean.contains("steps") || clean.contains("walked")) {
            val numbers = Regex("\\d+").findAll(clean).map { it.value.toInt() }.toList()
            val stepCount = if (numbers.isNotEmpty()) numbers.first() else 1000
            healthState.addSteps(stepCount)
            return VoiceActionResult.StepsAdded(stepCount)
        }

        // 3. Sleep Rest
        if (clean.contains("sleep") || clean.contains("slept") || clean.contains("nap")) {
            val numbers = Regex("\\d+(\\.\\d+)?").findAll(clean).mapNotNull { it.value.toFloatOrNull() }.toList()
            val hours = if (numbers.isNotEmpty()) numbers.first() else 8.0f
            healthState.setSleep(hours)
            return VoiceActionResult.SleepLogged(hours)
        }

        // 4. Heart Rate
        if (clean.contains("heart") || clean.contains("pulse") || clean.contains("bpm")) {
            val numbers = Regex("\\d+").findAll(clean).map { it.value.toInt() }.toList()
            val bpm = if (numbers.isNotEmpty()) numbers.first().toString() else "72"
            healthState.setHeartRate(bpm)
            return VoiceActionResult.HeartRateLogged(bpm)
        }

        // 5. Weight & BMI
        if (clean.contains("weight") || clean.contains("weigh") || clean.contains("kg")) {
            val numbers = Regex("\\d+(\\.\\d+)?").findAll(clean).mapNotNull { it.value.toFloatOrNull() }.toList()
            val weight = if (numbers.isNotEmpty()) numbers.first() else 70.5f
            healthState.setWeight(weight)
            return VoiceActionResult.WeightLogged(weight)
        }

        // 6. Mood Check-in
        if (clean.contains("mood") || clean.contains("feeling") || clean.contains("feel")) {
            val mood = when {
                clean.contains("great") || clean.contains("excellent") || clean.contains("amazing") || clean.contains("awesome") -> MoodType.EXCELLENT
                clean.contains("good") || clean.contains("happy") || clean.contains("fine") -> MoodType.GOOD
                clean.contains("tired") || clean.contains("low") || clean.contains("sad") -> MoodType.LOW
                clean.contains("stress") || clean.contains("anxious") -> MoodType.STRESSED
                else -> MoodType.OKAY
            }
            healthState.logMood(mood, "Logged via Voice AI")
            return VoiceActionResult.MoodLogged(mood)
        }

        // 7. Quick Workout Session
        if (clean.contains("workout") || clean.contains("run") || clean.contains("exercise") || clean.contains("walk")) {
            val numbers = Regex("\\d+").findAll(clean).map { it.value.toInt() }.toList()
            val mins = if (numbers.isNotEmpty()) numbers.first() else 25
            healthState.addExerciseSession(
                ExerciseSession(
                    id = System.currentTimeMillis().toString(),
                    category = ExerciseCategory.WALKING,
                    durationMinutes = mins,
                    distanceKm = mins * 0.08f,
                    steps = mins * 105,
                    caloriesBurned = (mins * 4.5f).toInt(),
                    timestamp = "Just now",
                    dateLabel = "Today"
                )
            )
            return VoiceActionResult.WalkStarted(mins)
        }

        return VoiceActionResult.Unrecognized(voiceQuery)
    }
}
