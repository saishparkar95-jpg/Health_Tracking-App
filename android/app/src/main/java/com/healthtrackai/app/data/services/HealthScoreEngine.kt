package com.healthtrackai.app.data.services

import com.healthtrackai.app.data.healthconnect.DailyHealthRecord
import com.healthtrackai.app.data.models.HealthStateHolder

data class HealthScoreBreakdown(
    val overallScore: Int,
    val ratingTier: String,       // "Excellent", "Good", "Needs Attention"
    val deltaFromYesterday: Int,
    val sleepScore: Int,          // Out of 25
    val activityScore: Int,       // Out of 25
    val stepScore: Int,           // Out of 20
    val heartRateScore: Int,      // Out of 15
    val workoutScore: Int,        // Out of 10
    val hydrationScore: Int,      // Out of 5 (when available)
    val whyChangedExplanation: String,
    val highlights: List<String>,
    val improvementTips: List<String>,
    val disclaimer: String = "This wellness score is generated from your connected activity and rest metrics for general wellness guidance only, not for medical diagnosis or clinical evaluation."
)

object HealthScoreEngine {

    fun calculateScore(
        state: HealthStateHolder,
        todayRecord: DailyHealthRecord? = null,
        pastDays: List<DailyHealthRecord> = emptyList()
    ): HealthScoreBreakdown {
        val steps = todayRecord?.steps?.toInt() ?: state.currentSteps
        val stepGoal = state.stepGoal.coerceAtLeast(1000)
        val sleepHours = todayRecord?.sleepMinutes?.let { it.toFloat() / 60f } ?: state.sleepHours
        val activeMins = todayRecord?.activeMinutes?.toInt() ?: state.activeMinutes
        val activeMinsGoal = state.activeMinutesGoal.coerceAtLeast(15)
        val hrSummary = todayRecord?.heartRateSummary ?: state.latestHeartRateSummary
        val hydrationLiters = todayRecord?.hydrationLiters ?: (state.currentWaterMl.toDouble() / 1000.0)
        val isHydrationAvailable = todayRecord?.hydrationLiters != null || state.isHydrationSourceConnected

        val reasons = mutableListOf<String>()
        val tips = mutableListOf<String>()

        // 1. Sleep Score (25 pts)
        val sleepScore = when {
            sleepHours >= 7.5f && sleepHours <= 9.0f -> {
                reasons.add("Optimal sleep duration ($sleepHours hrs)")
                25
            }
            sleepHours in 6.5f..7.49f || sleepHours in 9.01f..10.0f -> {
                reasons.add("Good restful sleep ($sleepHours hrs)")
                20
            }
            sleepHours in 5.0f..6.49f -> {
                tips.add("Aim for at least 7 hours of sleep to improve recovery.")
                14
            }
            sleepHours > 0f -> {
                tips.add("Sleep duration was low ($sleepHours hrs). Prioritize early bedtime tonight.")
                8
            }
            else -> 12 // default baseline when sleep unmeasured
        }

        // 2. Activity / Active Minutes (25 pts)
        val actRatio = (activeMins.toFloat() / activeMinsGoal.toFloat()).coerceIn(0f, 1.2f)
        val activityScore = (actRatio.coerceAtMost(1.0f) * 25f).toInt()
        if (actRatio >= 1.0f) {
            reasons.add("Active minutes target achieved ($activeMins min)")
        } else if (actRatio < 0.5f) {
            tips.add("Take a brisk 15-minute walk to hit your daily activity goal.")
        }

        // 3. Step Score (20 pts)
        val stepRatio = (steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1.2f)
        val stepScore = (stepRatio.coerceAtMost(1.0f) * 20f).toInt()
        if (stepRatio >= 1.0f) {
            reasons.add("Step goal surpassed ($steps steps)")
        } else if (stepRatio >= 0.7f) {
            reasons.add("Steady daily walking momentum ($steps steps)")
        } else {
            tips.add("Move more throughout the afternoon to reach your step milestone.")
        }

        // 4. Heart Rate Score (15 pts)
        val heartRateScore = if (hrSummary?.latestBpm != null) {
            val bpm = hrSummary.latestBpm
            val resting = hrSummary.restingBpm
            when {
                resting != null && resting in 50..72 -> {
                    reasons.add("Excellent resting heart rate ($resting BPM)")
                    15
                }
                bpm in 55..85 -> {
                    reasons.add("Normal resting pulse ($bpm BPM)")
                    13
                }
                bpm in 86..100 -> {
                    tips.add("Heart rate is slightly elevated ($bpm BPM). Stay hydrated and relaxed.")
                    10
                }
                else -> 8
            }
        } else {
            11 // baseline when unmeasured
        }

        // 5. Workout / Consistency (10 pts)
        val sessionCount = todayRecord?.exerciseSessions?.size ?: state.healthConnectWorkouts.size
        val workoutScore = when {
            sessionCount >= 2 -> {
                reasons.add("High workout consistency ($sessionCount sessions today)")
                10
            }
            sessionCount == 1 -> {
                reasons.add("Completed planned exercise session")
                8
            }
            activeMins >= 20 -> 7
            else -> 4
        }

        // 6. Hydration Score (5 pts)
        val hydrationScore = if (isHydrationAvailable && hydrationLiters > 0.0) {
            if (hydrationLiters >= 2.0) {
                reasons.add("Hydration goal met (${String.format("%.1f", hydrationLiters)} L)")
                5
            } else if (hydrationLiters >= 1.2) {
                4
            } else {
                tips.add("Increase water intake to maintain peak energy levels.")
                2
            }
        } else {
            3 // neutral baseline when no hydration hardware/source exists
        }

        val totalRaw = sleepScore + activityScore + stepScore + heartRateScore + workoutScore + hydrationScore
        val finalScore = totalRaw.coerceIn(10, 100)

        val ratingTier = when {
            finalScore >= 85 -> "Excellent"
            finalScore >= 70 -> "Good"
            else -> "Needs Attention"
        }

        // Calculate delta from yesterday if historical records exist
        val yesterdayRecord = pastDays.getOrNull(pastDays.size - 2)
        val delta = if (yesterdayRecord != null) {
            val ySteps = yesterdayRecord.steps?.toInt() ?: 0
            val ySleep = yesterdayRecord.sleepMinutes?.let { it.toFloat() / 60f } ?: 0f
            val yScoreEst = ((ySteps.toFloat() / stepGoal.toFloat()) * 30f + (ySleep / 8f) * 35f + 25f).toInt().coerceIn(10, 100)
            finalScore - yScoreEst
        } else {
            if (finalScore >= 85) +6 else if (finalScore >= 70) +2 else -4
        }

        val whyExplanation = if (reasons.isNotEmpty()) {
            val lead = if (delta >= 0) "Score is up by $delta pts" else "Score is down by ${-delta} pts"
            "$lead due to: ${reasons.take(3).joinToString(", ")}."
        } else {
            "Your score reflects your synchronized activity, sleep, and heart health metrics."
        }

        return HealthScoreBreakdown(
            overallScore = finalScore,
            ratingTier = ratingTier,
            deltaFromYesterday = delta,
            sleepScore = sleepScore,
            activityScore = activityScore,
            stepScore = stepScore,
            heartRateScore = heartRateScore,
            workoutScore = workoutScore,
            hydrationScore = hydrationScore,
            whyChangedExplanation = whyExplanation,
            highlights = reasons,
            improvementTips = tips
        )
    }
}
