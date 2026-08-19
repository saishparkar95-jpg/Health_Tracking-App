package com.healthtrackai.app.data.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

class HealthConnectManager(private val context: Context) {

    private val tag = "HealthConnectManager"

    // The set of read permissions requested by HealthTrack AI
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class)
    )

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (isHealthConnectAvailable()) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.e(tag, "Failed to initialize HealthConnectClient", e)
            null
        }
    }

    /**
     * Check whether Health Connect is supported and available on this device.
     */
    fun getSdkStatus(): HealthConnectSdkStatus {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectSdkStatus.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectSdkStatus.PROVIDER_UPDATE_REQUIRED
                else -> HealthConnectSdkStatus.UNAVAILABLE
            }
        } catch (e: Throwable) {
            Log.w(tag, "HealthConnect getSdkStatus failed", e)
            HealthConnectSdkStatus.UNAVAILABLE
        }
    }

    fun isHealthConnectAvailable(): Boolean {
        return getSdkStatus() == HealthConnectSdkStatus.AVAILABLE
    }

    /**
     * Check which permissions have been granted by user.
     */
    suspend fun checkPermissionState(): HealthConnectPermissionState = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext HealthConnectPermissionState.NOT_GRANTED
        try {
            val granted = client.permissionController.getGrantedPermissions()
            when {
                granted.containsAll(permissions) -> HealthConnectPermissionState.ALL_GRANTED
                permissions.any { granted.contains(it) } -> HealthConnectPermissionState.PARTIALLY_GRANTED
                else -> HealthConnectPermissionState.NOT_GRANTED
            }
        } catch (e: Throwable) {
            Log.e(tag, "Error checking granted permissions", e)
            HealthConnectPermissionState.NOT_GRANTED
        }
    }

    /**
     * Get the set of currently granted permissions.
     */
    suspend fun getGrantedPermissions(): Set<String> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext emptySet()
        try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Throwable) {
            Log.e(tag, "Error getting granted permissions", e)
            emptySet()
        }
    }

    /**
     * Create intent to launch Google Play Store to install or update Health Connect provider.
     */
    fun getInstallOrUpdateIntent(): Intent {
        val playStorePackage = "com.google.android.apps.healthdata"
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$playStorePackage")
            setPackage("com.android.vending")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Read complete daily health record for a given date.
     */
    suspend fun readDailyRecord(date: LocalDate): DailyHealthRecord = withContext(Dispatchers.IO) {
        val zoneId = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zoneId).toInstant()
        val endOfDay = date.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
        val isToday = date == LocalDate.now()
        val dayOfWeek = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

        val client = healthConnectClient
        if (client == null) {
            return@withContext DailyHealthRecord(date = date, dayOfWeek = dayOfWeek, isToday = isToday)
        }

        var steps: Long? = null
        var distanceMeters: Double? = null
        var activeCalories: Double? = null
        var totalCalories: Double? = null
        var hydrationLiters: Double? = null
        var sleepSession: SleepSessionData? = null
        var sleepMinutes: Long? = null
        var heartRateSummary: HeartRateSummary? = null
        var weightKg: Double? = null
        var heightMeters: Double? = null
        var bodyFatPercent: Double? = null
        val exerciseSessions = mutableListOf<ExerciseRecordItem>()

        // 1. Read Aggregate Metrics (Steps, Distance, Calories, Hydration)
        try {
            val timeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            val aggregateResult = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        HydrationRecord.VOLUME_TOTAL
                    ),
                    timeRangeFilter = timeFilter
                )
            )

            steps = aggregateResult[StepsRecord.COUNT_TOTAL]
            distanceMeters = aggregateResult[DistanceRecord.DISTANCE_TOTAL]?.inMeters
            activeCalories = aggregateResult[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
            totalCalories = aggregateResult[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            hydrationLiters = aggregateResult[HydrationRecord.VOLUME_TOTAL]?.inLiters
        } catch (e: Throwable) {
            Log.w(tag, "Aggregate query failed for date $date: ${e.message}")
        }

        // 2. Read Sleep Sessions
        try {
            // Check previous night sleep window (e.g. from 6 PM previous day to 6 PM target day)
            val sleepWindowStart = date.minusDays(1).atTime(18, 0).atZone(zoneId).toInstant()
            val sleepWindowEnd = date.atTime(18, 0).atZone(zoneId).toInstant()
            val sleepResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(sleepWindowStart, sleepWindowEnd)
                )
            )

            val session = sleepResponse.records.maxByOrNull { it.endTime.toEpochMilli() - it.startTime.toEpochMilli() }
            if (session != null) {
                val totalMins = ChronoUnit.MINUTES.between(session.startTime, session.endTime)
                val bedtimeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zoneId).format(session.startTime)
                val wakeTimeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zoneId).format(session.endTime)

                val stageSegments = session.stages.map { stage ->
                    val stageType = when (stage.stage) {
                        SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStageType.AWAKE
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
                        SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStageType.DEEP
                        SleepSessionRecord.STAGE_TYPE_REM -> SleepStageType.REM
                        else -> SleepStageType.UNKNOWN
                    }
                    val dur = ChronoUnit.MINUTES.between(stage.startTime, stage.endTime)
                    SleepStageSegment(stageType, stage.startTime, stage.endTime, dur)
                }

                val deepMins = stageSegments.filter { it.stage == SleepStageType.DEEP }.sumOf { it.durationMinutes }
                val lightMins = stageSegments.filter { it.stage == SleepStageType.LIGHT }.sumOf { it.durationMinutes }
                val remMins = stageSegments.filter { it.stage == SleepStageType.REM }.sumOf { it.durationMinutes }
                val awakeMins = stageSegments.filter { it.stage == SleepStageType.AWAKE }.sumOf { it.durationMinutes }

                val sourcePackage = session.metadata.dataOrigin.packageName
                val sourceLabel = formatSourceAppName(sourcePackage)

                sleepSession = SleepSessionData(
                    sessionId = session.metadata.id,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    totalMinutes = totalMins,
                    bedtimeFormatted = bedtimeFmt,
                    wakeTimeFormatted = wakeTimeFmt,
                    stages = stageSegments,
                    deepSleepMinutes = deepMins,
                    lightSleepMinutes = lightMins,
                    remSleepMinutes = remMins,
                    awakeMinutes = awakeMins,
                    sourceName = sourceLabel
                )
                sleepMinutes = totalMins
            }
        } catch (e: Throwable) {
            Log.w(tag, "Sleep read failed for date $date: ${e.message}")
        }

        // 3. Read Heart Rate
        try {
            val hrResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )

            if (hrResponse.records.isNotEmpty()) {
                val allSamples = mutableListOf<HeartRatePoint>()
                var sourcePkg = "Health Connect"
                hrResponse.records.forEach { record ->
                    sourcePkg = record.metadata.dataOrigin.packageName
                    record.samples.forEach { sample ->
                        allSamples.add(HeartRatePoint(sample.time, sample.beatsPerMinute))
                    }
                }

                if (allSamples.isNotEmpty()) {
                    val latest = allSamples.maxByOrNull { it.time }?.bpm
                    val min = allSamples.minOfOrNull { it.bpm }
                    val max = allSamples.maxOfOrNull { it.bpm }

                    // Also check resting heart rate
                    var restingBpm: Long? = null
                    try {
                        val restingResponse = client.readRecords(
                            ReadRecordsRequest(
                                recordType = RestingHeartRateRecord::class,
                                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                            )
                        )
                        restingBpm = restingResponse.records.lastOrNull()?.beatsPerMinute
                    } catch (e: Throwable) {
                        Log.w(tag, "Resting HR read failed: ${e.message}")
                    }

                    heartRateSummary = HeartRateSummary(
                        latestBpm = latest,
                        minBpm = min,
                        maxBpm = max,
                        restingBpm = restingBpm ?: (min?.let { (it + 5).coerceAtLeast(45) }),
                        sampleCount = allSamples.size,
                        samples = allSamples.sortedBy { it.time },
                        sourceName = formatSourceAppName(sourcePkg)
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "Heart rate read failed for date $date: ${e.message}")
        }

        // 4. Read Exercise Sessions
        try {
            val exerciseResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )

            exerciseResponse.records.forEach { session ->
                val duration = ChronoUnit.MINUTES.between(session.startTime, session.endTime)
                val typeName = getExerciseTypeName(session.exerciseType)
                val emoji = getExerciseEmoji(session.exerciseType)
                val srcLabel = formatSourceAppName(session.metadata.dataOrigin.packageName)

                exerciseSessions.add(
                    ExerciseRecordItem(
                        id = session.metadata.id,
                        exerciseType = typeName,
                        title = session.title ?: typeName,
                        iconEmoji = emoji,
                        startTime = session.startTime,
                        endTime = session.endTime,
                        durationMinutes = duration,
                        sourceName = srcLabel
                    )
                )
            }
        } catch (e: Throwable) {
            Log.w(tag, "Exercise read failed for date $date: ${e.message}")
        }

        // 5. Read Weight & Body Measurements
        try {
            val weightResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(date.minusDays(30).atStartOfDay(zoneId).toInstant(), endOfDay)
                )
            )
            weightKg = weightResponse.records.lastOrNull()?.weight?.inKilograms

            val heightResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(date.minusDays(90).atStartOfDay(zoneId).toInstant(), endOfDay)
                )
            )
            heightMeters = heightResponse.records.lastOrNull()?.height?.inMeters

            val bodyFatResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(date.minusDays(30).atStartOfDay(zoneId).toInstant(), endOfDay)
                )
            )
            bodyFatPercent = bodyFatResponse.records.lastOrNull()?.percentage?.value
        } catch (e: Throwable) {
            Log.w(tag, "Body measurements read failed: ${e.message}")
        }

        val activeMins = exerciseSessions.sumOf { it.durationMinutes }

        DailyHealthRecord(
            date = date,
            dayOfWeek = dayOfWeek,
            steps = steps,
            distanceMeters = distanceMeters,
            activeCaloriesKcal = activeCalories,
            totalCaloriesKcal = totalCalories,
            activeMinutes = if (activeMins > 0) activeMins else null,
            hydrationLiters = hydrationLiters,
            sleepMinutes = sleepMinutes,
            sleepSession = sleepSession,
            heartRateSummary = heartRateSummary,
            weightKg = weightKg,
            heightMeters = heightMeters,
            bodyFatPercent = bodyFatPercent,
            exerciseSessions = exerciseSessions,
            isToday = isToday
        )
    }

    /**
     * Read the past N days of health records (e.g. 7 days for weekly trends).
     */
    suspend fun readPastNDays(days: Int = 7): List<DailyHealthRecord> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val records = mutableListOf<DailyHealthRecord>()
        for (i in (days - 1) downTo 0) {
            val date = today.minusDays(i.toLong())
            val record = readDailyRecord(date)
            records.add(record)
        }
        records
    }

    /**
     * Detect sources connected for transparency screen.
     */
    suspend fun inspectDataSources(): List<MetricSourceInfo> = withContext(Dispatchers.IO) {
        val nowFmt = DateTimeFormatter.ofPattern("h:mm a").format(LocalTime.now())
        val todayRecord = readDailyRecord(LocalDate.now())

        listOf(
            MetricSourceInfo(
                metricName = "Steps & Distance",
                isAvailable = todayRecord.steps != null && todayRecord.steps > 0,
                sourceApp = if (todayRecord.steps != null) "Health Connect / Device Sensors" else "No active source",
                lastSyncTime = if (todayRecord.steps != null) "Synced today at $nowFmt" else "No recent data",
                iconEmoji = "🚶",
                note = "Aggregated automatically from device hardware pedometer and companion wearables."
            ),
            MetricSourceInfo(
                metricName = "Sleep & Rest",
                isAvailable = todayRecord.sleepSession != null,
                sourceApp = todayRecord.sleepSession?.sourceName ?: "No connected sleep tracker",
                lastSyncTime = if (todayRecord.sleepSession != null) "Synced today at $nowFmt" else "No recent data",
                iconEmoji = "😴",
                note = if (todayRecord.sleepSession?.hasStages == true) "Sleep stages recorded (Awake, Light, Deep, REM)." else "Total duration recorded without sleep stages."
            ),
            MetricSourceInfo(
                metricName = "Heart Rate & Pulse",
                isAvailable = todayRecord.heartRateSummary?.latestBpm != null,
                sourceApp = todayRecord.heartRateSummary?.sourceName ?: "No connected heart sensor",
                lastSyncTime = if (todayRecord.heartRateSummary != null) "Synced today at $nowFmt" else "No recent data",
                iconEmoji = "❤️",
                note = "Continuous or spot BPM from paired wearable / optical pulse sensor."
            ),
            MetricSourceInfo(
                metricName = "Workouts & Activity",
                isAvailable = todayRecord.exerciseSessions.isNotEmpty(),
                sourceApp = if (todayRecord.exerciseSessions.isNotEmpty()) todayRecord.exerciseSessions.first().sourceName else "Health Connect",
                lastSyncTime = if (todayRecord.exerciseSessions.isNotEmpty()) "Synced today at $nowFmt" else "No sessions recorded today",
                iconEmoji = "⚡",
                note = "GPS tracked runs, cycles, strength, and outdoor walking sessions."
            ),
            MetricSourceInfo(
                metricName = "Hydration Intake",
                isAvailable = todayRecord.hydrationLiters != null && todayRecord.hydrationLiters > 0.0,
                sourceApp = if (todayRecord.hydrationLiters != null) "Connected Health Source" else "No connected source",
                lastSyncTime = if (todayRecord.hydrationLiters != null) "Synced today at $nowFmt" else "Unavailable",
                iconEmoji = "💧",
                note = "Water intake from connected hydration apps or Bluetooth smart bottles."
            ),
            MetricSourceInfo(
                metricName = "Body Composition & Weight",
                isAvailable = todayRecord.weightKg != null,
                sourceApp = if (todayRecord.weightKg != null) "Smart Scale / Health Connect" else "Health Connect",
                lastSyncTime = if (todayRecord.weightKg != null) "Synced recently" else "No scale connected",
                iconEmoji = "⚖️",
                note = "Body weight, BMI calculation, and optional body fat percentage."
            )
        )
    }

    private fun formatSourceAppName(packageName: String): String {
        return when {
            packageName.contains("fit", ignoreCase = true) -> "Google Fit"
            packageName.contains("shealth", ignoreCase = true) || packageName.contains("samsung", ignoreCase = true) -> "Samsung Health"
            packageName.contains("garmin", ignoreCase = true) -> "Garmin Connect"
            packageName.contains("oura", ignoreCase = true) -> "Oura Ring"
            packageName.contains("whoop", ignoreCase = true) -> "Whoop"
            packageName.contains("fitbit", ignoreCase = true) -> "Fitbit"
            packageName.contains("healthtrackai", ignoreCase = true) -> "HealthTrack AI"
            packageName.contains("healthdata", ignoreCase = true) -> "Health Connect"
            packageName.isBlank() -> "Health Connect"
            else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }

    private fun getExerciseTypeName(type: Int): String {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength Training"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT Workout"
            else -> "Exercise Session"
        }
    }

    private fun getExerciseEmoji(type: Int): String {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "🏃"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "🚶"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "🚴"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "🏋️"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "🧘"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "🏊"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "🥾"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "🔥"
            else -> "⚡"
        }
    }
}
