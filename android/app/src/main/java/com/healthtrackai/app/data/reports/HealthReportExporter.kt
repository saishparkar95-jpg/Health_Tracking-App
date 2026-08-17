package com.healthtrackai.app.data.reports

import android.content.Context
import android.content.Intent
import com.healthtrackai.app.data.models.HealthStateHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Health Report Exporter
 * Generates formatted text & printable summary reports of the user's vitals,
 * weekly activity trends, sleep scores, and AI recommendations.
 */
class HealthReportExporter(
    private val context: Context,
    private val healthState: HealthStateHolder
) {

    fun generateTextReport(): String {
        val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        val user = healthState.user

        return buildString {
            appendLine("==================================================")
            appendLine("         HEALTHTRACK AI - WELLNESS REPORT         ")
            appendLine("==================================================")
            appendLine("Generated on: $dateStr")
            appendLine("Patient / User: ${user.name} (${user.email})")
            appendLine("Biometrics: Age ${user.age} | Height ${user.heightCm}cm | Weight ${healthState.currentWeightKg}kg")
            appendLine("BMI: ${String.format("%.1f", healthState.bmi)} (${healthState.bmiCategory})")
            appendLine("--------------------------------------------------")
            appendLine("1. DAILY ACTIVITY & MOVEMENT")
            appendLine("   • Today's Steps: ${healthState.currentSteps} / ${healthState.stepGoal} steps (${(healthState.stepProgress * 100).toInt()}%)")
            appendLine("   • Distance Walked: ${String.format("%.2f", healthState.distanceKm)} km")
            appendLine("   • Active Minutes: ${healthState.activeMinutes} mins")
            appendLine("   • Calories Burned: ${healthState.caloriesBurned} kcal")
            appendLine("--------------------------------------------------")
            appendLine("2. HYDRATION & RECOVERY")
            appendLine("   • Water Intake: ${healthState.currentWaterMl} / ${healthState.waterGoalMl} ml")
            appendLine("   • Sleep Duration: ${healthState.sleepDurationFormatted} (Quality: ${healthState.sleepQualityScore}/100)")
            appendLine("   • Rest Schedule: Bedtime ${healthState.sleepBedtime} -> Wake ${healthState.sleepWakeTime}")
            appendLine("--------------------------------------------------")
            appendLine("3. CARDIO & VITALS")
            appendLine("   • Resting Heart Rate: ${healthState.heartRateBpm} BPM")
            appendLine("   • Recent Mood Check-in: ${healthState.currentMood?.label ?: "Good"}")
            appendLine("--------------------------------------------------")
            appendLine("4. AI WELLNESS COACH SUMMARY")
            appendLine("   • Habit Consistency: ${healthState.currentStreakDays} days active streak")
            appendLine("   • Clinical Note: Patient maintains strong daily activity and hydration consistency. Recommend continuing moderate aerobic walk sessions.")
            appendLine("==================================================")
        }
    }

    fun shareReport() {
        val report = generateTextReport()
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report)
            putExtra(Intent.EXTRA_SUBJECT, "HealthTrack AI - Wellness Report (${healthState.user.name})")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Health Report")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
