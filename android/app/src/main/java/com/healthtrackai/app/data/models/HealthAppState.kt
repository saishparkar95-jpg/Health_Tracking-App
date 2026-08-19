package com.healthtrackai.app.data.models

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.healthtrackai.app.data.preferences.AppPreferences

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class UnitSystem {
    METRIC,    // kg, cm, ml, km
    IMPERIAL   // lbs, ft/in, oz, miles
}

enum class MoodType(val emoji: String, val label: String, val scoreValue: Int) {
    EXCELLENT("😍", "Excellent", 5),
    GOOD("😊", "Good", 4),
    OKAY("😐", "Okay", 3),
    LOW("😔", "Low", 2),
    STRESSED("😫", "Stressed", 1)
}

enum class ExerciseCategory(val displayName: String, val iconEmoji: String, val defaultCalPerMin: Float) {
    WALKING("Walking", "🚶", 4.2f),
    RUNNING("Running", "🏃", 9.5f),
    CYCLING("Cycling", "🚴", 7.8f),
    GYM("Gym Workout", "🏋️", 6.5f),
    YOGA("Yoga & Stretch", "🧘", 3.5f),
    HOME_WORKOUT("Home Workout", "🏠", 5.8f),
    OTHER("Other Activity", "⚡", 5.0f)
}

data class MoodEntry(
    val id: String,
    val mood: MoodType,
    val note: String = "",
    val timestamp: String,
    val dateLabel: String = "Today"
)

data class ExerciseSession(
    val id: String,
    val category: ExerciseCategory,
    val durationMinutes: Int,
    val distanceKm: Float = 0f,
    val steps: Int = 0,
    val caloriesBurned: Int,
    val timestamp: String,
    val dateLabel: String = "Today"
)

data class MealEntry(
    val id: String,
    val name: String,
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val timestamp: String,
    val hasVegetables: Boolean = true
)

data class ChallengeItem(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val current: Int,
    val unit: String,
    val isCompleted: Boolean = false,
    val rewardBadge: String = "🏆",
    val participantsCount: Int = 142
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val category: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null
)

data class RegisteredAccount(
    val name: String,
    val email: String,
    val passwordHash: String,
    val age: Int = 24,
    val gender: String = "Prefer not to say",
    val heightCm: Int = 178,
    val weightKg: Float = 70.5f,
    val activityLevel: String = "Moderate"
)

data class UserProfile(
    val name: String = "Alex Rivera",
    val email: String = "alex.rivera@healthtrack.ai",
    val age: Int = 24,
    val gender: String = "Prefer not to say",
    val heightCm: Int = 178,
    val weightKg: Float = 70.5f,
    val activityLevel: String = "Moderate",
    val preferredWorkout: ExerciseCategory = ExerciseCategory.WALKING,
    val isSignedIn: Boolean = true,
    val isGuestTrial: Boolean = false
)

data class DayLog(
    val day: String,
    val steps: Int,
    val goal: Int,
    val isToday: Boolean = false
)

class HealthStateHolder(context: Context? = null) {
    private val appPreferences: AppPreferences? = context?.let { AppPreferences(it) }

    // App Flow flags
    var isOnboardingCompleted by mutableStateOf(appPreferences?.isOnboardingCompleted ?: true)
    var isProfileSetupCompleted by mutableStateOf(appPreferences?.isProfileSetupCompleted ?: true)

    // In-memory registered accounts
    val registeredAccounts = mutableStateListOf<RegisteredAccount>().apply {
        appPreferences?.loadAccounts()?.let { addAll(it) }
    }

    // Current active user
    var user by mutableStateOf(appPreferences?.loadUserProfile() ?: UserProfile(isSignedIn = true, isGuestTrial = false))

    // Theme & Preferences
    var themeMode by mutableStateOf(appPreferences?.themeMode ?: AppThemeMode.DARK)
    var unitSystem by mutableStateOf(UnitSystem.METRIC)

    // Notification Toggles
    var hydrationAlerts by mutableStateOf(true)
    var inactivityAlerts by mutableStateOf(true)
    var sleepScheduleAlerts by mutableStateOf(true)
    var dailySummaryNotification by mutableStateOf(true)
    var goalReminders by mutableStateOf(true)
    var challengeNotifications by mutableStateOf(true)

    // Health Connect Synchronization State
    var healthConnectSdkStatus by mutableStateOf(com.healthtrackai.app.data.healthconnect.HealthConnectSdkStatus.CHECKING)
    var healthConnectPermissionState by mutableStateOf(com.healthtrackai.app.data.healthconnect.HealthConnectPermissionState.CHECKING)
    var isSyncingHealthConnect by mutableStateOf(false)
    var lastHealthConnectSyncTime by mutableStateOf("Just now")
    var healthConnectErrorMessage by mutableStateOf<String?>(null)
    var isHydrationSourceConnected by mutableStateOf(false)

    // Live Health Connect Record models
    var todayHealthRecord by mutableStateOf<com.healthtrackai.app.data.healthconnect.DailyHealthRecord?>(null)
    var latestSleepSession by mutableStateOf<com.healthtrackai.app.data.healthconnect.SleepSessionData?>(null)
    var latestHeartRateSummary by mutableStateOf<com.healthtrackai.app.data.healthconnect.HeartRateSummary?>(null)
    val healthConnectWorkouts = mutableStateListOf<com.healthtrackai.app.data.healthconnect.ExerciseRecordItem>()
    val historical7Days = mutableStateListOf<com.healthtrackai.app.data.healthconnect.DailyHealthRecord>()
    val connectedDataSources = mutableStateListOf<com.healthtrackai.app.data.healthconnect.MetricSourceInfo>()

    // Calculated Health Score & AI Summaries
    var currentHealthScoreResult by mutableStateOf<com.healthtrackai.app.data.services.HealthScoreBreakdown?>(null)
    var todayAiDailySummary by mutableStateOf<com.healthtrackai.app.data.services.AiDailySummaryData?>(null)
    var weeklyAiReport by mutableStateOf<com.healthtrackai.app.data.services.WeeklyHealthReportData?>(null)

    // Daily Steps & Activity (starts from 0 for fresh accounts / fresh days)
    var currentSteps by mutableIntStateOf(appPreferences?.todaySteps ?: 0)
    var stepGoal by mutableIntStateOf(appPreferences?.stepGoal ?: 10000)
    var activeMinutes by mutableIntStateOf(appPreferences?.todayActiveMinutes ?: 0)
    var activeMinutesGoal by mutableIntStateOf(60)

    // Real-Time Sensor & Pedometer State
    var isSensorActive by mutableStateOf(false)
    var activeSensorTypeDescription by mutableStateOf("Hardware Step Detector")
    var sensorCadenceSpm by mutableIntStateOf(0)

    val distanceKm: Float get() = (currentSteps * 0.00075f)
    val caloriesBurned: Int get() = (currentSteps * 0.042f + activeMinutes * 4.5f).toInt()
    val stepProgress: Float get() = if (stepGoal > 0) (currentSteps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f) else 0f

    // Water (starts from 0ml)
    var currentWaterMl by mutableIntStateOf(appPreferences?.todayWaterMl ?: 0)
    var waterGoalMl by mutableIntStateOf(appPreferences?.waterGoalMl ?: 2500)
    val waterProgress: Float get() = if (waterGoalMl > 0) (currentWaterMl.toFloat() / waterGoalMl.toFloat()).coerceIn(0f, 1f) else 0f

    // Sleep (starts from 0.0h)
    var sleepHours by mutableFloatStateOf(appPreferences?.todaySleepHours ?: 0.0f)
    var sleepGoalHours by mutableFloatStateOf(appPreferences?.sleepGoalHours ?: 8.0f)
    var sleepBedtime by mutableStateOf("23:15")
    var sleepWakeTime by mutableStateOf("06:45")
    var sleepQualityScore by mutableIntStateOf(82)
    val sleepProgress: Float get() = if (sleepGoalHours > 0) (sleepHours / sleepGoalHours).coerceIn(0f, 1f) else 0f
    val sleepDurationFormatted: String get() {
        val hrs = sleepHours.toInt()
        val mins = ((sleepHours - hrs) * 60).toInt()
        return if (hrs == 0 && mins == 0) "0h 0m" else "${hrs}h ${mins}m"
    }

    // Heart Rate (starts unmeasured)
    var heartRateBpm by mutableStateOf(appPreferences?.todayHeartRate ?: "--")

    // Mood History
    var currentMood by mutableStateOf<MoodType?>(null)
    val moodHistory = mutableStateListOf<MoodEntry>()

    // Exercise History
    val exerciseHistory = mutableStateListOf<ExerciseSession>()

    // Food / Meal History
    val mealHistory = mutableStateListOf<MealEntry>()

    // Weight & BMI
    var currentWeightKg by mutableFloatStateOf(appPreferences?.loadUserProfile()?.weightKg ?: 70.5f)
    var targetWeightKg by mutableFloatStateOf(68.0f)
    val bmi: Float get() {
        val hMeters = user.heightCm / 100f
        return if (hMeters > 0) currentWeightKg / (hMeters * hMeters) else 22.2f
    }
    val bmiCategory: String get() = when {
        bmi < 18.5f -> "Underweight"
        bmi < 24.9f -> "Normal Weight"
        bmi < 29.9f -> "Overweight"
        else -> "Obese"
    }

    // Weekly Step Logs (current week initialized with 0 steps today)
    val weeklyLogs = mutableStateListOf(
        DayLog("Mon", 0, 10000),
        DayLog("Tue", 0, 10000),
        DayLog("Wed", 0, 10000),
        DayLog("Thu", 0, 10000),
        DayLog("Fri", 0, 10000),
        DayLog("Sat", 0, 10000),
        DayLog("Sun", 0, 10000, isToday = true)
    )

    // Daily Challenges & Streaks (starts fresh from 0)
    var currentStreakDays by mutableIntStateOf(1)
    val challenges = mutableStateListOf(
        ChallengeItem("c1", "Walk 8,000 Steps", "Keep active throughout the day", 8000, 0, "steps", false, "🚶", 320),
        ChallengeItem("c2", "Hydrate to 2.5L", "Drink at least 2,500ml water", 2500, 0, "ml", false, "💧", 215),
        ChallengeItem("c3", "Log Your Mood", "Record how you feel today", 1, 0, "log", false, "😊", 180),
        ChallengeItem("c4", "Complete 20m Workout", "Log any exercise session", 20, 0, "mins", false, "🏃", 145)
    )

    // Achievements & Badges
    val achievements = mutableStateListOf(
        AchievementBadge("a1", "10K Steps Club", "Walk over 10,000 steps in a single day", "🏃", "Activity", false, null),
        AchievementBadge("a2", "Hydration Hero", "Achieve daily water goal 5 days in a row", "💧", "Hydration", false, null),
        AchievementBadge("a3", "7-Day Streak", "Maintain a 7-day consistency streak", "🔥", "Habits", false, null),
        AchievementBadge("a4", "Sleep Champion", "Log 8+ hours of quality restful sleep", "😴", "Rest", false, null),
        AchievementBadge("a5", "Mindful Moment", "Log daily mood 7 days consistently", "😊", "Wellness", false, null),
        AchievementBadge("a6", "30-Day Consistency", "Complete 30 consecutive active days", "🏆", "Mastery", false, null),
        AchievementBadge("a7", "Century Cyclist", "Record 100km total cycling distance", "🚴", "Fitness", false, null),
        AchievementBadge("a8", "Nutrition Master", "Scan and log 20 balanced meals", "🥗", "Nutrition", false, null)
    )

    // Active Walk / Workout Session State
    var isWorkoutActive by mutableStateOf(false)
    var activeWorkoutCategory by mutableStateOf(ExerciseCategory.WALKING)
    var workoutDurationSec by mutableIntStateOf(0)
    var workoutSteps by mutableIntStateOf(0)

    init {
        checkAndResetDailyMetricsIfNeeded()
    }

    /**
     * Automatic Date / Midnight Reset:
     * When opening the app on a new calendar day, resets all daily trackers (steps, water, sleep, active minutes) to 0.
     */
    fun checkAndResetDailyMetricsIfNeeded() {
        try {
            val todayDateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            appPreferences?.let { prefs ->
                val lastDate = prefs.lastActiveDate
                if (lastDate.isNotBlank() && lastDate != todayDateString) {
                    // New Day Detected: Reset metrics to 0
                    currentSteps = 0
                    currentWaterMl = 0
                    activeMinutes = 0
                    sleepHours = 0.0f
                    heartRateBpm = "--"

                    prefs.todaySteps = 0
                    prefs.todayWaterMl = 0
                    prefs.todayActiveMinutes = 0
                    prefs.todaySleepHours = 0.0f
                    prefs.todayHeartRate = "--"

                    // Reset challenge progress for the new day
                    for (i in challenges.indices) {
                        challenges[i] = challenges[i].copy(current = 0, isCompleted = false)
                    }
                } else if (lastDate == todayDateString) {
                    // Same day: load saved daily metrics
                    currentSteps = prefs.todaySteps
                    currentWaterMl = prefs.todayWaterMl
                    activeMinutes = prefs.todayActiveMinutes
                    sleepHours = prefs.todaySleepHours
                    heartRateBpm = prefs.todayHeartRate
                }
                prefs.lastActiveDate = todayDateString
            }
            updateWeeklyLogToday()
        } catch (e: Throwable) {
            // Ignore date parsing exceptions gracefully
        }
    }

    private fun updateWeeklyLogToday() {
        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        // Convert Calendar.DAY_OF_WEEK (Sun=1, Mon=2, ...) to 0-indexed Mon-Sun
        val todayIndex = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - 2
        for (i in weeklyLogs.indices) {
            weeklyLogs[i] = weeklyLogs[i].copy(
                isToday = (i == todayIndex),
                steps = if (i == todayIndex) currentSteps else weeklyLogs[i].steps
            )
        }
    }

    // Interactive Action Handlers
    fun addSteps(amount: Int) {
        currentSteps = (currentSteps + amount).coerceAtLeast(0)
        appPreferences?.todaySteps = currentSteps
        updateWeeklyLogToday()
        checkStepChallenges()
    }

    fun addWater(amountMl: Int) {
        currentWaterMl = (currentWaterMl + amountMl).coerceAtLeast(0)
        appPreferences?.todayWaterMl = currentWaterMl
        checkWaterChallenges()
    }

    fun setSleep(hours: Float) {
        sleepHours = hours.coerceIn(0f, 16f)
        appPreferences?.todaySleepHours = sleepHours
    }

    fun setHeartRate(bpm: String) {
        heartRateBpm = bpm
        appPreferences?.todayHeartRate = bpm
    }

    fun setWeight(kg: Float) {
        currentWeightKg = kg
        user = user.copy(weightKg = kg)
        appPreferences?.saveUserProfile(user)
    }

    fun logMood(mood: MoodType, note: String = "") {
        currentMood = mood
        moodHistory.add(0, MoodEntry(System.currentTimeMillis().toString(), mood, note, "Just now", "Today"))
    }

    fun addExerciseSession(session: ExerciseSession) {
        exerciseHistory.add(0, session)
        activeMinutes += session.durationMinutes
    }

    fun addMeal(meal: MealEntry) {
        mealHistory.add(0, meal)
    }

    private fun checkStepChallenges() {
        val index = challenges.indexOfFirst { it.id == "c1" }
        if (index != -1) {
            val item = challenges[index]
            challenges[index] = item.copy(
                current = currentSteps,
                isCompleted = currentSteps >= item.target
            )
        }
    }

    private fun checkWaterChallenges() {
        val index = challenges.indexOfFirst { it.id == "c2" }
        if (index != -1) {
            val item = challenges[index]
            challenges[index] = item.copy(
                current = currentWaterMl,
                isCompleted = currentWaterMl >= item.target
            )
        }
    }

    // -------------------------------------------------------------
    // AUTHENTICATION & REGISTRATION
    // -------------------------------------------------------------
    fun register(
        name: String,
        email: String,
        password: String,
        age: Int = 24,
        gender: String = "Prefer not to say",
        heightCm: Int = 178,
        weightKg: Float = 70.5f,
        activityLevel: String = "Moderate"
    ): Boolean {
        val cleanName = name.trim().ifBlank { "User" }
        val cleanEmail = email.trim().lowercase().ifBlank { "user@healthtrack.ai" }
        val cleanPass = password.ifBlank { "password" }

        val existingIndex = registeredAccounts.indexOfFirst { it.email == cleanEmail }
        val account = RegisteredAccount(
            name = cleanName,
            email = cleanEmail,
            passwordHash = cleanPass,
            age = age,
            gender = gender,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel
        )
        if (existingIndex != -1) {
            registeredAccounts[existingIndex] = account
        } else {
            registeredAccounts.add(account)
        }

        user = UserProfile(
            name = cleanName,
            email = cleanEmail,
            age = age,
            gender = gender,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel,
            isSignedIn = true,
            isGuestTrial = false
        )
        currentWeightKg = weightKg
        isOnboardingCompleted = true
        isProfileSetupCompleted = true

        appPreferences?.let { prefs ->
            prefs.isOnboardingCompleted = true
            prefs.isProfileSetupCompleted = true
            prefs.saveAccounts(registeredAccounts)
            prefs.saveUserProfile(user)
        }

        return true
    }

    fun login(email: String, password: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()
        val account = registeredAccounts.find { it.email == cleanEmail && it.passwordHash == cleanPass }
        return if (account != null) {
            user = UserProfile(
                name = account.name,
                email = account.email,
                age = account.age,
                gender = account.gender,
                heightCm = account.heightCm,
                weightKg = account.weightKg,
                activityLevel = account.activityLevel,
                isSignedIn = true,
                isGuestTrial = false
            )
            currentWeightKg = account.weightKg
            isOnboardingCompleted = true
            isProfileSetupCompleted = true

            appPreferences?.let { prefs ->
                prefs.isOnboardingCompleted = true
                prefs.isProfileSetupCompleted = true
                prefs.saveUserProfile(user)
            }
            true
        } else if (registeredAccounts.isEmpty()) {
            register(
                name = cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
                email = cleanEmail,
                password = cleanPass
            )
            true
        } else {
            false
        }
    }

    fun continueAsGuest() {
        user = UserProfile(
            name = "Guest User",
            email = "guest.trial@healthtrack.ai",
            age = 24,
            heightCm = 178,
            weightKg = 70.5f,
            isSignedIn = true,
            isGuestTrial = true
        )
        currentWeightKg = 70.5f
        isOnboardingCompleted = true
        isProfileSetupCompleted = true

        appPreferences?.let { prefs ->
            prefs.isOnboardingCompleted = true
            prefs.isProfileSetupCompleted = true
            prefs.saveUserProfile(user)
        }
    }

    fun logout() {
        user = UserProfile(isSignedIn = false, isGuestTrial = false)
        appPreferences?.clearSession()
    }

    fun updateProfile(
        name: String,
        age: Int,
        gender: String,
        heightCm: Int,
        weightKg: Float,
        activityLevel: String,
        stepGoal: Int,
        waterGoalMl: Int,
        sleepGoalHours: Float,
        preferredWorkout: ExerciseCategory
    ) {
        user = user.copy(
            name = name,
            age = age,
            gender = gender,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel,
            preferredWorkout = preferredWorkout
        )
        this.currentWeightKg = weightKg
        this.stepGoal = stepGoal
        this.waterGoalMl = waterGoalMl
        this.sleepGoalHours = sleepGoalHours
        isOnboardingCompleted = true
        isProfileSetupCompleted = true

        appPreferences?.let { prefs ->
            prefs.isOnboardingCompleted = true
            prefs.isProfileSetupCompleted = true
            prefs.stepGoal = stepGoal
            prefs.waterGoalMl = waterGoalMl
            prefs.sleepGoalHours = sleepGoalHours
            prefs.saveUserProfile(user)
        }
    }

    fun updateProfile(name: String, age: Int, heightCm: Int, weightKg: Float) {
        user = user.copy(
            name = name,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg
        )
        this.currentWeightKg = weightKg
        isOnboardingCompleted = true
        isProfileSetupCompleted = true

        appPreferences?.let { prefs ->
            prefs.isOnboardingCompleted = true
            prefs.isProfileSetupCompleted = true
            prefs.saveUserProfile(user)
        }
    }

    fun completeOnboarding() {
        isOnboardingCompleted = true
        appPreferences?.isOnboardingCompleted = true
    }

    fun completeProfileSetup() {
        isProfileSetupCompleted = true
        appPreferences?.isProfileSetupCompleted = true
    }

    fun resetDailyData() {
        currentSteps = 0
        currentWaterMl = 0
        sleepHours = 0f
        heartRateBpm = "--"
        val index = weeklyLogs.indexOfFirst { it.isToday }
        if (index != -1) {
            weeklyLogs[index] = weeklyLogs[index].copy(steps = 0)
        }
    }
}
