package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReportType(
    val label: String
) {
    STAIRS("Stairs / Steps"),
    STEPS("Landings"),
    CURB("Curb without ramp"),
    CONSTRUCTION("Sidewalk construction"),
    NARROW_PASSAGE("Narrow passage"),
    FLOORS("Unstable surface"),
    UNPAVED_SURFACE("Unpaved surface"),
    POOR_LIGHTING("Poor lighting"),
    RAMP("Access ramp"),
    OTHER("Other")
}
