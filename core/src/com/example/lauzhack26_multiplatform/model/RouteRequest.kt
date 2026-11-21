package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class RouteRequest(
    val start: Location,
    val end: Location,
    val profile: MobilityProfile
)
