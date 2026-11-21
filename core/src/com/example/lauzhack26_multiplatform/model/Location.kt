package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val lat: Double,
    val lon: Double
)
