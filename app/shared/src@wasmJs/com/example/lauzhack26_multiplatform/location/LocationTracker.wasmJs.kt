package com.example.lauzhack26_multiplatform.location

import com.example.lauzhack26_multiplatform.model.Location
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

/**
 * Top-level JS function that wraps navigator.geolocation.getCurrentPosition.
 * Takes a success callback (lat, lon) and an error callback, then invokes the browser API.
 */
@OptIn(ExperimentalWasmJsInterop::class)
private fun requestGeolocationPosition(
    onSuccess: (Double, Double) -> Unit,
    onError: () -> Unit
): Unit = js("""
    {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                onSuccess(position.coords.latitude, position.coords.longitude);
            },
            function(error) {
                onError();
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    }
""")

actual class LocationTracker {

    actual suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        requestGeolocationPosition(
            onSuccess = { lat, lon ->
                if (!continuation.isCompleted) {
                    continuation.resume(Location(lat = lat, lon = lon))
                }
            },
            onError = {
                if (!continuation.isCompleted) {
                    continuation.resume(null)
                }
            }
        )

        continuation.invokeOnCancellation {
            if (!continuation.isCompleted) {
                continuation.resume(null)
            }
        }
    }
}

actual fun createLocationTracker(): LocationTracker = LocationTracker()
