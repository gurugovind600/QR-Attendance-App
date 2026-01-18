package com.attendanceapp.myapplication.utils

import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

object LocationHelper {

    // Target Location (e.g., Your College/Office coordinates)
    // You can get these from Google Maps (Right click -> "What's here?")
    private const val TARGET_LAT = 25.868360 // Example: New Delhi
    private const val TARGET_LNG = 85.286882
    private const val ALLOWED_RADIUS_METERS = 100.0 // 100 meters range

    fun getCurrentLocation(context: Context): Task<Location> {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            return fusedLocationClient.lastLocation
        } catch (e: SecurityException) {
            throw e
        }
    }

    fun isWithinRange(currentLocation: Location): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            TARGET_LAT,
            TARGET_LNG,
            results
        )
        val distanceInMeters = results[0]
        return distanceInMeters <= ALLOWED_RADIUS_METERS
    }
}
