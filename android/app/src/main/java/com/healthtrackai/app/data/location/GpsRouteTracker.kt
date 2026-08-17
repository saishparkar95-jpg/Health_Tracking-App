package com.healthtrackai.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlin.math.*

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val speedKmh: Float = 0f,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * GPS Outdoor Route Tracker
 * Listens to device GPS / Network location providers and records live route waypoints,
 * speed, elevation gain, and split distances.
 */
class GpsRouteTracker(
    private val context: Context
) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    var isTracking: Boolean = false
        private set

    val routePoints = mutableListOf<RoutePoint>()

    var totalDistanceMeters: Float = 0f
        private set

    var currentSpeedKmh: Float = 0f
        private set

    var maxSpeedKmh: Float = 0f
        private set

    var elevationGainMeters: Float = 0f
        private set

    private var lastLocation: Location? = null
    var onLocationUpdated: ((point: RoutePoint, totalDistanceKm: Float, speedKmh: Float) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking || locationManager == null) return

        routePoints.clear()
        totalDistanceMeters = 0f
        currentSpeedKmh = 0f
        maxSpeedKmh = 0f
        elevationGainMeters = 0f
        lastLocation = null

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L, // 2 seconds
                    2.0f,  // 2 meters
                    this
                )
                isTracking = true
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    2.0f,
                    this
                )
                isTracking = true
            }
        } catch (e: Throwable) {
            isTracking = false
        }
    }

    fun stopTracking() {
        if (!isTracking || locationManager == null) return
        try {
            locationManager.removeUpdates(this)
        } catch (e: Throwable) { }
        isTracking = false
        currentSpeedKmh = 0f
    }

    override fun onLocationChanged(location: Location) {
        try {
            val speedKmh = (location.speed * 3.6f).coerceAtLeast(0f)
            currentSpeedKmh = speedKmh
            if (speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh

            val point = RoutePoint(
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = location.altitude,
                speedKmh = speedKmh,
                timestampMs = location.time
            )

            lastLocation?.let { prev ->
                val dist = prev.distanceTo(location)
                if (dist in 1.0f..200.0f) { // filter out GPS jitter
                    totalDistanceMeters += dist
                    val altDiff = (location.altitude - prev.altitude).toFloat()
                    if (altDiff > 0.5f) elevationGainMeters += altDiff
                }
            }

            lastLocation = location
            routePoints.add(point)

            val totalDistKm = totalDistanceMeters / 1000f
            onLocationUpdated?.invoke(point, totalDistKm, speedKmh)
        } catch (e: Throwable) { }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    fun generateSimulatedWaypoint(progressFraction: Float): RoutePoint {
        val baseLat = 37.7749
        val baseLng = -122.4194
        val radius = 0.0035

        val angle = progressFraction * 2.0 * Math.PI
        val lat = baseLat + radius * sin(angle)
        val lng = baseLng + radius * cos(angle) * 1.3
        val speed = 4.8f + (sin(angle * 3) * 0.8f).toFloat()

        val point = RoutePoint(
            latitude = lat,
            longitude = lng,
            altitudeMeters = 15.0 + sin(angle) * 5.0,
            speedKmh = speed
        )
        routePoints.add(point)
        totalDistanceMeters += 2.2f
        currentSpeedKmh = speed
        return point
    }
}
