package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

/**
 * Categorization of an obstacle from offline OSM data.
 */
enum class ObstacleType(
    val label: String,
    val icon: String
) {
    STAIRS("Stairs", "🪜"),
    WHEELCHAIR_RESTRICTED("Limited access", "⛔"),
    BAD_SURFACE("Impassable surface", "⚠️"),
    BARRIER("Blocking barrier", "🚧"),
    HIGH_KERB("High kerb", "🛑"),
    REVOLVING_DOOR("Revolving door", "🚪"),
    MISSING_TACTILE("No tactile paving", "🦯"),
    SILENT_CROSSING("Silent crossing", "👁️"),
    COBBLESTONE("Cobblestone", "🧱"),
    KERB("Kerb", "📐")
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
