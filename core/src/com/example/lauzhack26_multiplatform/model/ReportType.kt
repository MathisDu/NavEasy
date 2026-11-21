package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReportType(
    val label: String
) {
    STAIRS("Escaliers / Marches"),
    STEPS("Paliers"),
    CURB("Trottoir sans accotement"),
    CONSTRUCTION("Travaux sur trottoir"),
    NARROW_PASSAGE("Passage étroit"),
    FLOORS("Revêtement instable"),
    UNPAVED_SURFACE("Surface non pavée"),
    POOR_LIGHTING("Éclairage insuffisant"),
    RAMP("Rampe d'accès"),
    OTHER("Autre")
}
