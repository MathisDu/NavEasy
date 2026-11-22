package com.example.lauzhack26_multiplatform.location

import com.example.lauzhack26_multiplatform.model.Location

actual class LocationTracker {

    actual suspend fun getCurrentLocation(): Location? {
        // TODO: Implement Android location using FusedLocationProviderClient
        return null
    }
}

actual fun createLocationTracker(): LocationTracker = LocationTracker()
