package com.healthtrackai.app.data.models

data class DashboardState(
    val userName: String = "Alex Rivera",
    val dateDisplay: String = "Today, Oct 24",
    val wellnessScore: WellnessScore = WellnessScore(
        score = 88,
        level = "Optimal Flow",
        streakDays = 7,
        motivationText = "You've met your activity goals consistently this week!"
    ),
    val steps: StepSummary = StepSummary(
        current = 8450,
        goal = 10000,
        distanceKm = 6.2f,
        calories = 385,
        activeMinutes = 48
    ),
    val water: WaterSummary = WaterSummary(
        currentMl = 2250,
        goalMl = 3000
    ),
    val sleep: SleepSummary = SleepSummary(
        hours = 7.8f,
        goalHours = 8.0f,
        qualityScore = 86,
        deepSleepMinutes = 110
    ),
    val weight: WeightSummary = WeightSummary(
        currentKg = 72.4f,
        bmi = 23.1f,
        bmiCategory = "Normal Weight",
        targetKg = 70.0f
    ),
    val heartRate: HeartRateSummary = HeartRateSummary(
        latestBpm = 72,
        restingAvgBpm = 66,
        context = "Resting"
    ),
    val weeklyActivity: List<DailyActivityBar> = listOf(
        DailyActivityBar("Mon", 9200, 10000),
        DailyActivityBar("Tue", 10450, 10000),
        DailyActivityBar("Wed", 8100, 10000),
        DailyActivityBar("Thu", 11200, 10000),
        DailyActivityBar("Fri", 9800, 10000),
        DailyActivityBar("Sat", 12400, 10000),
        DailyActivityBar("Sun", 8450, 10000)
    )
)

data class StepSummary(
    val current: Int,
    val goal: Int,
    val distanceKm: Float,
    val calories: Int,
    val activeMinutes: Int
) {
    val progressPct: Float get() = (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val remainingSteps: Int get() = (goal - current).coerceAtLeast(0)
}

data class WaterSummary(
    val currentMl: Int,
    val goalMl: Int
) {
    val progressPct: Float get() = (currentMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
    val remainingMl: Int get() = (goalMl - currentMl).coerceAtLeast(0)
}

data class SleepSummary(
    val hours: Float,
    val goalHours: Float,
    val qualityScore: Int,
    val deepSleepMinutes: Int
) {
    val progressPct: Float get() = (hours / goalHours).coerceIn(0f, 1f)
}

data class WeightSummary(
    val currentKg: Float,
    val bmi: Float,
    val bmiCategory: String,
    val targetKg: Float
)

data class HeartRateSummary(
    val latestBpm: Int,
    val restingAvgBpm: Int,
    val context: String
)

data class WellnessScore(
    val score: Int,
    val level: String,
    val streakDays: Int,
    val motivationText: String
)

data class DailyActivityBar(
    val dayLabel: String,
    val steps: Int,
    val goal: Int
) {
    val isGoalMet: Boolean get() = steps >= goal
    val heightRatio: Float get() = (steps.toFloat() / 14000f).coerceIn(0.15f, 1f)
}
