package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

/**
 * Categorisation d'un obstacle issu des données OSM hors-ligne.
 */
enum class ObstacleType(
    val label: String,
    val icon: String
) {
    STAIRS("Escalier", "🪜"),
    COBBLESTONE("Pavés", "🧱"),
    KERB("Bordure", "📐")
}

/**
 * Represents an obstacle loaded from the offline GeoJSON resource.
 */
@Serializable
data class OsmFeature(
    val location: Location,
    val obstacleType: ObstacleType,
    val description: String
)
