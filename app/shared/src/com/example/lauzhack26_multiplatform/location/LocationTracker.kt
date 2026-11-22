package com.example.lauzhack26_multiplatform.location

import com.example.lauzhack26_multiplatform.model.Location

/**
 * Abstraction for device location tracking across platforms.
 */
expect class LocationTracker {

    /**
     * Returns the current device location, or null if unavailable or permission denied.
     */
    suspend fun getCurrentLocation(): Location?
}

/**
 * Creates a platform-specific LocationTracker instance.
 * Returns null if the platform cannot provide location services.
 */
expect fun createLocationTracker(): LocationTracker?
