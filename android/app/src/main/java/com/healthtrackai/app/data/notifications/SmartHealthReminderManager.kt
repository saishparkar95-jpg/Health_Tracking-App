package com.healthtrackai.app.data.notifications

import android.content.Context
import com.healthtrackai.app.data.models.HealthStateHolder

/**
 * Manages smart background reminders for:
 * - Hourly inactivity (sedentary alerts)
 * - Daily hydration intervals
 * - Bedtime wind-down schedules
 */
class SmartHealthReminderManager(
    private val context: Context,
    private val healthState: HealthStateHolder
) {
    private val notificationHelper by lazy {
        try {
            NotificationHelper(context)
        } catch (e: Throwable) {
            null
        }
    }

    fun triggerHydrationReminderNow() {
        try {
            notificationHelper?.showHydrationAlert(
                currentMl = healthState.currentWaterMl,
                goalMl = healthState.waterGoalMl
            )
        } catch (e: Throwable) { }
    }

    fun triggerInactivityReminderNow() {
        try {
            notificationHelper?.showInactivityAlert()
        } catch (e: Throwable) { }
    }

    fun triggerBedtimeReminderNow() {
        try {
            notificationHelper?.showBedtimeAlert(
                targetBedtime = healthState.sleepBedtime
            )
        } catch (e: Throwable) { }
    }

    fun syncLiveStepNotification() {
        try {
            notificationHelper?.showStepNotification(
                currentSteps = healthState.currentSteps,
                stepGoal = healthState.stepGoal,
                distanceKm = healthState.distanceKm,
                calories = healthState.caloriesBurned
            )
        } catch (e: Throwable) { }
    }
}
