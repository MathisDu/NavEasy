package com.example.lauzhack26_multiplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String,
    val userId: String?,
    val title: String,
    val description: String,
    val category: String,
    val location: Location,
    val disabilityTypes: List<String>,
    val severity: Severity,
    val status: ReportStatus,
    val resolvedVotes: Int = 0,
    val createdAt: String
)
