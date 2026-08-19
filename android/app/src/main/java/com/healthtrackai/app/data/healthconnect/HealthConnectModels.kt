package com.healthtrackai.app.data.healthconnect

import java.time.Instant
import java.time.LocalDate

/**
 * Availability status of the Android Health Connect SDK on this device.
 */
enum class HealthConnectSdkStatus {
    AVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    UNAVAILABLE,
    CHECKING
}

/**
 * Health Connect permission authorization status.
 */
enum class HealthConnectPermissionState {
    ALL_GRANTED,
    PARTIALLY_GRANTED,
    NOT_GRANTED,
    CHECKING
}

/**
 * Sleep stage classification
 */
enum class SleepStageType(val label: String, val colorHex: Long) {
    AWAKE("Awake", 0xFFFFB74D),
    LIGHT("Light Sleep", 0xFF64B5F6),
    DEEP("Deep Sleep", 0xFF283593),
    REM("REM", 0xFFBA68C8),
    UNKNOWN("Rest", 0xFF90A4AE)
}

data class SleepStageSegment(
    val stage: SleepStageType,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long
)

data class SleepSessionData(
    val sessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val totalMinutes: Long,
    val bedtimeFormatted: String,
    val wakeTimeFormatted: String,
    val stages: List<SleepStageSegment> = emptyList(),
    val deepSleepMinutes: Long = 0,
    val lightSleepMinutes: Long = 0,
    val remSleepMinutes: Long = 0,
    val awakeMinutes: Long = 0,
    val sourceName: String = "Health Connect"
) {
    val hasStages: Boolean get() = stages.isNotEmpty()
}

data class ExerciseRecordItem(
    val id: String,
    val exerciseType: String,
    val title: String,
    val iconEmoji: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long,
    val caloriesBurnedKcal: Double? = null,
    val distanceMeters: Double? = null,
    val sourceName: String = "Health Connect"
)

data class HeartRatePoint(
    val time: Instant,
    val bpm: Long
)

data class HeartRateSummary(
    val latestBpm: Long? = null,
    val minBpm: Long? = null,
    val maxBpm: Long? = null,
    val restingBpm: Long? = null,
    val sampleCount: Int = 0,
    val samples: List<HeartRatePoint> = emptyList(),
    val sourceName: String = "Health Connect"
)

data class DailyHealthRecord(
    val date: LocalDate,
    val dayOfWeek: String,
    val steps: Long? = null,
    val distanceMeters: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val totalCaloriesKcal: Double? = null,
    val activeMinutes: Long? = null,
    val hydrationLiters: Double? = null,
    val sleepMinutes: Long? = null,
    val sleepSession: SleepSessionData? = null,
    val heartRateSummary: HeartRateSummary? = null,
    val weightKg: Double? = null,
    val heightMeters: Double? = null,
    val bodyFatPercent: Double? = null,
    val exerciseSessions: List<ExerciseRecordItem> = emptyList(),
    val isToday: Boolean = false
) {
    val distanceKm: Float? get() = distanceMeters?.let { (it / 1000f).toFloat() }
    val sleepHoursFormatted: String get() {
        val mins = sleepMinutes ?: return "Not available"
        val h = mins / 60
        val m = mins % 60
        return "${h}h ${m}m"
    }
}

data class MetricSourceInfo(
    val metricName: String,
    val isAvailable: Boolean,
    val sourceApp: String,
    val lastSyncTime: String,
    val iconEmoji: String,
    val note: String = ""
)
