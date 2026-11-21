package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class RouteResponse(
    val coordinates: List<Location>,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Double = 0.0
)
