package com.healthtrackai.app.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.healthtrackai.app.data.models.HealthStateHolder
import kotlin.math.sqrt

enum class StepSensorType(val displayName: String) {
    STEP_DETECTOR("Hardware Step Detector"),
    STEP_COUNTER("Hardware Step Counter"),
    ACCELEROMETER("Dynamic Accelerometer Pedometer"),
    NONE("No Sensor Available")
}

enum class StepSensitivity(val threshold: Float, val label: String) {
    HIGH(1.15f, "High (Hand / Pocket)"),
    NORMAL(1.35f, "Normal (Walking)"),
    LOW(1.65f, "Low (Vigorous / Running)")
}

/**
 * Native Android Sensor Tracker
 * Listens to device hardware step sensors (TYPE_STEP_DETECTOR, TYPE_STEP_COUNTER, and Accelerometer fallback)
 * and directly counts steps in real-time as the user walks.
 */
class StepSensorTracker(
    private val context: Context,
    private val healthState: HealthStateHolder
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var stepDetectorSensor: Sensor? = null
    private var stepCounterSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    var activeSensorType: StepSensorType = StepSensorType.NONE
        private set

    var isListening: Boolean = false
        private set

    var sensitivity: StepSensitivity = StepSensitivity.NORMAL

    // Cumulative step counter baseline
    private var initialCounterValue: Float? = null

    // Accelerometer peak detection variables
    private var lastMagnitude = 9.8f
    private var prevMagnitude = 9.8f
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 9.8f
    private val alpha = 0.82f // Low-pass filter coefficient for gravity isolation
    private var lastStepTimestampMs = 0L
    private val minStepIntervalMs = 260L // Debounce: max ~230 steps/min

    // Real-time Cadence calculation (steps in last 8 seconds)
    private val recentStepTimestamps = mutableListOf<Long>()

    // Optional listener for live walk sessions
    var onLiveStepCallback: ((stepIncrement: Int, cadenceSpm: Int) -> Unit)? = null

    init {
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        activeSensorType = when {
            stepDetectorSensor != null -> StepSensorType.STEP_DETECTOR
            stepCounterSensor != null -> StepSensorType.STEP_COUNTER
            accelerometerSensor != null -> StepSensorType.ACCELEROMETER
            else -> StepSensorType.NONE
        }

        healthState.activeSensorTypeDescription = activeSensorType.displayName
    }

    fun startListening() {
        if (isListening || sensorManager == null) return

        try {
            // Priority 1: Hardware Step Detector (fires 1.0f on each individual step)
            stepDetectorSensor?.let { sensor ->
                val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
                if (registered) {
                    isListening = true
                    activeSensorType = StepSensorType.STEP_DETECTOR
                    healthState.isSensorActive = true
                    healthState.activeSensorTypeDescription = activeSensorType.displayName
                    return
                }
            }

            // Priority 2: Hardware Step Counter (cumulative device steps)
            stepCounterSensor?.let { sensor ->
                val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                if (registered) {
                    isListening = true
                    activeSensorType = StepSensorType.STEP_COUNTER
                    healthState.isSensorActive = true
                    healthState.activeSensorTypeDescription = activeSensorType.displayName
                    return
                }
            }

            // Priority 3: 3-Axis Dynamic Accelerometer Peak Detection (Works on 100% of devices & emulators)
            accelerometerSensor?.let { sensor ->
                val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
                if (registered) {
                    isListening = true
                    activeSensorType = StepSensorType.ACCELEROMETER
                    healthState.isSensorActive = true
                    healthState.activeSensorTypeDescription = activeSensorType.displayName
                    return
                }
            }
        } catch (e: Throwable) {
            // Graceful sensor fallback
        }

        activeSensorType = StepSensorType.NONE
        healthState.isSensorActive = false
        healthState.activeSensorTypeDescription = "Sensor Ready"
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Throwable) { }
        isListening = false
        healthState.isSensorActive = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values.isNotEmpty() && event.values[0] == 1.0f) {
                    processStepIncrement(1)
                }
            }

            Sensor.TYPE_STEP_COUNTER -> {
                if (event.values.isNotEmpty()) {
                    val cumulativeSteps = event.values[0]
                    if (initialCounterValue == null) {
                        initialCounterValue = cumulativeSteps
                    } else {
                        val sessionSteps = (cumulativeSteps - (initialCounterValue ?: cumulativeSteps)).toInt()
                        if (sessionSteps > 0) {
                            processStepIncrement(sessionSteps)
                            initialCounterValue = cumulativeSteps
                        }
                    }
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (event.values.size >= 3) {
                    processAccelerometerData(event.values[0], event.values[1], event.values[2])
                }
            }
        }
    }

    private fun processAccelerometerData(x: Float, y: Float, z: Float) {
        // High-pass filter to remove static Earth gravity (9.8 m/s²)
        gravityX = alpha * gravityX + (1 - alpha) * x
        gravityY = alpha * gravityY + (1 - alpha) * y
        gravityZ = alpha * gravityZ + (1 - alpha) * z

        val dynX = x - gravityX
        val dynY = y - gravityY
        val dynZ = z - gravityZ

        // 3D dynamic acceleration vector magnitude
        val magnitude = sqrt(dynX * dynX + dynY * dynY + dynZ * dynZ)
        val now = System.currentTimeMillis()

        // Peak detection algorithm: local maximum that crosses the sensitivity threshold
        if (prevMagnitude > lastMagnitude &&
            prevMagnitude > magnitude &&
            prevMagnitude > sensitivity.threshold
        ) {
            val timeSinceLast = now - lastStepTimestampMs
            if (timeSinceLast >= minStepIntervalMs) {
                lastStepTimestampMs = now
                processStepIncrement(1)
            }
        }

        lastMagnitude = prevMagnitude
        prevMagnitude = magnitude
    }

    private fun processStepIncrement(increment: Int) {
        val now = System.currentTimeMillis()

        // Update cadence calculation (steps per minute)
        for (i in 0 until increment) {
            recentStepTimestamps.add(now)
        }
        // Retain steps from last 8 seconds
        val cutoff = now - 8000L
        recentStepTimestamps.removeAll { it < cutoff }

        val cadenceSpm = if (recentStepTimestamps.size >= 2) {
            val timeSpanSec = (now - recentStepTimestamps.first()) / 1000f
            if (timeSpanSec > 0f) {
                ((recentStepTimestamps.size / timeSpanSec) * 60f).toInt().coerceIn(0, 240)
            } else 0
        } else {
            0
        }

        healthState.sensorCadenceSpm = cadenceSpm

        // Directly increment daily steps
        healthState.addSteps(increment)

        // If in an active walk session, notify session listener
        onLiveStepCallback?.invoke(increment, cadenceSpm)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required for motion sensors
    }
}
