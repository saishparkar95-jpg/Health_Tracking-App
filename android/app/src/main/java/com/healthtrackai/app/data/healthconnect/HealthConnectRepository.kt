package com.healthtrackai.app.data.healthconnect

import android.content.Context
import android.util.Log
import com.healthtrackai.app.data.services.AiWellnessAssistantEngine
import com.healthtrackai.app.data.services.HealthScoreEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HealthConnectRepository(
    private val context: Context,
    val manager: HealthConnectManager = HealthConnectManager(context)
) {
    private val tag = "HealthConnectRepo"

    /**
     * Perform complete health data synchronization.
     * Updates [state] with today's readings, 7-day history, sleep sessions,
     * heart rate metrics, workouts, data sources, and triggers health score/AI updates.
     */
    suspend fun syncHealthData(state: com.healthtrackai.app.data.models.HealthStateHolder): Boolean = withContext(Dispatchers.IO) {
        state.isSyncingHealthConnect = true
        state.healthConnectErrorMessage = null

        try {
            // 1. Check SDK Status
            val sdkStatus = manager.getSdkStatus()
            state.healthConnectSdkStatus = sdkStatus

            if (sdkStatus != HealthConnectSdkStatus.AVAILABLE) {
                state.isSyncingHealthConnect = false
                if (sdkStatus == HealthConnectSdkStatus.PROVIDER_UPDATE_REQUIRED) {
                    state.healthConnectErrorMessage = "Health Connect provider update required on this device."
                } else {
                    state.healthConnectErrorMessage = "Health Connect is not available on this device."
                }
                return@withContext false
            }

            // 2. Check Permissions
            val permState = manager.checkPermissionState()
            state.healthConnectPermissionState = permState

            // 3. Read Today's Health Data
            val todayRecord = manager.readDailyRecord(LocalDate.now())
            state.todayHealthRecord = todayRecord

            // Update state fields from Health Connect if available
            todayRecord.steps?.let { hcSteps ->
                state.currentSteps = hcSteps.toInt()
            }

            todayRecord.sleepMinutes?.let { mins ->
                val hoursFloat = mins.toFloat() / 60.0f
                state.sleepHours = hoursFloat
            }

            todayRecord.sleepSession?.let { session ->
                state.sleepBedtime = session.bedtimeFormatted
                state.sleepWakeTime = session.wakeTimeFormatted
                state.latestSleepSession = session
            }

            todayRecord.heartRateSummary?.let { hrSummary ->
                state.latestHeartRateSummary = hrSummary
                hrSummary.latestBpm?.let { bpm ->
                    state.heartRateBpm = bpm.toString()
                }
            }

            todayRecord.hydrationLiters?.let { liters ->
                state.currentWaterMl = (liters * 1000.0).toInt()
                state.isHydrationSourceConnected = true
            } ?: run {
                state.isHydrationSourceConnected = false
            }

            todayRecord.weightKg?.let { weight ->
                state.currentWeightKg = weight.toFloat()
            }

            todayRecord.activeMinutes?.let { actMins ->
                state.activeMinutes = actMins.toInt()
            }

            if (todayRecord.exerciseSessions.isNotEmpty()) {
                state.healthConnectWorkouts.clear()
                state.healthConnectWorkouts.addAll(todayRecord.exerciseSessions)
            }

            // 4. Read 7-Day History
            val past7Days = manager.readPastNDays(7)
            state.historical7Days.clear()
            state.historical7Days.addAll(past7Days)

            // Update weeklyLogs for charts
            val newLogs = past7Days.map { record ->
                com.healthtrackai.app.data.models.DayLog(
                    day = record.dayOfWeek,
                    steps = record.steps?.toInt() ?: 0,
                    goal = state.stepGoal,
                    isToday = record.isToday
                )
            }
            if (newLogs.isNotEmpty()) {
                state.weeklyLogs.clear()
                state.weeklyLogs.addAll(newLogs)
            }

            // 5. Inspect Data Sources
            val sources = manager.inspectDataSources()
            state.connectedDataSources.clear()
            state.connectedDataSources.addAll(sources)

            // 6. Recalculate Health Score & AI Summaries
            val scoreResult = HealthScoreEngine.calculateScore(state, todayRecord, past7Days)
            state.currentHealthScoreResult = scoreResult

            val dailySummary = AiWellnessAssistantEngine.generateDailySummary(state, todayRecord, scoreResult)
            state.todayAiDailySummary = dailySummary

            val weeklyReport = AiWellnessAssistantEngine.generateWeeklyReport(state, past7Days)
            state.weeklyAiReport = weeklyReport

            val timeFmt = DateTimeFormatter.ofPattern("h:mm a").format(java.time.LocalTime.now())
            state.lastHealthConnectSyncTime = "Synced at $timeFmt"
            state.isSyncingHealthConnect = false
            return@withContext true

        } catch (e: Throwable) {
            Log.e(tag, "Health Connect sync failed", e)
            state.isSyncingHealthConnect = false
            state.healthConnectErrorMessage = "Some health data is unavailable on this device."
            return@withContext false
        }
    }
}
