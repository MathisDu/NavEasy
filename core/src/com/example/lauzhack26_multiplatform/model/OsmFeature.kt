package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

/**
 * Represents a feature from OpenStreetMap (e.g., stairs, ramps).
 */
@Serializable
data class OsmFeature(
    val location: Location,
    val type: String,
    val description: String
)
