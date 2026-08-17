package com.healthtrackai.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.healthtrackai.app.MainActivity

/**
 * Manages system notification channels, persistent live step tracking notifications,
 * and smart health & hydration reminder notifications.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        const val CHANNEL_STEPS_ID = "healthtrack_steps_live_channel"
        const val CHANNEL_REMINDERS_ID = "healthtrack_reminders_channel"
        const val NOTIFICATION_ID_STEPS = 1001
        const val NOTIFICATION_ID_HYDRATION = 1002
        const val NOTIFICATION_ID_INACTIVITY = 1003
        const val NOTIFICATION_ID_BEDTIME = 1004
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
                val stepChannel = NotificationChannel(
                    CHANNEL_STEPS_ID,
                    "Live Step Tracking",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows real-time live daily step count and workout progress"
                    setShowBadge(false)
                }

                val reminderChannel = NotificationChannel(
                    CHANNEL_REMINDERS_ID,
                    "Health & Hydration Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts for hourly activity, water intake, and bedtime schedules"
                }

                notificationManager.createNotificationChannel(stepChannel)
                notificationManager.createNotificationChannel(reminderChannel)
            }
        } catch (e: Throwable) {
            // Ignore channel creation errors gracefully
        }
    }

    /**
     * Updates persistent notification widget with live steps and progress bar.
     */
    fun showStepNotification(currentSteps: Int, stepGoal: Int, distanceKm: Float, calories: Int) {
        if (notificationManager == null) return
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val progressPct = if (stepGoal > 0) ((currentSteps.toFloat() / stepGoal) * 100).toInt().coerceIn(0, 100) else 0

            val builder = NotificationCompat.Builder(context, CHANNEL_STEPS_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("👟 $currentSteps / $stepGoal steps ($progressPct%)")
                .setContentText("${String.format("%.2f", distanceKm)} km walked • $calories kcal burned today")
                .setProgress(100, progressPct, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            notificationManager.notify(NOTIFICATION_ID_STEPS, builder.build())
        } catch (e: Throwable) {
            // Safe fallback if notifications are blocked
        }
    }

    fun showHydrationAlert(currentMl: Int, goalMl: Int) {
        if (notificationManager == null) return
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("💧 Time to Hydrate!")
                .setContentText("Drink a glass of water to keep energy high ($currentMl / $goalMl ml).")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            notificationManager.notify(NOTIFICATION_ID_HYDRATION, builder.build())
        } catch (e: Throwable) { }
    }

    fun showInactivityAlert() {
        if (notificationManager == null) return
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚶 Time to Stretch & Walk!")
                .setContentText("You've been sitting for a while. Take a quick 250-step walk around.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            notificationManager.notify(NOTIFICATION_ID_INACTIVITY, builder.build())
        } catch (e: Throwable) { }
    }

    fun showBedtimeAlert(targetBedtime: String) {
        if (notificationManager == null) return
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                3,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🌙 Wind-down for Bedtime")
                .setContentText("Your target bedtime is $targetBedtime. Dim screens to ensure 8h restful sleep.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            notificationManager.notify(NOTIFICATION_ID_BEDTIME, builder.build())
        } catch (e: Throwable) { }
    }
}
