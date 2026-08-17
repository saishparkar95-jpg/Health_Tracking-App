package com.healthtrackai.app.data.services

import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.data.models.MoodType

data class HealthScoreResult(
    val overallScore: Int,
    val deltaFromYesterday: Int,
    val activityScore: Int,      // Out of 25
    val sleepScore: Int,         // Out of 20
    val hydrationScore: Int,     // Out of 15
    val exerciseScore: Int,      // Out of 15
    val moodScore: Int,          // Out of 10
    val goalScore: Int,          // Out of 15
    val ratingLevel: String,     // Optimal, Balanced, Moderate, Needs Attention
    val explanation: String,
    val aiInsightText: String,
    val factorDeltas: Map<String, Int> = emptyMap()
)

object HealthScoreCalculator {

    /**
     * Calculates transparent weighted Health Score (0-100):
     * - Activity: 25%
     * - Sleep: 20%
     * - Hydration: 15%
     * - Exercise: 15%
     * - Mood: 10%
     * - Goal completion: 15%
     */
    fun calculateScore(state: HealthStateHolder): HealthScoreResult {
        // 1. Activity (25 pts)
        val stepRatio = if (state.stepGoal > 0) (state.currentSteps.toFloat() / state.stepGoal.toFloat()).coerceIn(0f, 1.2f) else 0f
        val activityPoints = (stepRatio.coerceAtMost(1.0f) * 25f).toInt()

        // 2. Sleep (20 pts)
        val sleepRatio = if (state.sleepGoalHours > 0) (state.sleepHours / state.sleepGoalHours).coerceIn(0f, 1.2f) else 0f
        val sleepPoints = when {
            state.sleepHours == 0f -> 8 // default fallback
            state.sleepHours in 7.0f..9.0f -> 20
            state.sleepHours in 6.0f..7.0f || state.sleepHours in 9.0f..10.0f -> 16
            else -> 10
        }

        // 3. Hydration (15 pts)
        val waterRatio = if (state.waterGoalMl > 0) (state.currentWaterMl.toFloat() / state.waterGoalMl.toFloat()).coerceIn(0f, 1.2f) else 0f
        val hydrationPoints = (waterRatio.coerceAtMost(1.0f) * 15f).toInt()

        // 4. Exercise (15 pts)
        val exerciseRatio = if (state.activeMinutesGoal > 0) (state.activeMinutes.toFloat() / state.activeMinutesGoal.toFloat()).coerceIn(0f, 1.2f) else 0f
        val exercisePoints = (exerciseRatio.coerceAtMost(1.0f) * 15f).toInt()

        // 5. Mood (10 pts)
        val moodPoints = when (state.currentMood) {
            MoodType.EXCELLENT -> 10
            MoodType.GOOD -> 8
            MoodType.OKAY -> 6
            MoodType.LOW -> 4
            MoodType.STRESSED -> 3
            null -> 6
        }

        // 6. Goal & Challenge Completion (15 pts)
        val completedChallenges = state.challenges.count { it.isCompleted }
        val totalChallenges = state.challenges.size.coerceAtLeast(1)
        val goalRatio = completedChallenges.toFloat() / totalChallenges.toFloat()
        val goalPoints = (goalRatio * 15f).toInt()

        val totalScore = (activityPoints + sleepPoints + hydrationPoints + exercisePoints + moodPoints + goalPoints).coerceIn(15, 100)

        // Estimated delta from yesterday based on current progress
        val delta = when {
            totalScore >= 85 -> +7
            totalScore >= 75 -> +4
            totalScore >= 60 -> +1
            else -> -3
        }

        val level = when {
            totalScore >= 85 -> "Optimal Flow"
            totalScore >= 70 -> "Balanced Wellness"
            totalScore >= 55 -> "Moderate Progress"
            else -> "Needs Attention"
        }

        val explanation = when {
            totalScore >= 80 -> "Strong consistency across movement, hydration, and restful sleep today."
            totalScore >= 60 -> "Good daily habit progress. Increasing water intake or a light walk will boost your score."
            else -> "Complete more daily wellness logs (steps, water, sleep) to improve your personalized score."
        }

        val aiInsight = when {
            stepRatio >= 0.8f && sleepRatio < 0.7f -> "Your activity has improved this week, but your sleep consistency has decreased."
            waterRatio < 0.6f -> "Your hydration is below target today. Try adding a glass of water to support afternoon focus."
            stepRatio >= 1.0f -> "Great job reaching your step goal! Consistent daily movement supports cardiovascular energy."
            else -> "You're building strong wellness momentum. Keep tracking daily habits!"
        }

        val factorDeltas = mapOf(
            "Activity" to if (activityPoints >= 18) +3 else -2,
            "Sleep" to if (sleepPoints >= 16) +2 else -4,
            "Hydration" to if (hydrationPoints >= 10) +2 else -1,
            "Exercise" to if (exercisePoints >= 10) +2 else 0,
            "Mood" to if (moodPoints >= 7) +1 else -1
        )

        return HealthScoreResult(
            overallScore = totalScore,
            deltaFromYesterday = delta,
            activityScore = activityPoints,
            sleepScore = sleepPoints,
            hydrationScore = hydrationPoints,
            exerciseScore = exercisePoints,
            moodScore = moodPoints,
            goalScore = goalPoints,
            ratingLevel = level,
            explanation = explanation,
            aiInsightText = aiInsight,
            factorDeltas = factorDeltas
        )
    }
}
