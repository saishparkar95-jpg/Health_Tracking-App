package com.healthtrackai.app.data.services

import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.data.models.MoodType

data class CoachMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String = "Just now",
    val isMedicalDisclaimer: Boolean = false
)

enum class MessageSender {
    USER,
    AI_COACH
}

object AiCoachService {

    /**
     * Generates personalized daily wellness insights based on active user metrics.
     */
    fun generatePersonalizedInsights(state: HealthStateHolder): List<String> {
        val insights = mutableListOf<String>()

        // Steps insight
        if (state.currentSteps >= state.stepGoal) {
            insights.add("🎉 You've reached your daily step goal (${state.currentSteps.formatNumber()} steps). Outstanding consistency!")
        } else if (state.currentSteps < state.stepGoal * 0.5) {
            val remaining = (state.stepGoal - state.currentSteps).formatNumber()
            insights.add("🚶 You're at ${state.currentSteps.formatNumber()} steps. A brisk 20-minute walk will help you cover the remaining $remaining steps.")
        } else {
            insights.add("👟 Great progress today! You're ${((state.currentSteps.toFloat() / state.stepGoal) * 100).toInt()}% towards your daily movement target.")
        }

        // Hydration insight
        if (state.currentWaterMl < state.waterGoalMl * 0.6) {
            insights.add("💧 Hydration reminder: You have logged ${state.currentWaterMl}ml out of ${state.waterGoalMl}ml. Drinking 2 glasses will rebalance your intake.")
        } else {
            insights.add("💧 Excellent hydration level today (${state.currentWaterMl}ml / ${state.waterGoalMl}ml).")
        }

        // Sleep insight
        if (state.sleepHours > 0 && state.sleepHours < 6.5f) {
            insights.add("😴 You logged ${state.sleepDurationFormatted} of sleep. Consider an earlier wind-down routine tonight to restore energy.")
        } else if (state.sleepHours >= 7.5f) {
            insights.add("🌙 Optimal sleep logged (${state.sleepDurationFormatted}). Good rest enhances recovery and mental clarity.")
        }

        // Exercise insight
        if (state.exerciseHistory.isNotEmpty()) {
            insights.add("🏃 You have completed ${state.exerciseHistory.size} logged workouts recently. Great physical momentum!")
        }

        return insights
    }

    /**
     * Answers user questions with context-aware wellness reasoning and medical safety guardrails.
     */
    fun getCoachResponse(question: String, state: HealthStateHolder): CoachMessage {
        val q = question.lowercase().trim()

        // 1. Medical diagnosis safety guardrail
        val medicalKeywords = listOf("diagnose", "cure", "disease", "cancer", "chest pain", "heart attack", "stroke", "infection", "prescribe", "medicine", "hypertension", "diabetes")
        if (medicalKeywords.any { q.contains(it) }) {
            return CoachMessage(
                id = System.currentTimeMillis().toString(),
                sender = MessageSender.AI_COACH,
                text = "⚠️ HealthTrack AI is a general wellness companion and cannot provide medical diagnosis or treatment. For concerning symptoms, chest discomfort, or medical questions, please consult a qualified healthcare professional immediately.",
                isMedicalDisclaimer = true
            )
        }

        // 2. Context-aware prompt answering
        val replyText = when {
            q.contains("what should i do") || q.contains("today's plan") || q.contains("plan for today") -> {
                val remainingSteps = (state.stepGoal - state.currentSteps).coerceAtLeast(0)
                val remainingWater = (state.waterGoalMl - state.currentWaterMl).coerceAtLeast(0)
                "Here is your recommended wellness focus for today:\n\n" +
                "1. 🚶 Movement: Walk $remainingSteps more steps to hit your daily goal.\n" +
                "2. 💧 Hydration: Drink ${remainingWater}ml more water throughout the afternoon.\n" +
                "3. 🏃 Exercise: Complete 20 minutes of moderate exercise (${state.user.preferredWorkout.displayName}).\n" +
                "4. 😴 Rest: Aim for ${state.sleepGoalHours.toInt()} hours of sleep tonight with a 10:30 PM wind-down."
            }

            q.contains("sleep") -> {
                "To optimize your sleep quality and consistency:\n\n" +
                "• Keep a consistent bedtime (target: ${state.sleepBedtime}).\n" +
                "• Avoid bright screens and heavy meals 45 minutes before sleep.\n" +
                "• Keep your bedroom cool and dark for deep REM recovery.\n" +
                "• Log your wake-up time each morning to track weekly sleep debt."
            }

            q.contains("health score") || q.contains("score low") || q.contains("score changing") -> {
                val result = HealthScoreCalculator.calculateScore(state)
                "Your current Health Score is ${result.overallScore}/100 (${result.ratingLevel}).\n\n" +
                "Breakdown of key factors:\n" +
                "• Activity: ${result.activityScore}/25 pts\n" +
                "• Sleep: ${result.sleepScore}/20 pts\n" +
                "• Hydration: ${result.hydrationScore}/15 pts\n" +
                "• Exercise: ${result.exerciseScore}/15 pts\n" +
                "• Mood: ${result.moodScore}/10 pts\n" +
                "• Goal Completion: ${result.goalScore}/15 pts\n\n" +
                "Tip: Logging your unrecorded water or adding a 15-minute walk will boost your score immediately!"
            }

            q.contains("step") || q.contains("walk") -> {
                "To reach your ${state.stepGoal.formatNumber()} step target:\n\n" +
                "• Take a 10-minute walk after meals (adds ~1,200 steps).\n" +
                "• Use stairs instead of elevators.\n" +
                "• Set hourly standing reminders.\n" +
                "• Try a 20-minute evening neighborhood walk."
            }

            q.contains("workout") || q.contains("exercise") || q.contains("gym") -> {
                "Here is a simple 20-minute daily wellness workout plan:\n\n" +
                "• 3 min: Dynamic warm-up & arm swings\n" +
                "• 12 min: 3 sets of Bodyweight Squats (12 reps), Push-ups or Wall push-ups (10 reps), Plank hold (30s)\n" +
                "• 5 min: Light cool-down stretch & deep breathing\n\n" +
                "Remember to log your session under the Exercise tracker!"
            }

            q.contains("water") || q.contains("hydration") || q.contains("drink") -> {
                "Healthy hydration habits:\n\n" +
                "• Drink 1 glass (250ml) right after waking up.\n" +
                "• Keep a reusable water bottle near your desk.\n" +
                "• Drink 250ml before each meal.\n" +
                "• Current logged intake: ${state.currentWaterMl}ml / ${state.waterGoalMl}ml."
            }

            q.contains("mood") || q.contains("stress") || q.contains("feeling") -> {
                val mood = state.currentMood?.label ?: "Good"
                "You recently recorded your mood as $mood. Regular physical movement and 7-8 hours of sleep have shown a positive correlation with your mood trends. Taking a 5-minute deep breathing break can also help center your focus."
            }

            else -> {
                "Great question! Based on your current profile, focusing on consistent daily habits—such as reaching ${state.stepGoal.formatNumber()} steps, hydrating to ${state.waterGoalMl}ml, and logging your sleep—will give you the highest wellness return. What specific goal would you like help with today?"
            }
        }

        return CoachMessage(
            id = System.currentTimeMillis().toString(),
            sender = MessageSender.AI_COACH,
            text = replyText,
            isMedicalDisclaimer = false
        )
    }

    private fun Int.formatNumber(): String {
        return java.text.NumberFormat.getIntegerInstance().format(this)
    }
}
