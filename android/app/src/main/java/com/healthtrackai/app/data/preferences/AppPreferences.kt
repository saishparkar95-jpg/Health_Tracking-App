package com.healthtrackai.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.healthtrackai.app.data.models.AppThemeMode
import com.healthtrackai.app.data.models.ExerciseCategory
import com.healthtrackai.app.data.models.RegisteredAccount
import com.healthtrackai.app.data.models.UserProfile

/**
 * Local persistent storage manager using Android SharedPreferences.
 * Ensures user login status, onboarding completion, and profile details are permanently remembered
 * so the user is never asked to re-login or re-enter details repeatedly.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "healthtrack_ai_prefs"

        // Auth & Navigation flags
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_PROFILE_SETUP_COMPLETED = "profile_setup_completed"
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
        private const val KEY_IS_GUEST_TRIAL = "is_guest_trial"

        // Daily Tracker Persistence & Date Tracking
        private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
        private const val KEY_TODAY_STEPS = "today_steps"
        private const val KEY_TODAY_WATER_ML = "today_water_ml"
        private const val KEY_TODAY_ACTIVE_MINUTES = "today_active_minutes"
        private const val KEY_TODAY_SLEEP_HOURS = "today_sleep_hours"
        private const val KEY_TODAY_HEART_RATE = "today_heart_rate"
        private const val KEY_WEEKLY_HISTORY_DATA = "weekly_history_data"

        // User Profile
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AGE = "user_age"
        private const val KEY_USER_GENDER = "user_gender"
        private const val KEY_USER_HEIGHT = "user_height"
        private const val KEY_USER_WEIGHT = "user_weight"
        private const val KEY_USER_ACTIVITY_LEVEL = "user_activity_level"
        private const val KEY_USER_PREFERRED_WORKOUT = "user_preferred_workout"

        // Goals & Settings
        private const val KEY_STEP_GOAL = "step_goal"
        private const val KEY_WATER_GOAL = "water_goal"
        private const val KEY_SLEEP_GOAL = "sleep_goal"
        private const val KEY_THEME_MODE = "theme_mode"

        // Accounts Storage
        private const val KEY_REGISTERED_ACCOUNTS = "registered_accounts_data"
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isProfileSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_PROFILE_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROFILE_SETUP_COMPLETED, value).apply()

    var isSignedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_SIGNED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SIGNED_IN, value).apply()

    var isGuestTrial: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST_TRIAL, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST_TRIAL, value).apply()

    var lastActiveDate: String
        get() = prefs.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_ACTIVE_DATE, value).apply()

    var todaySteps: Int
        get() = prefs.getInt(KEY_TODAY_STEPS, 0)
        set(value) = prefs.edit().putInt(KEY_TODAY_STEPS, value).apply()

    var todayWaterMl: Int
        get() = prefs.getInt(KEY_TODAY_WATER_ML, 0)
        set(value) = prefs.edit().putInt(KEY_TODAY_WATER_ML, value).apply()

    var todayActiveMinutes: Int
        get() = prefs.getInt(KEY_TODAY_ACTIVE_MINUTES, 0)
        set(value) = prefs.edit().putInt(KEY_TODAY_ACTIVE_MINUTES, value).apply()

    var todaySleepHours: Float
        get() = prefs.getFloat(KEY_TODAY_SLEEP_HOURS, 0.0f)
        set(value) = prefs.edit().putFloat(KEY_TODAY_SLEEP_HOURS, value).apply()

    var todayHeartRate: String
        get() = prefs.getString(KEY_TODAY_HEART_RATE, "--") ?: "--"
        set(value) = prefs.edit().putString(KEY_TODAY_HEART_RATE, value).apply()

    var themeMode: AppThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
            return try {
                AppThemeMode.valueOf(name)
            } catch (e: Exception) {
                AppThemeMode.DARK
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var stepGoal: Int
        get() = prefs.getInt(KEY_STEP_GOAL, 10000)
        set(value) = prefs.edit().putInt(KEY_STEP_GOAL, value).apply()

    var waterGoalMl: Int
        get() = prefs.getInt(KEY_WATER_GOAL, 2500)
        set(value) = prefs.edit().putInt(KEY_WATER_GOAL, value).apply()

    var sleepGoalHours: Float
        get() = prefs.getFloat(KEY_SLEEP_GOAL, 8.0f)
        set(value) = prefs.edit().putFloat(KEY_SLEEP_GOAL, value).apply()

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_NAME, profile.name)
            .putString(KEY_USER_EMAIL, profile.email)
            .putInt(KEY_USER_AGE, profile.age)
            .putString(KEY_USER_GENDER, profile.gender)
            .putInt(KEY_USER_HEIGHT, profile.heightCm)
            .putFloat(KEY_USER_WEIGHT, profile.weightKg)
            .putString(KEY_USER_ACTIVITY_LEVEL, profile.activityLevel)
            .putString(KEY_USER_PREFERRED_WORKOUT, profile.preferredWorkout.name)
            .putBoolean(KEY_IS_SIGNED_IN, profile.isSignedIn)
            .putBoolean(KEY_IS_GUEST_TRIAL, profile.isGuestTrial)
            .apply()
    }

    fun loadUserProfile(): UserProfile {
        val preferredWorkoutName = prefs.getString(KEY_USER_PREFERRED_WORKOUT, ExerciseCategory.WALKING.name)
        val workout = try {
            ExerciseCategory.valueOf(preferredWorkoutName ?: ExerciseCategory.WALKING.name)
        } catch (e: Exception) {
            ExerciseCategory.WALKING
        }

        return UserProfile(
            name = prefs.getString(KEY_USER_NAME, "Alex Rivera") ?: "Alex Rivera",
            email = prefs.getString(KEY_USER_EMAIL, "alex.rivera@healthtrack.ai") ?: "alex.rivera@healthtrack.ai",
            age = prefs.getInt(KEY_USER_AGE, 24),
            gender = prefs.getString(KEY_USER_GENDER, "Prefer not to say") ?: "Prefer not to say",
            heightCm = prefs.getInt(KEY_USER_HEIGHT, 178),
            weightKg = prefs.getFloat(KEY_USER_WEIGHT, 70.5f),
            activityLevel = prefs.getString(KEY_USER_ACTIVITY_LEVEL, "Moderate") ?: "Moderate",
            preferredWorkout = workout,
            isSignedIn = prefs.getBoolean(KEY_IS_SIGNED_IN, true),
            isGuestTrial = prefs.getBoolean(KEY_IS_GUEST_TRIAL, false)
        )
    }

    fun saveAccounts(accounts: List<RegisteredAccount>) {
        val serialized = accounts.joinToString(";") { acc ->
            "${acc.name}|${acc.email}|${acc.passwordHash}|${acc.age}|${acc.gender}|${acc.heightCm}|${acc.weightKg}|${acc.activityLevel}"
        }
        prefs.edit().putString(KEY_REGISTERED_ACCOUNTS, serialized).apply()
    }

    fun loadAccounts(): List<RegisteredAccount> {
        val data = prefs.getString(KEY_REGISTERED_ACCOUNTS, null) ?: return emptyList()
        if (data.isBlank()) return emptyList()

        return data.split(";").mapNotNull { item ->
            val parts = item.split("|")
            if (parts.size >= 8) {
                RegisteredAccount(
                    name = parts[0],
                    email = parts[1],
                    passwordHash = parts[2],
                    age = parts[3].toIntOrNull() ?: 24,
                    gender = parts[4],
                    heightCm = parts[5].toIntOrNull() ?: 178,
                    weightKg = parts[6].toFloatOrNull() ?: 70.5f,
                    activityLevel = parts[7]
                )
            } else null
        }
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, false)
            .putBoolean(KEY_IS_GUEST_TRIAL, false)
            .apply()
    }
}
