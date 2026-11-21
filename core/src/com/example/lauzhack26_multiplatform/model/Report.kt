package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String,
    val type: ReportType,
    val description: String,
    val location: Location
)
