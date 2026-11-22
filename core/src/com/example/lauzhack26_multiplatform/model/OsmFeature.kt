package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

/**
 * Categorisation d'un obstacle issu des données OSM hors-ligne.
 */
enum class ObstacleType(
    val label: String,
    val icon: String
) {
    STAIRS("Escaliers", "🪜"),
    WHEELCHAIR_RESTRICTED("Accès limité", "⛔"),
    BAD_SURFACE("Surface impraticable", "⚠️"),
    BARRIER("Barrière bloquante", "🚧"),
    HIGH_KERB("Bordure haute", "🛑"),
    REVOLVING_DOOR("Porte à tambour", "🚪"),
    MISSING_TACTILE("Pas de bande podotactile", "🦯"),
    SILENT_CROSSING("Feu piéton muet", "👁️"),
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
