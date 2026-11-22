package com.example.lauzhack26_multiplatform.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location as AndroidLocation
import androidx.core.content.ContextCompat
import com.example.lauzhack26_multiplatform.model.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Holds the Android application context so that [createLocationTracker] can create
 * a [LocationTracker] without requiring an explicit context parameter.
 * Set by [MainActivity] before the shared code runs.
 */
var androidApplicationContext: Context? = null

actual class LocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    actual suspend fun getCurrentLocation(): Location? {
        // Check if location permissions are granted
        val fineLocation = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocation != PackageManager.PERMISSION_GRANTED &&
            coarseLocation != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return try {
            suspendCancellableCoroutine<Location?> { continuation ->
                fusedLocationClient.lastLocation.addOnSuccessListener { androidLocation: AndroidLocation? ->
                    if (!continuation.isCompleted) {
                        continuation.resumeWith(
                            Result.success(androidLocation?.let { Location(it.latitude, it.longitude) })
                        )
                    }
                }.addOnFailureListener {
                    if (!continuation.isCompleted) {
                        continuation.resumeWith(Result.success(null))
                    }
                }

                continuation.invokeOnCancellation {
                    if (!continuation.isCompleted) {
                        continuation.resumeWith(Result.success(null))
                    }
                }
            }
        } catch (_: SecurityException) {
            null
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

actual fun createLocationTracker(): LocationTracker? {
    val ctx = androidApplicationContext ?: return null
    return LocationTracker(ctx)
}
